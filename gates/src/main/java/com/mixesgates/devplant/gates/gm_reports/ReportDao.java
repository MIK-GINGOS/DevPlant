package com.mixesgates.devplant.gates.gm_reports;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC LIMIT :limit")
    List<ReportRow> recent(int limit);

    @Query("SELECT * FROM reports WHERE id = :id")
    ReportRow get(long id);

    @Query("SELECT * FROM reports WHERE scanId = :scanId LIMIT 1")
    ReportRow forScan(long scanId);

    /** Uncertain photos taken at or after {@code since}: earlier tries at the same leaf. */
    @Query("SELECT * FROM reports WHERE scanId = 0 AND createdAt >= :since")
    List<ReportRow> uncertainSince(long since);

    @Insert
    long insert(ReportRow row);

    @Query("DELETE FROM reports WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM reports WHERE scanId = :scanId")
    void deleteForScan(long scanId);

    @Query("UPDATE reports SET sync = :to WHERE sync = :from")
    void moveSync(String from, String to);

    @Query("UPDATE reports SET sync = :sync WHERE id = :id")
    void setSync(long id, String sync);

    /** Oldest first, so a long queue goes out in the order the farmer scanned. */
    @Query("SELECT * FROM reports WHERE sync = 'QUEUED' ORDER BY createdAt LIMIT :limit")
    List<ReportRow> queued(int limit);

    /** Only rows still QUEUED: a farmer who turned sharing off meanwhile keeps theirs LOCAL. */
    @Query("UPDATE reports SET sync = 'SENT' WHERE sync = 'QUEUED' AND id IN (:ids)")
    void markSent(List<Long> ids);

    @Query("SELECT COUNT(*) FROM reports WHERE sync = 'SENT'")
    int sentCount();

    /** Returns 1 if the officer changed something, 0 if it was already so. */
    @Query("UPDATE reports SET verification = :verification, officerNote = :note WHERE id = :id "
            + "AND (verification != :verification OR IFNULL(officerNote, '') != IFNULL(:note, ''))")
    int applyOfficer(long id, String verification, String note);
}
