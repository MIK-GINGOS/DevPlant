package com.mixesgates.devplant.contracts;

/** What the farmer agreed to share, set on the welcome screen. Photos are never part of it. */
public final class Consent {

    /** False until the farmer has answered the welcome screen once. */
    public final boolean decided;
    /** Send report details (no photos) when the phone is online. */
    public final boolean share;
    public final String country;
    public final String district;

    public Consent(boolean decided, boolean share, String country, String district) {
        this.decided = decided;
        this.share = share;
        this.country = country;
        this.district = district;
    }
}
