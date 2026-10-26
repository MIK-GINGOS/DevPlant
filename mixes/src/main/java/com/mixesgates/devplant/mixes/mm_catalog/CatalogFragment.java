package com.mixesgates.devplant.mixes.mm_catalog;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Plant;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.ArrayList;
import java.util.List;

/** The plant catalogue: every plant a scan can name, with its photo. Search and filter by category. */
public class CatalogFragment extends Fragment {

    private static final class Data {
        String query;
        String category;
        List<String> categories;
        List<Plant> plants;
        int total;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Adapter adapter = new Adapter();
    private ChipGroup cats;
    private TextView status;
    private RecyclerView grid;
    private FrameLayout message;
    private String query = "";
    private String category;
    private boolean chipsBuilt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.column(c);
        int side = Ui.dp(c, 16);
        LinearLayout top = Ui.column(c);
        top.setPadding(side, Ui.dp(c, 8), side, 0);

        EditText search = new EditText(c);
        search.setHint("Search plants or problems");
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setTextColor(Ui.color(c, R.color.mm_ink));
        search.setHintTextColor(Ui.color(c, R.color.mm_ink_soft));
        GradientDrawable box = Ui.rounded(Ui.color(c, R.color.mm_surface), Ui.dp(c, 14));
        box.setStroke(Ui.dp(c, 1), Ui.color(c, R.color.mm_line));
        search.setBackground(box);
        search.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacksAndMessages(null);
                query = s.toString();
                handler.postDelayed(CatalogFragment.this::reload, 250);
            }
        });
        top.addView(search);

        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        cats = new ChipGroup(c);
        cats.setSingleLine(true);
        cats.setSingleSelection(true);
        cats.setSelectionRequired(true);
        scroll.addView(cats);
        top.addView(Ui.gap(scroll, 4));

        LinearLayout line = Ui.row(c);
        status = Ui.text(c, "Loading the catalogue…", Ui.Type.CAPTION);
        line.addView(status, Ui.weight(1));
        TextView guide = Ui.text(c, "Pest guide →", Ui.Type.CAPTION);
        guide.setTextColor(Ui.color(c, R.color.mm_leaf));
        guide.setPadding(Ui.dp(c, 8), Ui.dp(c, 6), 0, Ui.dp(c, 6));
        line.addView(Ui.tappable(guide, v -> Nav.of(this).showTab(Nav.TAB_LIBRARY)));
        top.addView(Ui.gap(line, 2));
        root.addView(top);

        FrameLayout body = new FrameLayout(c);
        grid = new RecyclerView(c);
        grid.setLayoutManager(new GridLayoutManager(c, 2));
        grid.setAdapter(adapter);
        grid.setClipToPadding(false);
        int edge = Ui.dp(c, 10);
        grid.setPadding(edge, Ui.dp(c, 4), edge, Ui.dp(c, Ui.ISLAND_CLEARANCE_DP));
        body.addView(grid);
        message = new FrameLayout(c);
        message.setVisibility(View.GONE);
        body.addView(message);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).listen(getViewLifecycleOwner(), this::reload, Changes.Topic.CATALOG);
        reload();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void reload() {
        if (grid == null) return;
        String q = query;
        String cat = category;
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> {
            Data d = new Data();
            d.query = q;
            d.category = cat;
            d.categories = s.plants().categories();
            d.plants = s.plants().search(q, cat);
            d.total = s.plants().plants().size();
            return d;
        }, this::render, this::offline);
    }

    private void render(Data d) {
        if (!d.query.equals(query) || !TextUtils.equals(d.category, category)) return;
        Context c = requireContext();
        if (!chipsBuilt) {
            chipsBuilt = true;
            cats.removeAllViews();
            cats.addView(chip(c, "All", true, () -> pick(null)));
            for (String name : d.categories) cats.addView(chip(c, name, false, () -> pick(name)));
        }
        status.setText(d.plants.size() == d.total ? d.total + " plants"
                : d.plants.size() + " of " + d.total + " plants");
        adapter.show(d.plants);
        message.removeAllViews();
        if (d.plants.isEmpty()) {
            message.addView(Ui.empty(c, "No plants match", "Try another word or category."));
            message.setVisibility(View.VISIBLE);
        } else {
            message.setVisibility(View.GONE);
        }
    }

    /** Never downloaded and no signal: say so and offer a retry. */
    private void offline(Throwable t) {
        Context c = requireContext();
        status.setText("Catalogue not downloaded yet");
        adapter.show(new ArrayList<>());
        message.removeAllViews();
        LinearLayout box = Ui.column(c);
        box.addView(Ui.empty(c, "Connect to the internet once",
                "The plant catalogue downloads the first time, then works offline."));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        View retry = Ui.button(c, "Try again", true);
        retry.setOnClickListener(v -> reload());
        box.addView(retry, lp);
        message.addView(box);
        message.setVisibility(View.VISIBLE);
    }

    private void pick(String name) {
        category = name;
        reload();
    }

    private static Chip chip(Context c, String text, boolean checked, Runnable onPick) {
        Chip chip = new Chip(c);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setOnCheckedChangeListener((b, isChecked) -> {
            if (isChecked) onPick.run();
        });
        return chip;
    }

    /** Two-column grid of plant cards; photos load as cards scroll into view. */
    private final class Adapter extends RecyclerView.Adapter<Holder> {
        private List<Plant> plants = new ArrayList<>();

        @SuppressWarnings("NotifyDataSetChanged")
        void show(List<Plant> list) {
            plants = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(parent.getContext());
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Plant p = plants.get(position);
            h.name.setText(p.name);
            h.category.setText(p.category);
            h.root.setContentDescription(p.name);
            h.root.setOnClickListener(v -> Nav.of(CatalogFragment.this).openPlant(p.key));
            Store.of(h.root.getContext()).plantPhoto(h.photo, p, h.photoPx);
        }

        @Override
        public int getItemCount() {
            return plants.size();
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final LinearLayout root;
        final ImageView photo;
        final TextView name;
        final TextView category;
        final int photoPx;

        Holder(Context c) {
            this(c, Ui.column(c));
        }

        private Holder(Context c, LinearLayout card) {
            super(card);
            root = card;
            GradientDrawable bg = Ui.rounded(Ui.color(c, R.color.mm_surface), Ui.dp(c, 18));
            bg.setStroke(Ui.dp(c, 1), Ui.color(c, R.color.mm_line));
            card.setBackground(bg);
            card.setClipToOutline(true);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int m = Ui.dp(c, 6);
            lp.setMargins(m, m, m, m);
            card.setLayoutParams(lp);
            Ui.tappable(card, null);

            photo = new ImageView(c);
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setBackgroundColor(Ui.color(c, R.color.mm_leaf_soft));
            photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.addView(photo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 120)));
            photoPx = Ui.dp(c, 180);

            LinearLayout text = Ui.column(c);
            int pad = Ui.dp(c, 12);
            text.setPadding(pad, Ui.dp(c, 10), pad, pad);
            name = Ui.text(c, "", Ui.Type.HEADING);
            name.setTextSize(15);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            text.addView(name);
            category = Ui.text(c, "", Ui.Type.CAPTION);
            category.setTextSize(12);
            category.setSingleLine(true);
            category.setEllipsize(TextUtils.TruncateAt.END);
            text.addView(category);
            card.addView(text);
        }
    }
}
