package com.mixesgates.devplant.wiring;

import com.mixesgates.devplant.contracts.Changes;

import java.util.concurrent.CopyOnWriteArrayList;

/** In-process change events, delivered on the posting thread. */
final class SimpleChanges implements Changes {

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void listen(Listener listener) {
        listeners.addIfAbsent(listener);
    }

    @Override
    public void unlisten(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void post(Topic topic) {
        for (Listener listener : listeners) listener.onChanged(topic);
    }
}
