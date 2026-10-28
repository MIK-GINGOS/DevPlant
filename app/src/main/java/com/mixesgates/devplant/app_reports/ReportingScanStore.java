package com.mixesgates.devplant.app_reports;

import android.util.Log;

import com.mixesgates.devplant.contracts.CropStore;
import com.mixesgates.devplant.contracts.Field;
import com.mixesgates.devplant.contracts.HealthSummary;
import com.mixesgates.devplant.contracts.Report;
import com.mixesgates.devplant.contracts.Scan;
import com.mixesgates.devplant.contracts.ScanOutcome;
import com.mixesgates.devplant.contracts.ScanRequest;
import com.mixesgates.devplant.contracts.ScanStore;
import com.mixesgates.devplant.gates.gm_images.LeafPhotos;
import com.mixesgates.devplant.gates.gm_model.LeafModel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Wraps DevPlant's own ScanStore and files a report beside every scan. Scans pass through
 * unchanged. A photo the rules ask to retake is kept as an uncertain report instead of being
 * thrown away; if the farmer then retakes it within a couple of minutes, that try is dropped.
 */
public final class ReportingScanStore implements ScanStore {

    private static final String TAG = "ReportingScanStore";
    /** Uncertain photos younger than this are treated as tries at the same leaf. */
    static final long RETRY_WINDOW_MS = 2L * 60 * 1000;

    private final ScanStore scans;
    private final RoomReportStore reports;
    private final CropStore crops;
    private final LeafPhotos photos;
    private final Supplier<LeafModel> model;

    public ReportingScanStore(ScanStore scans, RoomReportStore reports, CropStore crops,
                              LeafPhotos photos, Supplier<LeafModel> model) {
        this.scans = scans;
        this.reports = reports;
        this.crops = crops;
        this.photos = photos;
        this.model = model;
    }

    @Override
    public ScanOutcome scan(ScanRequest request) throws IOException {
        ScanOutcome outcome = scans.scan(request);
        // A report that fails to save must never cost the farmer their result.
        try {
            long now = System.currentTimeMillis();
            reports.dropRetries(now - RETRY_WINDOW_MS);
            Report.Source source = model.get().isTrained() ? Report.Source.AI_MODEL : Report.Source.SIMULATED;
            String crop = cropOf(request.fieldId);
            if (outcome.retake) {
                File kept = photos.keep(new File(request.photoPath));
                reports.addUncertain(now, kept.getPath(), crop, outcome.retakeReason, source);
            } else if (outcome.scan != null) {
                reports.addForScan(outcome.scan, crop, source);
            }
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "report not saved", e);
        }
        return outcome;
    }

    private String cropOf(long fieldId) {
        if (fieldId <= 0) return null;
        for (Field f : crops.fields()) {
            if (f.id == fieldId) return f.cropName;
        }
        return null;
    }

    @Override
    public void delete(long scanId) {
        scans.delete(scanId);
        reports.deleteForScan(scanId);
    }

    @Override
    public List<Scan> recent(int limit) {
        return scans.recent(limit);
    }

    @Override
    public Scan get(long scanId) {
        return scans.get(scanId);
    }

    @Override
    public HealthSummary health(long sinceMillis) {
        return scans.health(sinceMillis);
    }
}
