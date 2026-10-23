package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface DiagnosisDao {
    @Insert
    long insert(DiagnosisRow row);

    @Query("SELECT * FROM diagnoses WHERE scanId = :scanId")
    DiagnosisRow forScan(long scanId);
}
