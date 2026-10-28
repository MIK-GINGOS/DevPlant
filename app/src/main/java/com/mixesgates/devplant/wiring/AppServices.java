package com.mixesgates.devplant.wiring;

import android.content.Context;

import com.mixesgates.devplant.BuildConfig;
import com.mixesgates.devplant.app_db.CatalogueLookup;
import com.mixesgates.devplant.app_db.RoomAlertStore;
import com.mixesgates.devplant.app_db.RoomCropStore;
import com.mixesgates.devplant.app_db.RoomDiagnosisStore;
import com.mixesgates.devplant.app_db.RoomScanStore;
import com.mixesgates.devplant.app_http.CloudPlantCatalog;
import com.mixesgates.devplant.app_reports.ReportingScanStore;
import com.mixesgates.devplant.app_reports.RoomReportStore;
import com.mixesgates.devplant.app_rules.DiagnosisRules;
import com.mixesgates.devplant.app_rules.LeafPictures;
import com.mixesgates.devplant.contracts.AlertStore;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.CropStore;
import com.mixesgates.devplant.contracts.DiagnosisStore;
import com.mixesgates.devplant.contracts.PlantCatalog;
import com.mixesgates.devplant.contracts.ReportServices;
import com.mixesgates.devplant.contracts.ReportStore;
import com.mixesgates.devplant.contracts.ScanStore;
import com.mixesgates.devplant.contracts.Services;
import com.mixesgates.devplant.gates.gm_db.DevPlantDatabase;
import com.mixesgates.devplant.gates.gm_http.JsonHttp;
import com.mixesgates.devplant.gates.gm_http.WebFiles;
import com.mixesgates.devplant.gates.gm_images.LeafPhotos;
import com.mixesgates.devplant.gates.gm_model.LeafModel;
import com.mixesgates.devplant.gates.gm_model.TfliteModel;
import com.mixesgates.devplant.gates.gm_reports.ConsentPrefs;
import com.mixesgates.devplant.gates.gm_reports.ReportsDatabase;

/** Picks which gate sits behind each contract. */
public final class AppServices implements Services, ReportServices {

    private final Context context;
    private final Changes changes = new SimpleChanges();
    private final ScanStore scans;
    private final DiagnosisStore diagnoses;
    private final CropStore crops;
    private final AlertStore alerts;
    private final PlantCatalog plants;
    private final RoomReportStore reports;
    private LeafModel model;

    public AppServices(Context context) {
        this.context = context.getApplicationContext();
        DevPlantDatabase db = DevPlantDatabase.open(this.context, "devplant.db");
        LeafPhotos photos = new LeafPhotos(this.context);
        crops = new RoomCropStore(db, changes);
        // Every scan also files a report beside it (CropGuard); the scan itself is unchanged.
        // Shared reports go to the officer service on the same cloud/ site as the catalogue.
        reports = new RoomReportStore(ReportsDatabase.open(this.context, "devplant_reports.db"),
                new ConsentPrefs(this.context), photos, changes, new JsonHttp(), BuildConfig.CATALOG_URL);
        reports.syncSoon();
        scans = new ReportingScanStore(
                new RoomScanStore(db, photos, new LeafPictures(), this::model,
                        new DiagnosisRules(), new CatalogueLookup(db), changes),
                reports, crops, photos, this::model);
        diagnoses = new RoomDiagnosisStore(db, this::model);
        alerts = new RoomAlertStore(db, changes);
        // The catalogue site and its mirror are set in gradle.properties (devplant.catalogUrl, catalogMirror).
        plants = new CloudPlantCatalog(new WebFiles(this.context), BuildConfig.CATALOG_URL,
                BuildConfig.CATALOG_MIRROR, changes);
    }

    /** Loaded on first use, off the main thread. */
    private synchronized LeafModel model() {
        if (model == null) {
            // Where the model is chosen. A better model is a new file in gm_model; mixes does not change.
            model = TfliteModel.open(context, "devplant_v1");
        }
        return model;
    }

    @Override
    public ScanStore scans() {
        return scans;
    }

    @Override
    public DiagnosisStore diagnoses() {
        return diagnoses;
    }

    @Override
    public CropStore crops() {
        return crops;
    }

    @Override
    public AlertStore alerts() {
        return alerts;
    }

    @Override
    public PlantCatalog plants() {
        return plants;
    }

    @Override
    public Changes changes() {
        return changes;
    }

    @Override
    public ReportStore reports() {
        return reports;
    }
}
