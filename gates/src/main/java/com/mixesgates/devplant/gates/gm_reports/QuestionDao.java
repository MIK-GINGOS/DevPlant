package com.mixesgates.devplant.gates.gm_reports;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY askedAt DESC")
    List<QuestionRow> all();

    @Query("SELECT * FROM questions WHERE id = :id")
    QuestionRow get(long id);

    @Insert
    long insert(QuestionRow row);

    @Query("SELECT * FROM questions WHERE sync = 'QUEUED' ORDER BY askedAt LIMIT :limit")
    List<QuestionRow> queued(int limit);

    @Query("UPDATE questions SET sync = 'SENT' WHERE id IN (:ids)")
    void markSent(List<Long> ids);

    @Query("SELECT COUNT(*) FROM questions WHERE sync = 'SENT'")
    int sentCount();

    /** Returns 1 for a new answer, 0 if the phone already had it. */
    @Query("UPDATE questions SET answer = :answer, answeredBy = :by, answeredAt = :at "
            + "WHERE id = :id AND IFNULL(answer, '') != :answer")
    int applyAnswer(long id, String answer, String by, long at);
}
