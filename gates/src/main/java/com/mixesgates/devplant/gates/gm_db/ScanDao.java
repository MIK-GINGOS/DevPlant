package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ScanDao {
    @Insert
    long insert(ScanRow row);

    @Transaction
    @Query("SELECT * FROM scans ORDER BY takenAt DESC LIMIT :limit")
    List<ScanFull> recent(int limit);

    @Transaction
    @Query("SELECT * FROM scans WHERE id = :id")
    ScanFull get(long id);

    @Query("DELETE FROM scans WHERE id = :id")
    void delete(long id);

    @Query("SELECT s.fieldId AS fieldId, f.name AS fieldName, COUNT(*) AS scans, "
            + "SUM(CASE WHEN d.kind = 'HEALTHY' THEN 1 ELSE 0 END) AS healthy, "
            + "SUM(CASE WHEN d.kind = 'DISEASE' THEN 1 ELSE 0 END) AS diseases, "
            + "SUM(CASE WHEN d.kind = 'PEST' THEN 1 ELSE 0 END) AS pests, "
            + "SUM(CASE WHEN d.kind = 'UNSURE' OR d.kind IS NULL THEN 1 ELSE 0 END) AS unsure "
            + "FROM scans s LEFT JOIN fields f ON f.id = s.fieldId "
            + "LEFT JOIN diagnoses d ON d.scanId = s.id "
            + "WHERE s.takenAt >= :since GROUP BY s.fieldId ORDER BY scans DESC")
    List<FieldCount> countsByField(long since);

    /** How many scans in a field (null = no field) were diagnosed with a subject since a time. */
    @Query("SELECT COUNT(*) FROM diagnoses d JOIN scans s ON s.id = d.scanId "
            + "WHERE d.subjectKey = :subjectKey AND s.fieldId IS :fieldId AND s.takenAt >= :since")
    int countSubjectInField(String subjectKey, Long fieldId, long since);
}
