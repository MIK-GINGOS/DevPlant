package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "crops", indices = @Index(value = "name", unique = true))
public class CropRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String name = "";
    public String scientificName;
}
