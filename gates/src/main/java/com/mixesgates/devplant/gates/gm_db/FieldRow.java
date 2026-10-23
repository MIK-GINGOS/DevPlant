package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "fields",
        foreignKeys = @ForeignKey(entity = CropRow.class, parentColumns = "id",
                childColumns = "cropId", onDelete = ForeignKey.RESTRICT),
        indices = @Index("cropId"))
public class FieldRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String name = "";
    public long cropId;
    public double areaHectares;
    public String notes;
    public long createdAt;
}
