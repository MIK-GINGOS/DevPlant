package com.mixesgates.devplant.mixes.mm_search;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.Crop;
import com.mixesgates.devplant.contracts.GuideEntry;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_library.GuideViews;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.List;

/** Search crops, diseases and pests. */
public class SearchFragment extends Fragment {

    private static final class Results {
        String query;
        List<Crop> crops;
        List<GuideEntry> entries;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private EditText input;
    private LinearLayout body;
    private String latest = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        LinearLayout bar = Ui.topBar(c, "", () -> Nav.of(this).back());
        input = new EditText(c);
        input.setHint("Crops, diseases, pests, symptoms");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        bar.removeViewAt(1);
        bar.addView(input, Ui.weight(1));
        root.addView(bar);
        Ui.Page page = Ui.page(c, false);
        body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        body.addView(Ui.text(c, "Try \"blight\", \"maize\" or \"yellow\".", Ui.Type.CAPTION));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacksAndMessages(null);
                String q = s.toString();
                handler.postDelayed(() -> search(q), 250);
            }
        });
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        input.post(() -> imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT));
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void search(String query) {
        latest = query;
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> {
            Results r = new Results();
            r.query = query;
            r.crops = s.crops().searchCrops(query);
            r.entries = s.diagnoses().searchGuide(query);
            return r;
        }, this::render);
    }

    private void render(Results r) {
        if (!r.query.equals(latest)) return;
        Context c = requireContext();
        body.removeAllViews();
        if (r.query.trim().isEmpty()) return;
        if (r.crops.isEmpty() && r.entries.isEmpty()) {
            body.addView(Ui.empty(c, "No matches", "Check the spelling or try a symptom."));
            return;
        }
        if (!r.crops.isEmpty()) {
            body.addView(Ui.text(c, "Crops", Ui.Type.LABEL));
            for (Crop crop : r.crops) {
                LinearLayout card = Ui.card(c);
                card.addView(Ui.text(c, crop.name, Ui.Type.HEADING));
                card.addView(Ui.text(c, crop.scientificName, Ui.Type.CAPTION));
                body.addView(card);
            }
        }
        if (!r.entries.isEmpty()) {
            body.addView(Ui.gap(Ui.text(c, "Diseases and pests", Ui.Type.LABEL), r.crops.isEmpty() ? 0 : 16));
            for (GuideEntry e : r.entries) {
                body.addView(GuideViews.entryRow(c, e, v -> Nav.of(this).openGuide(e.key)));
            }
        }
    }
}
