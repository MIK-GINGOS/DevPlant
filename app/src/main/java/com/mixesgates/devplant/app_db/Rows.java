package com.mixesgates.devplant.app_db;

import com.mixesgates.devplant.contracts.Alert;
import com.mixesgates.devplant.contracts.Crop;
import com.mixesgates.devplant.contracts.Diagnosis;
import com.mixesgates.devplant.contracts.Field;
import com.mixesgates.devplant.contracts.GuideEntry;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Scan;
import com.mixesgates.devplant.contracts.Severity;
import com.mixesgates.devplant.contracts.Treatment;
import com.mixesgates.devplant.gates.gm_db.AlertRow;
import com.mixesgates.devplant.gates.gm_db.CropRow;
import com.mixesgates.devplant.gates.gm_db.DiagnosisRow;
import com.mixesgates.devplant.gates.gm_db.DiseaseRow;
import com.mixesgates.devplant.gates.gm_db.FieldWithCrop;
import com.mixesgates.devplant.gates.gm_db.PestRow;
import com.mixesgates.devplant.gates.gm_db.ScanFull;
import com.mixesgates.devplant.gates.gm_db.TreatmentRow;

import java.util.ArrayList;
import java.util.List;

/** Turns database rows into contract shapes. */
final class Rows {

    private Rows() {
    }

    static Crop crop(CropRow r) {
        return new Crop(r.id, r.name, r.scientificName);
    }

    static List<Crop> crops(List<CropRow> rows) {
        List<Crop> out = new ArrayList<>(rows.size());
        for (CropRow r : rows) out.add(crop(r));
        return out;
    }

    static Field field(FieldWithCrop r) {
        return new Field(r.field.id, r.field.name, r.field.cropId, r.cropName,
                r.field.areaHectares, r.field.notes, r.field.createdAt);
    }

    static Scan scan(ScanFull r) {
        return new Scan(r.scan.id, r.scan.takenAt, r.scan.photoPath,
                r.scan.fieldId == null ? 0 : r.scan.fieldId,
                r.field == null ? null : r.field.name,
                r.diagnosis == null ? null : diagnosis(r.diagnosis));
    }

    static Diagnosis diagnosis(DiagnosisRow r) {
        return new Diagnosis(r.id, r.scanId, Kind.valueOf(r.kind), r.subjectKey, r.name,
                r.confidence, Severity.valueOf(r.severity), r.damage, r.alternatives, r.advice);
    }

    static Treatment treatment(TreatmentRow r) {
        return new Treatment(r.id, Treatment.Method.valueOf(r.method), r.title, r.detail);
    }

    static GuideEntry disease(DiseaseRow r) {
        return new GuideEntry(r.subjectKey, Kind.DISEASE, r.name, r.scientificName, r.crops,
                r.symptoms, r.spread);
    }

    static GuideEntry pest(PestRow r) {
        return new GuideEntry(r.subjectKey, Kind.PEST, r.name, r.scientificName, r.crops,
                r.symptoms, r.spread);
    }

    static Alert alert(AlertRow r) {
        return new Alert(r.id, r.createdAt, Alert.Level.valueOf(r.level), r.title, r.body,
                r.subjectKey, r.fieldId == null ? 0 : r.fieldId, r.read);
    }
}
