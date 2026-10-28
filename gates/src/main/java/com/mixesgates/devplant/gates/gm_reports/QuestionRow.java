package com.mixesgates.devplant.gates.gm_reports;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A question to an extension officer. Added in version 2 of devplant_reports.db. */
@Entity(tableName = "questions")
public class QuestionRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    /** The report it is about, or 0. */
    public long reportId;
    public long askedAt;
    @NonNull
    public String text = "";
    public String cropName;
    public String answer;
    public String answeredBy;
    public long answeredAt;
    /** QUEUED or SENT. */
    @NonNull
    public String sync = "QUEUED";
}
