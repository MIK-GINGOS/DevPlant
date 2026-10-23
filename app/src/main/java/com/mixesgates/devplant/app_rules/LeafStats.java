package com.mixesgates.devplant.app_rules;

/** What a prepared leaf picture looks like, measured from its colours. */
public final class LeafStats {
    /** 0..1, share of the picture that is plant material. */
    public final float leafFraction;
    /** 0..1, share of the plant material that is yellow or brown. */
    public final float damage;

    public LeafStats(float leafFraction, float damage) {
        this.leafFraction = leafFraction;
        this.damage = damage;
    }
}
