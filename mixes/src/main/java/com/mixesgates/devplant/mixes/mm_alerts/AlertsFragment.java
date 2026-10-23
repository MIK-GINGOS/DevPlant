package com.mixesgates.devplant.mixes.mm_alerts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.Alert;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.List;

/** Outbreak warnings raised from the farmer's own scans. */
public class AlertsFragment extends Fragment {

    private LinearLayout body;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        LinearLayout bar = Ui.topBar(c, "Alerts", () -> Nav.of(this).back());
        View readAll = Ui.button(c, "Mark all read", false);
        readAll.setOnClickListener(v -> Store.of(c).write(s -> {
            s.alerts().markAllRead();
            return null;
        }));
        bar.addView(readAll);
        root.addView(bar);
        Ui.Page page = Ui.page(c, false);
        body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).listen(getViewLifecycleOwner(), this::reload, Changes.Topic.ALERTS);
        reload();
    }

    private void reload() {
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> s.alerts().alerts(), this::render);
    }

    private void render(List<Alert> alerts) {
        Context c = requireContext();
        body.removeAllViews();
        if (alerts.isEmpty()) {
            body.addView(Ui.empty(c, "No warnings",
                    "DevPlant warns you when a problem keeps showing up in a field or a scan finds severe damage."));
            return;
        }
        for (Alert a : alerts) {
            LinearLayout card = Ui.card(c);
            if (!a.read) card.setBackground(Ui.rounded(Ui.color(c, R.color.mm_leaf_soft), Ui.dp(c, 18)));
            LinearLayout top = Ui.row(c);
            boolean warning = a.level == Alert.Level.WARNING;
            top.addView(Ui.pill(c, warning ? "Warning" : "Watch", warning ? R.color.mm_severe : R.color.mm_moderate));
            if (!a.read) top.addView(Ui.pill(c, "New", R.color.mm_leaf));
            card.addView(top);
            card.addView(Ui.gap(Ui.text(c, a.title, Ui.Type.HEADING), 8));
            card.addView(Ui.gap(Ui.text(c, a.body, Ui.Type.BODY), 4));
            card.addView(Ui.gap(Ui.text(c, Ui.ago(a.createdAt), Ui.Type.CAPTION), 6));
            Ui.tappable(card, v -> {
                if (!a.read) Store.of(c).write(s -> {
                    s.alerts().markRead(a.id);
                    return null;
                });
                if (a.key != null) Nav.of(this).openGuide(a.key);
            });
            body.addView(card);
        }
        body.addView(Ui.gap(Ui.text(c, "Regional outbreak alerts from the internet are planned.",
                Ui.Type.CAPTION), 12));
    }
}
