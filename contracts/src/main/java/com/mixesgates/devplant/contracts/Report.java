package com.mixesgates.devplant.contracts;

/**
 * One scan as a report an extension officer could review: what was found, how sure, where, and
 * whether it has left the phone. Kept beside the scan, which it never changes.
 */
public final class Report {

    /** Who produced the result. */
    public enum Source { SIMULATED, AI_MODEL, EXPERT }

    /** Kept apart on purpose: a report is not a confirmed case until someone checks the field. */
    public enum Verification { REPORTED, UNDER_REVIEW, FIELD_VERIFIED }

    /** LOCAL stays on the phone; QUEUED waits for a connection; SENT has reached the officers. */
    public enum Sync { LOCAL, QUEUED, SENT }

    public final long id;
    /** The saved scan, or 0 for an uncertain photo the rules asked to retake. */
    public final long scanId;
    public final long createdAt;
    public final String photoPath;
    public final String cropName;
    public final String country;
    public final String district;
    public final Kind kind;
    public final String resultKey;
    public final String resultName;
    public final float confidence;
    /** Why the result is uncertain, or null. */
    public final String uncertainReason;
    public final Source source;
    public final Verification verification;
    public final Sync sync;
    /** What the extension officer wrote back, or null. */
    public final String officerNote;

    public Report(long id, long scanId, long createdAt, String photoPath, String cropName,
                  String country, String district, Kind kind, String resultKey, String resultName,
                  float confidence, String uncertainReason, Source source,
                  Verification verification, Sync sync, String officerNote) {
        this.id = id;
        this.scanId = scanId;
        this.createdAt = createdAt;
        this.photoPath = photoPath;
        this.cropName = cropName;
        this.country = country;
        this.district = district;
        this.kind = kind;
        this.resultKey = resultKey;
        this.resultName = resultName;
        this.confidence = confidence;
        this.uncertainReason = uncertainReason;
        this.source = source;
        this.verification = verification;
        this.sync = sync;
        this.officerNote = officerNote;
    }

    /** An uncertain photo kept instead of thrown away; it has no saved scan. */
    public boolean isUncertain() {
        return scanId == 0;
    }
}
