package com.mixesgates.devplant.contracts;

import java.io.IOException;
import java.util.List;

/** Save and list scans. Calls block; the caller picks the thread. */
public interface ScanStore {

    /** Takes one leaf photo all the way to a saved diagnosis, or asks for a retake. */
    ScanOutcome scan(ScanRequest request) throws IOException;

    /** Newest first, each with its diagnosis. */
    List<Scan> recent(int limit);

    /** Null when the scan no longer exists. */
    Scan get(long scanId);

    void delete(long scanId);

    /** Field health numbers for scans taken at or after {@code sinceMillis}. */
    HealthSummary health(long sinceMillis);
}
