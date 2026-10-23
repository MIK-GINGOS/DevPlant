package com.mixesgates.devplant.app_db;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Crop;
import com.mixesgates.devplant.contracts.CropStore;
import com.mixesgates.devplant.contracts.Field;
import com.mixesgates.devplant.gates.gm_db.DevPlantDatabase;
import com.mixesgates.devplant.gates.gm_db.FieldRow;
import com.mixesgates.devplant.gates.gm_db.FieldWithCrop;

import java.util.ArrayList;
import java.util.List;

public final class RoomCropStore implements CropStore {

    private final DevPlantDatabase db;
    private final Changes changes;

    public RoomCropStore(DevPlantDatabase db, Changes changes) {
        this.db = db;
        this.changes = changes;
    }

    @Override
    public List<Crop> crops() {
        db.ensureSeeded();
        return Rows.crops(db.crops().all());
    }

    @Override
    public List<Crop> searchCrops(String query) {
        db.ensureSeeded();
        String q = query.trim();
        return q.isEmpty() ? new ArrayList<>() : Rows.crops(db.crops().search(q));
    }

    @Override
    public List<Field> fields() {
        List<Field> out = new ArrayList<>();
        for (FieldWithCrop r : db.fields().all()) out.add(Rows.field(r));
        return out;
    }

    @Override
    public long addField(String name, long cropId, double areaHectares, String notes) {
        FieldRow row = new FieldRow();
        row.name = name.trim();
        row.cropId = cropId;
        row.areaHectares = areaHectares;
        row.notes = notes;
        row.createdAt = System.currentTimeMillis();
        long id = db.fields().insert(row);
        changes.post(Changes.Topic.CROPS);
        return id;
    }

    @Override
    public void deleteField(long fieldId) {
        db.fields().delete(fieldId);
        changes.post(Changes.Topic.CROPS);
        changes.post(Changes.Topic.SCANS);
        changes.post(Changes.Topic.ALERTS);
    }
}
