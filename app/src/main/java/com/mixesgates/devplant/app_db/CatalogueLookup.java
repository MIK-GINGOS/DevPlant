package com.mixesgates.devplant.app_db;

import com.mixesgates.devplant.app_rules.DiagnosisRules;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.gates.gm_db.DevPlantDatabase;
import com.mixesgates.devplant.gates.gm_db.DiseaseRow;
import com.mixesgates.devplant.gates.gm_db.PestRow;

/** Answers the rules' questions about model labels from the disease and pest tables. */
public final class CatalogueLookup implements DiagnosisRules.Lookup {

    private final DevPlantDatabase db;

    public CatalogueLookup(DevPlantDatabase db) {
        this.db = db;
    }

    @Override
    public Kind kindOf(String key) {
        if (DiagnosisRules.HEALTHY.equals(key)) return Kind.HEALTHY;
        if (db.catalogue().disease(key) != null) return Kind.DISEASE;
        if (db.catalogue().pest(key) != null) return Kind.PEST;
        return null;
    }

    @Override
    public String nameOf(String key) {
        DiseaseRow d = db.catalogue().disease(key);
        if (d != null) return d.name;
        PestRow p = db.catalogue().pest(key);
        return p == null ? null : p.name;
    }
}
