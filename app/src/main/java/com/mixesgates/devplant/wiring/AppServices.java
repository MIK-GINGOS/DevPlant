package com.mixesgates.devplant.wiring;

import android.content.Context;

import com.mixesgates.devplant.app_db.CatalogueLookup;
import com.mixesgates.devplant.app_db.RoomAlertStore;
import com.mixesgates.devplant.app_db.RoomCropStore;
import com.mixesgates.devplant.app_db.RoomDiagnosisStore;
import com.mixesgates.devplant.app_db.RoomScanStore;
import com.mixesgates.devplant.app_rules.DiagnosisRules;
import com.mixesgates.devplant.app_rules.LeafPictures;
import com.mixesgates.devplant.contracts.AlertStore;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.CropStore;
import com.mixesgates.devplant.contracts.DiagnosisStore;
import com.mixesgates.devplant.contracts.ScanStore;
import com.mixesgates.devplant.contracts.Services;
import com.mixesgates.devplant.gates.gm_db.DevPlantDatabase;
import com.mixesgates.devplant.gates.gm_images.LeafPhotos;
import com.mixesgates.devplant.gates.gm_model.LeafModel;
import com.mixesgates.devplant.gates.gm_model.TfliteModel;

/** Picks which gate sits behind each contract. */
public final class AppServices implements Services {

    private final Context context;
    private final Changes changes = new SimpleChanges();
    private final ScanStore scans;
    private final DiagnosisStore diagnoses;
    private final CropStore crops;
    private final AlertStore alerts;
    private LeafModel model;

    public AppServices(Context context) {
        this.context = context.getApplicationContext();
        DevPlantDatabase db = DevPlantDatabase.open(this.context, "devplant.db");
        LeafPhotos photos = new LeafPhotos(this.context);
        scans = new RoomScanStore(db, photos, new LeafPictures(), this::model,
                new DiagnosisRules(), new CatalogueLookup(db), changes);
        diagnoses = new RoomDiagnosisStore(db, this::model);
        crops = new RoomCropStore(db, changes);
        alerts = new RoomAlertStore(db, changes);
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
    public Changes changes() {
        return changes;
    }
}
