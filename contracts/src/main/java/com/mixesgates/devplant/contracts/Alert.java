package com.mixesgates.devplant.contracts;

/** An outbreak warning. */
public final class Alert {

    public enum Level { WATCH, WARNING }

    public final long id;
    public final long createdAt;
    public final Level level;
    public final String title;
    public final String body;
    /** Guide key the alert is about, or null. */
    public final String key;
    /** 0 when not tied to a field. */
    public final long fieldId;
    public final boolean read;

    public Alert(long id, long createdAt, Level level, String title, String body, String key,
                 long fieldId, boolean read) {
        this.id = id;
        this.createdAt = createdAt;
        this.level = level;
        this.title = title;
        this.body = body;
        this.key = key;
        this.fieldId = fieldId;
        this.read = read;
    }
}
