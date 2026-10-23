package com.mixesgates.devplant.mixes.mm_result;

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
import com.mixesgates.devplant.contracts.Diagnosis;
import com.mixesgates.devplant.contracts.GuideEntry;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Scan;
import com.mixesgates.devplant.contracts.Treatment;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_library.GuideViews;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.Collections;
import java.util.List;

/** Diagnosis and advice for one scan. */
public class ResultFragment extends Fragment {

    private static final String ARG_SCAN = "scanId";

    public static ResultFragment newInstance(long scanId) {
        ResultFragment f = new ResultFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SCAN, scanId);
        f.setArguments(args);
        return f;
    }

    private static final class Data {
        Scan scan;
        List<Treatment> treatments = Collections.emptyList();
        GuideEntry entry;
    }

    private LinearLayout body;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        root.addView(Ui.topBar(c, "Diagnosis", () -> Nav.of(this).back()));
        Ui.Page page = Ui.page(c, false);
        body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long scanId = requireArguments().getLong(ARG_SCAN);
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> {
            Data d = new Data();
            d.scan = s.scans().get(scanId);
            if (d.scan != null && d.scan.diagnosis != null) {
                d.treatments = s.diagnoses().treatmentsFor(d.scan.diagnosis.key);
                d.entry = s.diagnoses().guideEntry(d.scan.diagnosis.key);
            }
            return d;
        }, this::render);
    }

    private void render(Data data) {
        Context c = requireContext();
        body.removeAllViews();
        if (data.scan == null) {
            body.addView(Ui.empty(c, "Scan not found", "It may have been deleted."));
            return;
        }
        Scan scan = data.scan;

        ImageView photo = new ImageView(c);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackground(Ui.rounded(Ui.color(c, R.color.mm_line), Ui.dp(c, 18)));
        photo.setClipToOutline(true);
        photo.setContentDescription("Leaf photo");
        body.addView(photo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 240)));
        Store.of(c).photos.load(photo, scan.photoPath, Ui.dp(c, 480));

        Diagnosis d = scan.diagnosis;
        if (d == null) {
            body.addView(Ui.gap(Ui.text(c, "Not diagnosed", Ui.Type.TITLE), 16));
            return;
        }

        body.addView(Ui.gap(Ui.text(c, d.name, Ui.Type.TITLE), 16));
        LinearLayout pills = Ui.row(c);
        pills.addView(Ui.pill(c, Ui.kindLabel(d.kind), Ui.kindColor(d.kind)));
        if (d.kind == Kind.DISEASE || d.kind == Kind.PEST) {
            pills.addView(Ui.pill(c, Ui.severityLabel(d.severity), Ui.severityColor(d.severity)));
        }
        pills.addView(Ui.pill(c, Ui.percent(d.confidence) + " sure", R.color.mm_unsure));
        body.addView(Ui.gap(pills, 8));
        String where = (scan.fieldName == null ? "No field" : scan.fieldName) + " · " + Ui.dateTime(scan.takenAt);
        body.addView(Ui.gap(Ui.text(c, where, Ui.Type.CAPTION), 8));

        LinearLayout advice = Ui.card(c);
        advice.setBackground(Ui.rounded(Ui.color(c, R.color.mm_leaf_soft), Ui.dp(c, 18)));
        advice.addView(Ui.text(c, "What to do", Ui.Type.LABEL));
        advice.addView(Ui.gap(Ui.text(c, d.advice, Ui.Type.BODY), 6));
        if (d.kind != Kind.HEALTHY) {
            advice.addView(Ui.gap(Ui.text(c, "Damaged leaf area: about " + Ui.percent(d.damage), Ui.Type.CAPTION), 8));
        }
        if (d.alternatives != null) {
            advice.addView(Ui.gap(Ui.text(c, "Other possibilities: " + d.alternatives, Ui.Type.CAPTION), 4));
        }
        body.addView(advice);

        if (!data.treatments.isEmpty()) {
            body.addView(Ui.gap(Ui.text(c, "Treatments", Ui.Type.HEADING), 20));
            GuideViews.treatments(c, body, data.treatments);
        }

        if (data.entry != null) {
            GuideEntry e = data.entry;
            LinearLayout about = Ui.card(c);
            about.addView(Ui.text(c, "About " + e.name, Ui.Type.HEADING));
            about.addView(Ui.gap(Ui.text(c, e.symptoms, Ui.Type.BODY), 6));
            View open = Ui.gap(Ui.button(c, "Open in guide", false), 8);
            open.setOnClickListener(v -> Nav.of(this).openGuide(e.key));
            about.addView(open);
            body.addView(Ui.gap(about, 20));
        }

        LinearLayout actions = Ui.row(c);
        View again = Ui.button(c, "Scan another leaf", true);
        again.setOnClickListener(v -> Nav.of(this).showTab(Nav.TAB_SCAN));
        actions.addView(again, Ui.weight(1));
        View spacer = new View(c);
        actions.addView(spacer, new LinearLayout.LayoutParams(Ui.dp(c, 12), 1));
        View delete = Ui.button(c, "Delete", false);
        delete.setOnClickListener(v -> confirmDelete(scan.id));
        actions.addView(delete);
        body.addView(Ui.gap(actions, 24));
    }

    private void confirmDelete(long scanId) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete this scan?")
                .setMessage("The photo and diagnosis will be removed from this phone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    Store.of(requireContext()).write(s -> {
                        s.scans().delete(scanId);
                        return null;
                    });
                    Nav.of(this).back();
                })
                .show();
    }
}
