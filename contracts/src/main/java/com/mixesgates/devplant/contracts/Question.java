package com.mixesgates.devplant.contracts;

/** A farmer's question to an extension officer, maybe about one of their reports. */
public final class Question {

    public final long id;
    /** The report it is about, or 0. */
    public final long reportId;
    public final long askedAt;
    public final String text;
    public final String cropName;
    /** The officer's answer, or null while there is none. */
    public final String answer;
    public final String answeredBy;
    /** 0 while unanswered. */
    public final long answeredAt;
    /** QUEUED until it has reached the officers, then SENT. Never LOCAL: asking is sharing. */
    public final Report.Sync sync;

    public Question(long id, long reportId, long askedAt, String text, String cropName,
                    String answer, String answeredBy, long answeredAt, Report.Sync sync) {
        this.id = id;
        this.reportId = reportId;
        this.askedAt = askedAt;
        this.text = text;
        this.cropName = cropName;
        this.answer = answer;
        this.answeredBy = answeredBy;
        this.answeredAt = answeredAt;
        this.sync = sync;
    }

    public boolean isAnswered() {
        return answer != null;
    }
}
