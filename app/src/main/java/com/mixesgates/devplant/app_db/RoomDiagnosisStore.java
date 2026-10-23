package com.mixesgates.devplant.app_db;

import com.mixesgates.devplant.contracts.Diagnosis;
import com.mixesgates.devplant.contracts.DiagnosisStore;
import com.mixesgates.devplant.contracts.GuideEntry;
import com.mixesgates.devplant.contracts.Treatment;
import com.mixesgates.devplant.gates.gm_db.DevPlantDatabase;
import com.mixesgates.devplant.gates.gm_db.DiagnosisRow;
import com.mixesgates.devplant.gates.gm_db.DiseaseRow;
import com.mixesgates.devplant.gates.gm_db.PestRow;
import com.mixesgates.devplant.gates.gm_db.TreatmentRow;
import com.mixesgates.devplant.gates.gm_model.LeafModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RoomDiagnosisStore implements DiagnosisStore {

    private final DevPlantDatabase db;
    private final Supplier<LeafModel> model;

    public RoomDiagnosisStore(DevPlantDatabase db, Supplier<LeafModel> model) {
        this.db = db;
        this.model = model;
    }

    @Override
    public Diagnosis forScan(long scanId) {
        DiagnosisRow row = db.diagnoses().forScan(scanId);
        return row == null ? null : Rows.diagnosis(row);
    }

    @Override
    public List<Treatment> treatmentsFor(String key) {
        db.ensureSeeded();
        List<Treatment> out = new ArrayList<>();
        for (TreatmentRow r : db.catalogue().treatments(key)) out.add(Rows.treatment(r));
        return out;
    }

    @Override
    public List<GuideEntry> guide() {
        db.ensureSeeded();
        List<GuideEntry> out = new ArrayList<>();
        for (DiseaseRow r : db.catalogue().diseases()) out.add(Rows.disease(r));
        for (PestRow r : db.catalogue().pests()) out.add(Rows.pest(r));
        return out;
    }

    @Override
    public GuideEntry guideEntry(String key) {
        db.ensureSeeded();
        DiseaseRow d = db.catalogue().disease(key);
        if (d != null) return Rows.disease(d);
        PestRow p = db.catalogue().pest(key);
        return p == null ? null : Rows.pest(p);
    }

    @Override
    public List<GuideEntry> searchGuide(String query) {
        db.ensureSeeded();
        String q = query.trim();
        List<GuideEntry> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        for (DiseaseRow r : db.catalogue().searchDiseases(q)) out.add(Rows.disease(r));
        for (PestRow r : db.catalogue().searchPests(q)) out.add(Rows.pest(r));
        return out;
    }

    @Override
    public String modelName() {
        return model.get().name();
    }
}
