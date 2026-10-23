package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "diagnoses",
        foreignKeys = @ForeignKey(entity = ScanRow.class, parentColumns = "id",
                childColumns = "scanId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "scanId", unique = true), @Index("subjectKey")})
public class DiagnosisRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long scanId;
    /** HEALTHY, DISEASE, PEST or UNSURE. */
    @NonNull
    public String kind = "UNSURE";
    @NonNull
    public String subjectKey = "";
    @NonNull
    public String name = "";
    public float confidence;
    /** NONE, MILD, MODERATE or SEVERE. */
    @NonNull
    public String severity = "NONE";
    public float damage;
    public String alternatives;
    public String advice;
}
