package com.mixesgates.devplant.mixes.mm_catalog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.Plant;
import com.mixesgates.devplant.contracts.PlantProblem;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

/** One catalogue plant: its photo, its 4 common problems and what to do about each. */
public class PlantFragment extends Fragment {

    private static final String ARG_KEY = "key";

    public static PlantFragment newInstance(String key) {
        PlantFragment f = new PlantFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        f.setArguments(args);
        return f;
    }

    private LinearLayout body;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        root.addView(Ui.topBar(c, "Plant catalogue", () -> Nav.of(this).back()));
        Ui.Page page = Ui.page(c, false);
        body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String key = requireArguments().getString(ARG_KEY);
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> s.plants().plant(key), this::render);
    }

    private void render(Plant p) {
        Context c = requireContext();
        body.removeAllViews();
        if (p == null) {
            body.addView(Ui.empty(c, "Not in the catalogue", "This plant could not be found."));
            return;
        }
        ImageView photo = new ImageView(c);
        photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photo.setBackground(Ui.rounded(Ui.color(c, R.color.mm_leaf_soft), Ui.dp(c, 18)));
        photo.setClipToOutline(true);
        photo.setContentDescription("Photo of " + p.name);
        body.addView(photo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 240)));
        Store.of(c).plantPhoto(photo, p, Ui.dp(c, 360));

        body.addView(Ui.gap(Ui.text(c, p.name, Ui.Type.TITLE), 16));
        TextView sci = Ui.text(c, p.scientificName, Ui.Type.CAPTION);
        sci.setTypeface(sci.getTypeface(), Typeface.ITALIC);
        body.addView(Ui.gap(sci, 2));
        LinearLayout pills = Ui.row(c);
        pills.addView(Ui.pill(c, p.category, R.color.mm_leaf));
        body.addView(Ui.gap(pills, 8));

        body.addView(Ui.gap(Ui.text(c, "Common problems", Ui.Type.HEADING), 20));
        int n = 1;
        for (PlantProblem x : p.problems) body.addView(problem(c, n++, x));

        if (!p.imageSource.isEmpty()) {
            TextView credit = Ui.text(c, "Photo: " + p.imageCredit + " · tap to see the source",
                    Ui.Type.CAPTION);
            credit.setTextSize(12);
            body.addView(Ui.gap(Ui.tappable(credit, v -> openLink(p.imageSource)), 16));
        }
        body.addView(Ui.gap(Ui.text(c,
                "Advice is general. Check with a local extension officer and follow product labels.",
                Ui.Type.CAPTION), 8));
    }

    private static View problem(Context c, int number, PlantProblem x) {
        LinearLayout card = Ui.card(c);
        LinearLayout top = Ui.row(c);
        top.addView(Ui.pill(c, String.valueOf(number), R.color.mm_disease));
        top.addView(Ui.text(c, x.name, Ui.Type.HEADING), Ui.weight(1));
        card.addView(top);
        card.addView(Ui.gap(Ui.text(c, "Signs", Ui.Type.LABEL), 10));
        card.addView(Ui.gap(Ui.text(c, x.signs, Ui.Type.BODY), 2));
        card.addView(Ui.gap(Ui.text(c, "What to do", Ui.Type.LABEL), 10));
        card.addView(Ui.gap(Ui.text(c, x.treatment, Ui.Type.BODY), 2));
        return card;
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
            // no browser on the phone
        }
    }
}
