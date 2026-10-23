package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FieldDao {
    @Query("SELECT f.*, c.name AS cropName FROM fields f JOIN crops c ON c.id = f.cropId "
            + "ORDER BY f.name")
    List<FieldWithCrop> all();

    @Query("SELECT f.*, c.name AS cropName FROM fields f JOIN crops c ON c.id = f.cropId "
            + "WHERE f.id = :id")
    FieldWithCrop get(long id);

    @Insert
    long insert(FieldRow row);

    @Query("DELETE FROM fields WHERE id = :id")
    void delete(long id);
}
