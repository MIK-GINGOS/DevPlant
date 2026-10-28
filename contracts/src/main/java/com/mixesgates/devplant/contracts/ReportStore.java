package com.mixesgates.devplant.contracts;

import java.util.List;

/**
 * Reports kept beside the scans, the farmer's sharing choice, and questions to extension
 * officers. Calls block.
 */
public interface ReportStore {

    /** Newest first. */
    List<Report> recent(int limit);

    /** Null when the scan has no report (taken before reports existed). */
    Report forScan(long scanId);

    /** Removes an uncertain report and its photo. Saved scans are deleted through ScanStore. */
    void delete(long reportId);

    Consent consent();

    /** Saves the choice and moves waiting reports between LOCAL and QUEUED to match it. */
    void saveConsent(Consent consent);

    /** Newest first. */
    List<Question> questions();

    /**
     * Saves a question for an extension officer, about one report ({@code reportId}) or none (0),
     * and sends it as soon as the phone has signal. Asking sends that report's details too, even
     * when sharing is off, because the farmer chose to; never its photo.
     */
    Question ask(String text, long reportId);

    /**
     * Sends whatever is waiting and fetches officers' statuses, notes and answers, in the
     * background; does not block. Anything new is announced as a SCANS change. With no signal
     * everything waiting stays queued for the next try.
     */
    void syncSoon();
}
