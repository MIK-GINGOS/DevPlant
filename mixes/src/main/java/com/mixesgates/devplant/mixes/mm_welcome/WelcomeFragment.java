package com.mixesgates.devplant.mixes.mm_welcome;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.Consent;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_reports.Reports;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

/**
 * Shown once on first launch, and again from My Reports: what DevPlant is, what stays on the
 * phone, and whether report details may be shared with extension officers.
 */
public class WelcomeFragment extends Fragment {

    private static final String[] COUNTRIES = {"Zambia", "Malawi", "Zimbabwe", "Botswana", "Other"};

    private RadioGroup countries;
    private EditText district;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        root.addView(Ui.topBar(c, "Welcome", () -> Nav.of(this).back()));
        Ui.Page page = Ui.page(c, true); // the menu island stays on top of this screen
        LinearLayout body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        body.addView(Ui.text(c, "Check a crop from one leaf", Ui.Type.TITLE));
        body.addView(Ui.gap(Ui.text(c, "Photograph one leaf and DevPlant suggests what may be wrong and what to do next. "
                + "A result is a first assessment, not a confirmed diagnosis.", Ui.Type.BODY), 8));

        LinearLayout stays = Ui.card(c);
        stays.setBackground(Ui.rounded(Ui.color(c, R.color.mm_leaf_soft), Ui.dp(c, 18)));
        stays.addView(Ui.text(c, "Stays on this phone", Ui.Type.LABEL));
        stays.addView(Ui.gap(Ui.text(c, "Your photos, always. DevPlant works with no signal.", Ui.Type.BODY), 4));
        body.addView(stays);

        LinearLayout share = Ui.card(c);
        share.addView(Ui.text(c, "Share reports with extension officers?", Ui.Type.HEADING));
        share.addView(Ui.gap(Ui.text(c, "If you agree, each scan is shared as a short text record: crop, result, how sure "
                + "DevPlant was, the date and the area you choose below. No photos, no exact location. "
                + "Records are sent when the phone has signal, and officers can reply with what they found.", Ui.Type.BODY), 6));
        share.addView(Ui.gap(Ui.text(c, "Country", Ui.Type.LABEL), 12));
        countries = new RadioGroup(c);
        for (String name : COUNTRIES) {
            RadioButton b = new RadioButton(c);
            b.setId(View.generateViewId());
            b.setText(name);
            b.setTextSize(15);
            countries.addView(b);
        }
        share.addView(Ui.gap(countries, 4));
        share.addView(Ui.gap(Ui.text(c, "District (optional)", Ui.Type.LABEL), 12));
        district = new EditText(c);
        district.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        district.setHint("For example Chipata");
        district.setSingleLine(true);
        share.addView(Ui.gap(district, 4));
        body.addView(share);

        View yes = Ui.gap(Ui.button(c, "Share reports", true), 20);
        yes.setOnClickListener(v -> save(true));
        body.addView(yes);
        View no = Ui.gap(Ui.button(c, "Keep everything on this phone", false), 8);
        no.setOnClickListener(v -> save(false));
        body.addView(no);
        body.addView(Ui.gap(Ui.text(c, "You can change this any time in My reports.", Ui.Type.CAPTION), 12));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) return; // the views restore what the farmer typed
        Context app = requireContext().getApplicationContext();
        Store.of(app).load(getViewLifecycleOwner(), s -> Reports.of(app).consent(), consent -> {
            for (int i = 0; i < COUNTRIES.length; i++) {
                if (COUNTRIES[i].equals(consent.country)) {
                    ((RadioButton) countries.getChildAt(i)).setChecked(true);
                }
            }
            if (consent.district != null) district.setText(consent.district);
        });
    }

    private void save(boolean share) {
        String country = null;
        int checked = countries.getCheckedRadioButtonId();
        for (int i = 0; i < COUNTRIES.length; i++) {
            if (countries.getChildAt(i).getId() == checked) country = COUNTRIES[i];
        }
        String typed = district.getText().toString().trim();
        Consent consent = new Consent(true, share, country, typed.isEmpty() ? null : typed);
        Context app = requireContext().getApplicationContext();
        Store.of(app).write(s -> {
            Reports.of(app).saveConsent(consent);
            return null;
        });
        Nav.of(this).back();
    }
}
