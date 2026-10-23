package com.mixesgates.devplant.app_db;

import com.mixesgates.devplant.contracts.Alert;
import com.mixesgates.devplant.contracts.AlertStore;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.gates.gm_db.AlertRow;
import com.mixesgates.devplant.gates.gm_db.DevPlantDatabase;

import java.util.ArrayList;
import java.util.List;

public final class RoomAlertStore implements AlertStore {

    private final DevPlantDatabase db;
    private final Changes changes;

    public RoomAlertStore(DevPlantDatabase db, Changes changes) {
        this.db = db;
        this.changes = changes;
    }

    @Override
    public List<Alert> alerts() {
        List<Alert> out = new ArrayList<>();
        for (AlertRow r : db.alerts().all()) out.add(Rows.alert(r));
        return out;
    }

    @Override
    public int unreadCount() {
        return db.alerts().unreadCount();
    }

    @Override
    public void markRead(long alertId) {
        db.alerts().markRead(alertId);
        changes.post(Changes.Topic.ALERTS);
    }

    @Override
    public void markAllRead() {
        db.alerts().markAllRead();
        changes.post(Changes.Topic.ALERTS);
    }
}
