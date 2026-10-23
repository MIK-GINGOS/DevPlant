package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Embedded;
import androidx.room.Relation;

/** A scan with its field and diagnosis, both of which may be missing. */
public class ScanFull {
    @Embedded
    public ScanRow scan;

    @Relation(parentColumn = "fieldId", entityColumn = "id")
    public FieldRow field;

    @Relation(parentColumn = "id", entityColumn = "scanId")
    public DiagnosisRow diagnosis;
}
