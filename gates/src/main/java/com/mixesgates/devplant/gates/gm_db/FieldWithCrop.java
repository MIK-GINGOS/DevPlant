package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Embedded;

public class FieldWithCrop {
    @Embedded
    public FieldRow field;
    public String cropName;
}
