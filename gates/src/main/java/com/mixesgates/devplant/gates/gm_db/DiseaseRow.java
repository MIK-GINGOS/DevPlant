package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diseases")
public class DiseaseRow {
    @PrimaryKey
    @NonNull
    public String subjectKey = "";
    @NonNull
    public String name = "";
    public String scientificName;
    public String crops;
    public String symptoms;
    public String spread;
}
