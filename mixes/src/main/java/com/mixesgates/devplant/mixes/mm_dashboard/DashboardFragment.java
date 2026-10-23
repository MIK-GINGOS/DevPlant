package com.mixesgates.devplant.mixes.mm_dashboard;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.FieldHealth;
import com.mixesgates.devplant.contracts.HealthSummary;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

/** Field health numbers over the last 30 days. */
public class DashboardFragment extends Fragment {

    private static final long DAYS_30 = 30L * 24 * 60 * 60 * 1000;

    private LinearLayout body;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Ui.Page page = Ui.page(requireContext(), true);
        body = page.body;
        return page.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).listen(getViewLifecycleOwner(), this::reload,
                Changes.Topic.SCANS, Changes.Topic.CROPS, Changes.Topic.ALERTS);
        reload();
    }

    private void reload() {
        long since = System.currentTimeMillis() - DAYS_30;
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> s.scans().health(since), this::render);
    }

    private void render(HealthSummary h) {
        Context c = requireContext();
        body.removeAllViews();
        body.addView(Ui.text(c, "Last 30 days", Ui.Type.LABEL));

        int decided = h.healthy + h.diseases + h.pests;
        LinearLayout row1 = Ui.row(c);
        row1.addView(tile(c, String.valueOf(h.scans), "Scans", R.color.mm_ink), Ui.weight(1));
        row1.addView(spacer(c));
        row1.addView(tile(c, decided == 0 ? "–" : Math.round(100f * h.healthy / decided) + "%",
                "Healthy", R.color.mm_healthy), Ui.weight(1));
        body.addView(Ui.gap(row1, 8));
        LinearLayout row2 = Ui.row(c);
        row2.addView(tile(c, String.valueOf(h.diseases), "Diseases", R.color.mm_disease), Ui.weight(1));
        row2.addView(spacer(c));
        row2.addView(tile(c, String.valueOf(h.pests), "Pests", R.color.mm_pest), Ui.weight(1));
        body.addView(Ui.gap(row2, 12));
        if (h.unsure > 0) {
            body.addView(Ui.gap(Ui.text(c, h.unsure + " scans could not be matched to the guide.", Ui.Type.CAPTION), 8));
        }

        LinearLayout alerts = Ui.card(c);
        LinearLayout top = Ui.row(c);
        top.addView(Ui.text(c, h.unreadAlerts == 0 ? "No new warnings"
                : h.unreadAlerts + (h.unreadAlerts == 1 ? " new warning" : " new warnings"), Ui.Type.HEADING), Ui.weight(1));
        top.addView(Ui.icon(c, R.drawable.mm_ic_bell,
                Ui.color(c, h.unreadAlerts == 0 ? R.color.mm_ink_soft : R.color.mm_severe), 22));
        alerts.addView(top);
        body.addView(Ui.tappable(alerts, v -> Nav.of(this).openAlerts()));

        body.addView(Ui.gap(Ui.text(c, "By field", Ui.Type.LABEL), 20));
        if (h.fields.isEmpty()) {
            LinearLayout card = Ui.card(c);
            card.addView(Ui.text(c, "Nothing to show yet", Ui.Type.HEADING));
            card.addView(Ui.gap(Ui.text(c, "Scan a few leaves to see how your fields are doing.", Ui.Type.BODY), 4));
            View scan = Ui.gap(Ui.button(c, "Scan a leaf", true), 12);
            scan.setOnClickListener(v -> Nav.of(this).showTab(Nav.TAB_SCAN));
            card.addView(scan);
            body.addView(card);
        }
        for (FieldHealth f : h.fields) body.addView(fieldRow(c, f));
    }

    private static View tile(Context c, String number, String label, int colorRes) {
        LinearLayout card = Ui.card(c);
        card.setLayoutParams(Ui.weight(1));
        android.widget.TextView n = Ui.text(c, number, Ui.Type.NUMBER);
        n.setTextColor(Ui.color(c, colorRes));
        card.addView(n);
        card.addView(Ui.text(c, label, Ui.Type.CAPTION));
        return card;
    }

    private static View spacer(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, 12), 1));
        return v;
    }

    private static View fieldRow(Context c, FieldHealth f) {
        LinearLayout card = Ui.card(c);
        LinearLayout top = Ui.row(c);
        top.addView(Ui.text(c, f.fieldName == null ? "No field" : f.fieldName, Ui.Type.HEADING), Ui.weight(1));
        int score = f.score();
        top.addView(Ui.text(c, score < 0 ? "–" : score + "% healthy", Ui.Type.CAPTION));
        card.addView(top);

        LinearLayout bar = Ui.row(c);
        bar.setBackground(Ui.rounded(Ui.color(c, R.color.mm_line), Ui.dp(c, 5)));
        bar.setClipToOutline(true);
        if (f.healthy + f.sick > 0) {
            View good = new View(c);
            good.setBackgroundColor(Ui.color(c, R.color.mm_healthy));
            bar.addView(good, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f.healthy));
            View bad = new View(c);
            bad.setBackgroundColor(Ui.color(c, R.color.mm_severe));
            bar.addView(bad, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f.sick));
        }
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 10));
        barLp.topMargin = Ui.dp(c, 10);
        card.addView(bar, barLp);
        String counts = f.scans == 0 ? "No scans in 30 days"
                : f.scans + (f.scans == 1 ? " scan" : " scans") + " · " + f.sick + " with problems";
        card.addView(Ui.gap(Ui.text(c, counts, Ui.Type.CAPTION), 6));
        return card;
    }
}
