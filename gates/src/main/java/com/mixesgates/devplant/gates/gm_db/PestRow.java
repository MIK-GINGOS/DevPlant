package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pests")
public class PestRow {
    @PrimaryKey
    @NonNull
    public String subjectKey = "";
    @NonNull
    public String name = "";
    public String scientificName;
    public String crops;
    public String symptoms;
    /** How the pest spreads or damages. */
    public String spread;
}
