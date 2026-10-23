package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Diseases, pests and their treatments. */
@Dao
public interface CatalogueDao {
    @Query("SELECT * FROM diseases ORDER BY name")
    List<DiseaseRow> diseases();

    @Query("SELECT * FROM pests ORDER BY name")
    List<PestRow> pests();

    @Query("SELECT * FROM diseases WHERE subjectKey = :key")
    DiseaseRow disease(String key);

    @Query("SELECT * FROM pests WHERE subjectKey = :key")
    PestRow pest(String key);

    @Query("SELECT * FROM diseases WHERE name LIKE '%' || :q || '%' "
            + "OR scientificName LIKE '%' || :q || '%' OR crops LIKE '%' || :q || '%' "
            + "OR symptoms LIKE '%' || :q || '%' ORDER BY name")
    List<DiseaseRow> searchDiseases(String q);

    @Query("SELECT * FROM pests WHERE name LIKE '%' || :q || '%' "
            + "OR scientificName LIKE '%' || :q || '%' OR crops LIKE '%' || :q || '%' "
            + "OR symptoms LIKE '%' || :q || '%' ORDER BY name")
    List<PestRow> searchPests(String q);

    @Query("SELECT * FROM treatments WHERE subjectKey = :key ORDER BY sortOrder")
    List<TreatmentRow> treatments(String key);

    @Query("SELECT COUNT(*) FROM diseases")
    int diseaseCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDiseases(List<DiseaseRow> rows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPests(List<PestRow> rows);

    @Insert
    void insertTreatments(List<TreatmentRow> rows);
}
