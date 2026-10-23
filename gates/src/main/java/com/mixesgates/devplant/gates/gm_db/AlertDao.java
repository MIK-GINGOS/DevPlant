package com.mixesgates.devplant.gates.gm_db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    List<AlertRow> all();

    @Query("SELECT COUNT(*) FROM alerts WHERE read = 0")
    int unreadCount();

    /** Stops the same warning being raised again and again for one field. */
    @Query("SELECT COUNT(*) FROM alerts WHERE subjectKey = :subjectKey AND fieldId IS :fieldId "
            + "AND createdAt >= :since")
    int countSince(String subjectKey, Long fieldId, long since);

    @Insert
    long insert(AlertRow row);

    @Query("UPDATE alerts SET read = 1 WHERE id = :id")
    void markRead(long id);

    @Query("UPDATE alerts SET read = 1")
    void markAllRead();
}
