package com.mixesgates.devplant.app_db;

import android.graphics.Bitmap;

import com.mixesgates.devplant.app_rules.DiagnosisRules;
import com.mixesgates.devplant.app_rules.LeafPictures;
import com.mixesgates.devplant.app_rules.LeafStats;
import com.mixesgates.devplant.app_rules.Verdict;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.FieldHealth;
import com.mixesgates.devplant.contracts.HealthSummary;
import com.mixesgates.devplant.contracts.Scan;
import com.mixesgates.devplant.contracts.ScanOutcome;
import com.mixesgates.devplant.contracts.ScanRequest;
import com.mixesgates.devplant.contracts.ScanStore;
import com.mixesgates.devplant.gates.gm_db.AlertRow;
import com.mixesgates.devplant.gates.gm_db.DevPlantDatabase;
import com.mixesgates.devplant.gates.gm_db.DiagnosisRow;
import com.mixesgates.devplant.gates.gm_db.FieldCount;
import com.mixesgates.devplant.gates.gm_db.FieldWithCrop;
import com.mixesgates.devplant.gates.gm_db.ScanFull;
import com.mixesgates.devplant.gates.gm_db.ScanRow;
import com.mixesgates.devplant.gates.gm_images.LeafPhotos;
import com.mixesgates.devplant.gates.gm_model.Label;
import com.mixesgates.devplant.gates.gm_model.LeafModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** One scan, from leaf to diagnosis: pictures, model, rules, then Room. */
public final class RoomScanStore implements ScanStore {

    private static final long ALERT_REPEAT_MS = 7L * 24 * 60 * 60 * 1000;

    private final DevPlantDatabase db;
    private final LeafPhotos photos;
    private final LeafPictures pictures;
    private final Supplier<LeafModel> model;
    private final DiagnosisRules rules;
    private final DiagnosisRules.Lookup lookup;
    private final Changes changes;

    public RoomScanStore(DevPlantDatabase db, LeafPhotos photos, LeafPictures pictures,
                         Supplier<LeafModel> model, DiagnosisRules rules,
                         DiagnosisRules.Lookup lookup, Changes changes) {
        this.db = db;
        this.photos = photos;
        this.pictures = pictures;
        this.model = model;
        this.rules = rules;
        this.lookup = lookup;
        this.changes = changes;
    }

    @Override
    public ScanOutcome scan(ScanRequest request) throws IOException {
        db.ensureSeeded();
        File source = new File(request.photoPath);
        LeafModel leafModel = model.get();
        Bitmap square = pictures.prepare(source, leafModel.inputSize());
        LeafStats stats = pictures.measure(square);
        List<Label> labels = leafModel.classify(square);
        square.recycle();

        Verdict verdict = rules.decide(labels, stats, lookup);
        if (verdict.retake) return ScanOutcome.retake(verdict.retakeReason);

        File kept = photos.keep(source);
        long now = System.currentTimeMillis();
        Long fieldId = request.fieldId > 0 ? request.fieldId : null;
        long[] scanId = new long[1];
        boolean[] alerted = new boolean[1];
        try {
            db.runInTransaction(() -> {
                ScanRow scan = new ScanRow();
                scan.takenAt = now;
                scan.photoPath = kept.getPath();
                scan.fieldId = fieldId;
                scanId[0] = db.scans().insert(scan);

                DiagnosisRow d = new DiagnosisRow();
                d.scanId = scanId[0];
                d.kind = verdict.kind.name();
                d.subjectKey = verdict.key;
                d.name = verdict.name;
                d.confidence = verdict.confidence;
                d.severity = verdict.severity.name();
                d.damage = verdict.damage;
                d.alternatives = verdict.alternatives;
                d.advice = verdict.advice;
                db.diagnoses().insert(d);

                alerted[0] = raiseAlertIfNeeded(verdict, fieldId, now);
            });
        } catch (RuntimeException e) {
            photos.delete(kept.getPath());
            throw e;
        }

        changes.post(Changes.Topic.SCANS);
        if (alerted[0]) changes.post(Changes.Topic.ALERTS);
        return ScanOutcome.saved(get(scanId[0]));
    }

    private boolean raiseAlertIfNeeded(Verdict verdict, Long fieldId, long now) {
        if (!verdict.isProblem()) return false;
        int recent = db.scans().countSubjectInField(verdict.key, fieldId,
                now - DiagnosisRules.OUTBREAK_WINDOW_MS);
        String fieldName = null;
        if (fieldId != null) {
            FieldWithCrop field = db.fields().get(fieldId);
            if (field != null) fieldName = field.field.name;
        }
        DiagnosisRules.Warning warning = rules.warningFor(verdict, recent, fieldName);
        if (warning == null) return false;
        if (db.alerts().countSince(verdict.key, fieldId, now - ALERT_REPEAT_MS) > 0) return false;

        AlertRow alert = new AlertRow();
        alert.createdAt = now;
        alert.level = warning.level.name();
        alert.title = warning.title;
        alert.body = warning.body;
        alert.subjectKey = verdict.key;
        alert.fieldId = fieldId;
        db.alerts().insert(alert);
        return true;
    }

    @Override
    public List<Scan> recent(int limit) {
        List<ScanFull> rows = db.scans().recent(limit);
        List<Scan> out = new ArrayList<>(rows.size());
        for (ScanFull r : rows) out.add(Rows.scan(r));
        return out;
    }

    @Override
    public Scan get(long scanId) {
        ScanFull row = db.scans().get(scanId);
        return row == null ? null : Rows.scan(row);
    }

    @Override
    public void delete(long scanId) {
        ScanFull row = db.scans().get(scanId);
        if (row == null) return;
        db.scans().delete(scanId);
        photos.delete(row.scan.photoPath);
        changes.post(Changes.Topic.SCANS);
    }

    @Override
    public HealthSummary health(long sinceMillis) {
        db.ensureSeeded();
        int scans = 0, healthy = 0, diseases = 0, pests = 0, unsure = 0;
        List<FieldHealth> fields = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (FieldCount c : db.scans().countsByField(sinceMillis)) {
            scans += c.scans;
            healthy += c.healthy;
            diseases += c.diseases;
            pests += c.pests;
            unsure += c.unsure;
            long id = c.fieldId == null ? 0 : c.fieldId;
            seen.add(id);
            fields.add(new FieldHealth(id, c.fieldId == null ? null : c.fieldName, c.scans,
                    c.healthy, c.diseases + c.pests));
        }
        for (FieldWithCrop f : db.fields().all()) {
            if (!seen.contains(f.field.id)) {
                fields.add(new FieldHealth(f.field.id, f.field.name, 0, 0, 0));
            }
        }
        return new HealthSummary(sinceMillis, scans, healthy, diseases, pests, unsure,
                db.alerts().unreadCount(), fields);
    }
}
