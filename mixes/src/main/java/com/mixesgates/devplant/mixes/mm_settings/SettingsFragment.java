package com.mixesgates.devplant.mixes.mm_settings;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

/** Theme, model and about. */
public class SettingsFragment extends Fragment {

    private android.widget.TextView modelName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = Ui.screen(c);
        root.addView(Ui.topBar(c, "Settings", () -> Nav.of(this).back()));
        Ui.Page page = Ui.page(c, false);
        LinearLayout body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout theme = Ui.card(c);
        theme.addView(Ui.text(c, "Theme", Ui.Type.LABEL));
        RadioGroup group = new RadioGroup(c);
        String[] modes = {ThemePrefs.SYSTEM, ThemePrefs.LIGHT, ThemePrefs.DARK};
        String[] labels = {"Follow the phone", "Light", "Dark"};
        String current = ThemePrefs.get(c);
        for (int i = 0; i < modes.length; i++) {
            RadioButton b = new RadioButton(c);
            b.setId(View.generateViewId());
            b.setText(labels[i]);
            b.setTextSize(15);
            group.addView(b);
            if (modes[i].equals(current)) b.setChecked(true);
            String mode = modes[i];
            b.setOnCheckedChangeListener((btn, checked) -> {
                if (checked && !mode.equals(ThemePrefs.get(c))) ThemePrefs.set(c, mode);
            });
        }
        theme.addView(Ui.gap(group, 4));
        body.addView(theme);

        LinearLayout model = Ui.card(c);
        model.addView(Ui.text(c, "Diagnosis model", Ui.Type.LABEL));
        modelName = Ui.text(c, "Loading…", Ui.Type.HEADING);
        model.addView(Ui.gap(modelName, 4));
        model.addView(Ui.gap(Ui.text(c, "Runs on this phone with no signal needed. "
                + "Sending unsure scans to an expert is planned.", Ui.Type.BODY), 4));
        body.addView(model);

        LinearLayout about = Ui.card(c);
        about.addView(Ui.text(c, "About", Ui.Type.LABEL));
        about.addView(Ui.gap(Ui.text(c, "DevPlant " + version(c), Ui.Type.HEADING), 4));
        about.addView(Ui.gap(Ui.text(c, "Early detection of crop pests and diseases from a photo of one leaf. "
                + "Photos and results stay on this phone.", Ui.Type.BODY), 4));
        about.addView(Ui.gap(Ui.text(c, "Advice is general. Confirm serious problems with your local "
                + "extension officer and always follow product labels.", Ui.Type.CAPTION), 8));
        body.addView(about);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> s.diagnoses().modelName(),
                name -> modelName.setText(name));
    }

    private static String version(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
