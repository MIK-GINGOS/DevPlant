package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "scans",
        foreignKeys = @ForeignKey(entity = FieldRow.class, parentColumns = "id",
                childColumns = "fieldId", onDelete = ForeignKey.SET_NULL),
        indices = {@Index("fieldId"), @Index("takenAt")})
public class ScanRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long takenAt;
    @NonNull
    public String photoPath = "";
    /** Null when the scan is not tied to a field. */
    public Long fieldId;
}
