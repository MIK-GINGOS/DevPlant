package com.mixesgates.devplant.mixes.mm_library;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.GuideEntry;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Treatment;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.List;

/** One disease or pest: symptoms, spread and treatments. */
public class GuideEntryFragment extends Fragment {

    private static final String ARG_KEY = "key";

    public static GuideEntryFragment newInstance(String key) {
        GuideEntryFragment f = new GuideEntryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        f.setArguments(args);
        return f;
    }

    private static final class Data {
        GuideEntry entry;
        List<Treatment> treatments;
    }

    private LinearLayout body;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        root.addView(Ui.topBar(c, "Guide", () -> Nav.of(this).back()));
        Ui.Page page = Ui.page(c, false);
        body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String key = requireArguments().getString(ARG_KEY);
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> {
            Data d = new Data();
            d.entry = s.diagnoses().guideEntry(key);
            d.treatments = s.diagnoses().treatmentsFor(key);
            return d;
        }, this::render);
    }

    private void render(Data d) {
        Context c = requireContext();
        body.removeAllViews();
        if (d.entry == null) {
            body.addView(Ui.empty(c, "Not in the guide", "This entry could not be found."));
            return;
        }
        GuideEntry e = d.entry;
        body.addView(Ui.text(c, e.name, Ui.Type.TITLE));
        TextView sci = Ui.text(c, e.scientificName, Ui.Type.CAPTION);
        sci.setTypeface(sci.getTypeface(), Typeface.ITALIC);
        body.addView(Ui.gap(sci, 2));
        LinearLayout pills = Ui.row(c);
        pills.addView(Ui.pill(c, Ui.kindLabel(e.kind), Ui.kindColor(e.kind)));
        body.addView(Ui.gap(pills, 8));

        body.addView(section(c, "Crops", e.crops));
        body.addView(section(c, "Symptoms", e.symptoms));
        body.addView(section(c, e.kind == Kind.PEST ? "How it spreads and feeds" : "How it spreads", e.spread));

        if (!d.treatments.isEmpty()) {
            body.addView(Ui.gap(Ui.text(c, "Treatments", Ui.Type.HEADING), 20));
            GuideViews.treatments(c, body, d.treatments);
        }
    }

    private static View section(Context c, String label, String text) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.text(c, label, Ui.Type.LABEL));
        card.addView(Ui.gap(Ui.text(c, text, Ui.Type.BODY), 4));
        return card;
    }
}
