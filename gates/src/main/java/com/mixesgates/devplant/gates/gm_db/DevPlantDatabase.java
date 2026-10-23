package com.mixesgates.devplant.gates.gm_db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** The Room database: 8 tables, filled with the crop, disease and pest catalogue on first use. */
@Database(
        entities = {CropRow.class, FieldRow.class, ScanRow.class, DiagnosisRow.class,
                DiseaseRow.class, PestRow.class, TreatmentRow.class, AlertRow.class},
        version = 1,
        exportSchema = false)
public abstract class DevPlantDatabase extends RoomDatabase {

    public abstract CropDao crops();

    public abstract FieldDao fields();

    public abstract ScanDao scans();

    public abstract DiagnosisDao diagnoses();

    public abstract CatalogueDao catalogue();

    public abstract AlertDao alerts();

    private volatile boolean seeded;

    public static DevPlantDatabase open(Context context, String fileName) {
        return Room.databaseBuilder(context.getApplicationContext(), DevPlantDatabase.class, fileName)
                .build();
    }

    /** Fills the catalogue if it is empty. Blocks; call off the main thread. */
    public void ensureSeeded() {
        if (seeded) return;
        synchronized (this) {
            if (seeded) return;
            runInTransaction(() -> {
                if (catalogue().diseaseCount() == 0) Catalogue.seed(this);
            });
            seeded = true;
        }
    }
}
