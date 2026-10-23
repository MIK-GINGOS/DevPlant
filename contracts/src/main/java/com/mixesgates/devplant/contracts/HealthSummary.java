package com.mixesgates.devplant.contracts;

import java.util.List;

/** Dashboard numbers over a period. */
public final class HealthSummary {
    public final long since;
    public final int scans;
    public final int healthy;
    public final int diseases;
    public final int pests;
    public final int unsure;
    public final int unreadAlerts;
    public final List<FieldHealth> fields;

    public HealthSummary(long since, int scans, int healthy, int diseases, int pests, int unsure,
                         int unreadAlerts, List<FieldHealth> fields) {
        this.since = since;
        this.scans = scans;
        this.healthy = healthy;
        this.diseases = diseases;
        this.pests = pests;
        this.unsure = unsure;
        this.unreadAlerts = unreadAlerts;
        this.fields = fields;
    }
}
