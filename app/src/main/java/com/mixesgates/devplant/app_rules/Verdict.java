package com.mixesgates.devplant.app_rules;

import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Severity;

/** What {@link DiagnosisRules} decided. */
public final class Verdict {
    public final boolean retake;
    public final String retakeReason;
    public final Kind kind;
    public final String key;
    public final String name;
    public final float confidence;
    public final Severity severity;
    public final float damage;
    public final String alternatives;
    public final String advice;

    Verdict(boolean retake, String retakeReason, Kind kind, String key, String name,
            float confidence, Severity severity, float damage, String alternatives, String advice) {
        this.retake = retake;
        this.retakeReason = retakeReason;
        this.kind = kind;
        this.key = key;
        this.name = name;
        this.confidence = confidence;
        this.severity = severity;
        this.damage = damage;
        this.alternatives = alternatives;
        this.advice = advice;
    }

    static Verdict retake(String reason) {
        return new Verdict(true, reason, Kind.UNSURE, "", "", 0f, Severity.NONE, 0f, null, null);
    }

    public boolean isProblem() {
        return kind == Kind.DISEASE || kind == Kind.PEST;
    }
}
