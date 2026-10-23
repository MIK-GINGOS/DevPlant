package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CropDao {
    @Query("SELECT * FROM crops ORDER BY name")
    List<CropRow> all();

    @Query("SELECT * FROM crops WHERE name LIKE '%' || :q || '%' "
            + "OR scientificName LIKE '%' || :q || '%' ORDER BY name")
    List<CropRow> search(String q);

    @Query("SELECT COUNT(*) FROM crops")
    int count();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CropRow row);
}
