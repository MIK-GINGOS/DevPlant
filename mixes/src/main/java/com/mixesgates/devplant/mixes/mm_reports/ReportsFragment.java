package com.mixesgates.devplant.mixes.mm_reports;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Consent;
import com.mixesgates.devplant.contracts.Report;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_help.HelpFragment;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;
import com.mixesgates.devplant.mixes.mm_welcome.WelcomeFragment;

import java.util.List;

/** My Reports: every scan as a report, with where it stands and whether it has left the phone. */
public class ReportsFragment extends Fragment {

    private static final int LIMIT = 300;

    private static final class Data {
        Consent consent;
        List<Report> reports;
    }

    private LinearLayout body;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        root.addView(Ui.topBar(c, "My reports", () -> Nav.of(this).back()));
        Ui.Page page = Ui.page(c, true); // the menu island stays on top of this screen
        body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).listen(getViewLifecycleOwner(), this::reload, Changes.Topic.SCANS);
        reload();
        // Send what waits and fetch officers' statuses; anything new reloads the list.
        Reports.of(requireContext()).syncSoon();
    }

    private void reload() {
        Context app = requireContext().getApplicationContext();
        Store.of(app).load(getViewLifecycleOwner(), s -> {
            Data d = new Data();
            d.consent = Reports.of(app).consent();
            d.reports = Reports.of(app).recent(LIMIT);
            return d;
        }, this::render);
    }

    private void render(Data data) {
        Context c = requireContext();
        body.removeAllViews();

        LinearLayout sharing = Ui.card(c);
        sharing.setLayoutParams(Ui.below(c, 0));
        sharing.addView(Ui.text(c, "Sharing with extension officers", Ui.Type.LABEL));
        String where = Reports.area(data.consent.country, data.consent.district);
        String state = data.consent.share
                ? "On. Report details are sent when the phone has signal. Photos are never sent."
                : "Off. Everything stays on this phone, unless you ask an officer about a report.";
        sharing.addView(Ui.gap(Ui.text(c, state, Ui.Type.BODY), 4));
        if (where != null) sharing.addView(Ui.gap(Ui.text(c, "Area: " + where, Ui.Type.CAPTION), 4));
        View change = Ui.gap(Ui.button(c, "Change", false), 8);
        change.setOnClickListener(v -> Reports.open(this, new WelcomeFragment()));
        sharing.addView(change);
        body.addView(sharing);

        View ask = Ui.gap(Ui.button(c, "Ask an extension officer", true), 12);
        ask.setOnClickListener(v -> Reports.open(this, HelpFragment.newInstance(0)));
        body.addView(ask);

        if (data.reports.isEmpty()) {
            body.addView(Ui.empty(c, "No reports yet", "Each scan becomes a report here, including photos DevPlant could not read."));
            return;
        }
        for (Report r : data.reports) body.addView(card(c, r));
        body.addView(Ui.gap(Ui.text(c, "\"Reported\" means only that a scan was made. A case is confirmed only after "
                + "an officer checks the field.", Ui.Type.CAPTION), 16));
    }

    private View card(Context c, Report r) {
        LinearLayout card = Ui.card(c);
        card.setPadding(Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
        LinearLayout row = Ui.row(c);
        ImageView thumb = new ImageView(c);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setBackground(Ui.rounded(Ui.color(c, R.color.mm_line), Ui.dp(c, 12)));
        thumb.setClipToOutline(true);
        row.addView(thumb, new LinearLayout.LayoutParams(Ui.dp(c, 60), Ui.dp(c, 60)));
        Store.of(c).photos.load(thumb, r.photoPath, Ui.dp(c, 60));

        LinearLayout texts = Ui.column(c);
        texts.setPadding(Ui.dp(c, 12), 0, 0, 0);
        String title = r.isUncertain() ? "Not recognised yet" : (r.resultName == null ? "Not diagnosed" : r.resultName);
        texts.addView(Ui.text(c, title, Ui.Type.HEADING));
        StringBuilder caption = new StringBuilder();
        if (r.cropName != null) caption.append(r.cropName).append(" · ");
        String where = Reports.area(r.country, r.district);
        if (where != null) caption.append(where).append(" · ");
        caption.append(Ui.dateTime(r.createdAt));
        texts.addView(Ui.text(c, caption, Ui.Type.CAPTION));

        LinearLayout found = Ui.row(c);
        found.addView(Ui.pill(c, Ui.kindLabel(r.kind), Ui.kindColor(r.kind)));
        found.addView(Ui.pill(c, Reports.sourceLabel(r.source), R.color.mm_unsure));
        texts.addView(Ui.gap(found, 4));
        LinearLayout status = Ui.row(c);
        status.addView(Ui.pill(c, Reports.verificationLabel(r.verification), Reports.verificationColor(r.verification)));
        status.addView(Ui.pill(c, Reports.syncLabel(r.sync), R.color.mm_leaf));
        texts.addView(Ui.gap(status, 4));
        row.addView(texts, Ui.weight(1));
        card.addView(row);

        if (r.isUncertain() && r.uncertainReason != null) {
            card.addView(Ui.gap(Ui.text(c, r.uncertainReason, Ui.Type.CAPTION), 8));
        }
        if (r.officerNote != null) {
            card.addView(Ui.gap(Ui.text(c, "Officer: " + r.officerNote, Ui.Type.BODY), 8));
        }
        Ui.tappable(card, v -> {
            if (r.isUncertain()) confirmUncertain(r);
            else Nav.of(this).openResult(r.scanId);
        });
        return card;
    }

    private void confirmUncertain(Report r) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("We couldn't tell yet")
                .setMessage((r.uncertainReason == null ? "" : r.uncertainReason + "\n\n")
                        + "Try another clear photo, check more than one plant, or ask your extension officer from My reports.")
                .setNeutralButton("Delete", (dialog, which) -> {
                    Context app = requireContext().getApplicationContext();
                    Store.of(app).write(s -> {
                        Reports.of(app).delete(r.id);
                        return null;
                    });
                })
                .setNegativeButton("Close", null)
                .setPositiveButton("Scan again", (dialog, which) -> Nav.of(this).showTab(Nav.TAB_SCAN))
                .show();
    }
}
