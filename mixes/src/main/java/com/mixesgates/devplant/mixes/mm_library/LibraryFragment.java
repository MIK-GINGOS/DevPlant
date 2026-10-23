package com.mixesgates.devplant.mixes.mm_library;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.mixesgates.devplant.contracts.Crop;
import com.mixesgates.devplant.contracts.GuideEntry;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.ArrayList;
import java.util.List;

/** The pest and disease guide, filtered by type and crop. */
public class LibraryFragment extends Fragment {

    private static final class Data {
        List<GuideEntry> entries;
        List<Crop> crops;
    }

    private LinearLayout list;
    private ChipGroup cropChips;
    private Data data;
    private Kind kind;
    private String crop;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        Ui.Page page = Ui.page(c, true);
        LinearLayout body = page.body;

        ChipGroup kinds = new ChipGroup(c);
        kinds.setSingleSelection(true);
        kinds.setSelectionRequired(true);
        kinds.addView(chip(c, "All", true, () -> setKind(null)));
        kinds.addView(chip(c, "Diseases", false, () -> setKind(Kind.DISEASE)));
        kinds.addView(chip(c, "Pests", false, () -> setKind(Kind.PEST)));
        body.addView(kinds);

        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        cropChips = new ChipGroup(c);
        cropChips.setSingleLine(true);
        cropChips.setSingleSelection(true);
        cropChips.setSelectionRequired(true);
        scroll.addView(cropChips);
        body.addView(scroll);

        list = Ui.column(c);
        body.addView(list);
        return page.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> {
            Data d = new Data();
            d.entries = s.diagnoses().guide();
            d.crops = s.crops().crops();
            return d;
        }, d -> {
            data = d;
            Context c = requireContext();
            cropChips.removeAllViews();
            cropChips.addView(chip(c, "All crops", crop == null, () -> setCrop(null)));
            for (Crop cr : d.crops) {
                cropChips.addView(chip(c, cr.name, cr.name.equals(crop), () -> setCrop(cr.name)));
            }
            render();
        });
    }

    private void setKind(Kind k) {
        kind = k;
        render();
    }

    private void setCrop(String name) {
        crop = name;
        render();
    }

    private void render() {
        if (data == null || list == null) return;
        Context c = requireContext();
        list.removeAllViews();
        List<GuideEntry> shown = new ArrayList<>();
        for (GuideEntry e : data.entries) {
            if (kind != null && e.kind != kind) continue;
            if (crop != null && !(e.crops.contains(crop) || e.crops.contains("All crops"))) continue;
            shown.add(e);
        }
        if (shown.isEmpty()) {
            list.addView(Ui.empty(c, "Nothing here", "Try another crop or type."));
            return;
        }
        for (GuideEntry e : shown) {
            list.addView(GuideViews.entryRow(c, e, v -> Nav.of(this).openGuide(e.key)));
        }
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
}
