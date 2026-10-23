package com.mixesgates.devplant.contracts;

/** Either a saved scan with its diagnosis, or a request to take the photo again. */
public final class ScanOutcome {
    public final boolean retake;
    /** Why a retake is needed; null otherwise. */
    public final String retakeReason;
    /** Null on retake. */
    public final Scan scan;

    private ScanOutcome(boolean retake, String retakeReason, Scan scan) {
        this.retake = retake;
        this.retakeReason = retakeReason;
        this.scan = scan;
    }

    public static ScanOutcome saved(Scan scan) {
        return new ScanOutcome(false, null, scan);
    }

    public static ScanOutcome retake(String reason) {
        return new ScanOutcome(true, reason, null);
    }
}
