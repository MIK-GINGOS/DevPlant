package com.mixesgates.devplant.app_reports;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Consent;
import com.mixesgates.devplant.contracts.Diagnosis;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Question;
import com.mixesgates.devplant.contracts.Report;
import com.mixesgates.devplant.contracts.ReportStore;
import com.mixesgates.devplant.contracts.Scan;
import com.mixesgates.devplant.gates.gm_http.JsonHttp;
import com.mixesgates.devplant.gates.gm_images.LeafPhotos;
import com.mixesgates.devplant.gates.gm_reports.ConsentPrefs;
import com.mixesgates.devplant.gates.gm_reports.QuestionRow;
import com.mixesgates.devplant.gates.gm_reports.ReportRow;
import com.mixesgates.devplant.gates.gm_reports.ReportsDatabase;

import java.util.ArrayList;
import java.util.List;

/** Reports and questions in their own Room file, plus the sharing choice; ReportSync sends them. */
public final class RoomReportStore implements ReportStore {

    private final ReportsDatabase db;
    private final ConsentPrefs consent;
    private final LeafPhotos photos;
    private final Changes changes;
    private final ReportSync sync;

    /** @param cloudUrl the officer service, the same cloud/ site as the plant catalogue */
    public RoomReportStore(ReportsDatabase db, ConsentPrefs consent, LeafPhotos photos, Changes changes,
                           JsonHttp http, String cloudUrl) {
        this.db = db;
        this.consent = consent;
        this.photos = photos;
        this.changes = changes;
        sync = new ReportSync(db, consent, http, cloudUrl, changes);
    }

    @Override
    public void syncSoon() {
        sync.soon();
    }

    // ---- written by ReportingScanStore ----

    void addForScan(Scan scan, String cropName, Report.Source source) {
        Diagnosis d = scan.diagnosis;
        ReportRow row = newRow(scan.takenAt, scan.photoPath, cropName, source);
        row.scanId = scan.id;
        if (d != null) {
            row.kind = d.kind.name();
            row.resultKey = d.key;
            row.resultName = d.name;
            row.confidence = d.confidence;
            if (d.kind == Kind.UNSURE) row.uncertainReason = d.advice;
        }
        db.reports().insert(row);
        changes.post(Changes.Topic.SCANS);
        if (consent.share()) sync.soon();
    }

    void addUncertain(long now, String photoPath, String cropName, String reason, Report.Source source) {
        ReportRow row = newRow(now, photoPath, cropName, source);
        row.kind = Kind.UNSURE.name();
        row.uncertainReason = reason;
        db.reports().insert(row);
        changes.post(Changes.Topic.SCANS);
        if (consent.share()) sync.soon();
    }

    /** Drops uncertain photos taken since {@code since}: they were earlier tries at the same leaf. */
    void dropRetries(long since) {
        for (ReportRow r : db.reports().uncertainSince(since)) {
            photos.delete(r.photoPath);
            db.reports().delete(r.id);
        }
    }

    void deleteForScan(long scanId) {
        db.reports().deleteForScan(scanId);
        changes.post(Changes.Topic.SCANS);
    }

    private ReportRow newRow(long createdAt, String photoPath, String cropName, Report.Source source) {
        ReportRow row = new ReportRow();
        row.createdAt = createdAt;
        row.photoPath = photoPath;
        row.cropName = cropName;
        row.country = consent.country();
        row.district = consent.district();
        row.source = source.name();
        row.verification = Report.Verification.REPORTED.name();
        row.sync = (consent.share() ? Report.Sync.QUEUED : Report.Sync.LOCAL).name();
        return row;
    }

    // ---- ReportStore ----

    @Override
    public List<Report> recent(int limit) {
        List<ReportRow> rows = db.reports().recent(limit);
        List<Report> out = new ArrayList<>(rows.size());
        for (ReportRow r : rows) out.add(toReport(r));
        return out;
    }

    @Override
    public Report forScan(long scanId) {
        ReportRow row = db.reports().forScan(scanId);
        return row == null ? null : toReport(row);
    }

    @Override
    public void delete(long reportId) {
        ReportRow row = db.reports().get(reportId);
        if (row == null || row.scanId != 0) return;
        photos.delete(row.photoPath);
        db.reports().delete(reportId);
        changes.post(Changes.Topic.SCANS);
    }

    @Override
    public Consent consent() {
        return new Consent(consent.decided(), consent.share(), consent.country(), consent.district());
    }

    @Override
    public void saveConsent(Consent c) {
        consent.save(c.share, c.country, c.district);
        if (c.share) db.reports().moveSync(Report.Sync.LOCAL.name(), Report.Sync.QUEUED.name());
        else db.reports().moveSync(Report.Sync.QUEUED.name(), Report.Sync.LOCAL.name());
        changes.post(Changes.Topic.SCANS);
        if (c.share) sync.soon();
    }

    @Override
    public List<Question> questions() {
        List<QuestionRow> rows = db.questions().all();
        List<Question> out = new ArrayList<>(rows.size());
        for (QuestionRow q : rows) {
            out.add(new Question(q.id, q.reportId, q.askedAt, q.text, q.cropName, q.answer, q.answeredBy,
                    q.answeredAt, parse(Report.Sync.class, q.sync, Report.Sync.QUEUED)));
        }
        return out;
    }

    @Override
    public Question ask(String text, long reportId) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("A question needs some text");
        QuestionRow q = new QuestionRow();
        q.askedAt = System.currentTimeMillis();
        q.text = trimmed;
        ReportRow report = reportId == 0 ? null : db.reports().get(reportId);
        if (report != null) {
            q.reportId = report.id;
            q.cropName = report.cropName;
            // The farmer chose to ask about this report, so its details go along with the question.
            if (Report.Sync.LOCAL.name().equals(report.sync)) db.reports().setSync(report.id, Report.Sync.QUEUED.name());
        }
        q.id = db.questions().insert(q);
        changes.post(Changes.Topic.SCANS);
        sync.soon();
        return new Question(q.id, q.reportId, q.askedAt, q.text, q.cropName, null, null, 0, Report.Sync.QUEUED);
    }

    private static Report toReport(ReportRow r) {
        return new Report(r.id, r.scanId, r.createdAt, r.photoPath, r.cropName, r.country,
                r.district, parse(Kind.class, r.kind, Kind.UNSURE), r.resultKey, r.resultName,
                r.confidence, r.uncertainReason,
                parse(Report.Source.class, r.source, Report.Source.SIMULATED),
                parse(Report.Verification.class, r.verification, Report.Verification.REPORTED),
                parse(Report.Sync.class, r.sync, Report.Sync.LOCAL), r.officerNote);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return fallback;
        }
    }
}
