package com.mixesgates.devplant.contracts;

/** Scan counts for one field (fieldId 0 = scans with no field). */
public final class FieldHealth {
    public final long fieldId;
    public final String fieldName;
    public final int scans;
    public final int healthy;
    public final int sick;

    public FieldHealth(long fieldId, String fieldName, int scans, int healthy, int sick) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.scans = scans;
        this.healthy = healthy;
        this.sick = sick;
    }

    /** 0..100, share of decided scans that were healthy; -1 when there is nothing to judge. */
    public int score() {
        int decided = healthy + sick;
        return decided == 0 ? -1 : Math.round(100f * healthy / decided);
    }
}
