package com.mixesgates.devplant.gates.gm_reports;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reports", indices = {@Index("scanId"), @Index("createdAt")})
public class ReportRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    /** 0 for an uncertain photo that was never saved as a scan. */
    public long scanId;
    public long createdAt;
    public String photoPath;
    public String cropName;
    public String country;
    public String district;
    @NonNull
    public String kind = "UNSURE";
    public String resultKey;
    public String resultName;
    public float confidence;
    public String uncertainReason;
    /** SIMULATED, AI_MODEL or EXPERT. */
    @NonNull
    public String source = "SIMULATED";
    /** REPORTED, UNDER_REVIEW or FIELD_VERIFIED. */
    @NonNull
    public String verification = "REPORTED";
    /** LOCAL, QUEUED or SENT. */
    @NonNull
    public String sync = "LOCAL";
    /** What the extension officer wrote back, or null. Added in version 2. */
    public String officerNote;
}
