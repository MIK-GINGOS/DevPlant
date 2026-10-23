package com.mixesgates.devplant.contracts;

import java.util.List;

/** Outbreak warnings. */
public interface AlertStore {

    /** Newest first. */
    List<Alert> alerts();

    int unreadCount();

    void markRead(long alertId);

    void markAllRead();
}
