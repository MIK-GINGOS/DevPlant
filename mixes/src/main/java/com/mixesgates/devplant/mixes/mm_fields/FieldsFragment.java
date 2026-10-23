package com.mixesgates.devplant.mixes.mm_fields;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Crop;
import com.mixesgates.devplant.contracts.Field;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** My crops and plots. */
public class FieldsFragment extends Fragment {

    private static final class Data {
        List<Field> fields;
        List<Crop> crops;
    }

    private LinearLayout list;
    private Data data;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        Ui.Page page = Ui.page(c, true);
        View add = Ui.button(c, "Add a field", true);
        add.setOnClickListener(v -> showAddDialog());
        page.body.addView(add);
        list = Ui.column(c);
        page.body.addView(list);
        return page.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).listen(getViewLifecycleOwner(), this::reload, Changes.Topic.CROPS);
        reload();
    }

    private void reload() {
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> {
            Data d = new Data();
            d.fields = s.crops().fields();
            d.crops = s.crops().crops();
            return d;
        }, d -> {
            data = d;
            render();
        });
    }

    private void render() {
        Context c = requireContext();
        list.removeAllViews();
        if (data.fields.isEmpty()) {
            list.addView(Ui.empty(c, "No fields yet",
                    "Add your plots so scans and outbreak warnings can be tracked per field."));
        }
        for (Field f : data.fields) {
            LinearLayout card = Ui.card(c);
            LinearLayout top = Ui.row(c);
            top.addView(Ui.text(c, f.name, Ui.Type.HEADING), Ui.weight(1));
            top.addView(Ui.pill(c, f.cropName, com.mixesgates.devplant.mixes.R.color.mm_leaf));
            card.addView(top);
            String size = f.areaHectares > 0
                    ? String.format(Locale.getDefault(), "%.2f ha", f.areaHectares) : "Size not set";
            card.addView(Ui.gap(Ui.text(c, size, Ui.Type.CAPTION), 2));
            if (f.notes != null && !f.notes.isEmpty()) {
                card.addView(Ui.gap(Ui.text(c, f.notes, Ui.Type.BODY), 6));
            }
            card.setOnLongClickListener(v -> {
                confirmDelete(f);
                return true;
            });
            list.addView(card);
        }

        list.addView(Ui.gap(Ui.text(c, "Crops in the guide", Ui.Type.LABEL), 24));
        LinearLayout crops = Ui.column(c);
        for (Crop crop : data.crops) {
            crops.addView(Ui.gap(Ui.text(c, crop.name + "  ·  " + crop.scientificName, Ui.Type.BODY), 6));
        }
        list.addView(crops);
        list.addView(Ui.gap(Ui.text(c, "Long-press a field to delete it. GPS field mapping is planned.",
                Ui.Type.CAPTION), 16));
    }

    private void showAddDialog() {
        if (data == null || data.crops.isEmpty()) return;
        Context c = requireContext();
        LinearLayout form = Ui.column(c);
        int pad = Ui.dp(c, 20);
        form.setPadding(pad, Ui.dp(c, 8), pad, 0);

        EditText name = new EditText(c);
        name.setHint("Field name, e.g. North plot");
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(name);

        Spinner crop = new Spinner(c);
        List<String> names = new ArrayList<>();
        for (Crop cr : data.crops) names.add(cr.name);
        crop.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_spinner_dropdown_item, names));
        form.addView(Ui.gap(crop, 8));

        EditText area = new EditText(c);
        area.setHint("Size in hectares (optional)");
        area.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(Ui.gap(area, 8));

        EditText notes = new EditText(c);
        notes.setHint("Notes (optional)");
        form.addView(Ui.gap(notes, 8));

        AlertDialog dialog = new MaterialAlertDialogBuilder(c)
                .setTitle("Add a field")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) {
                name.setError("Give the field a name");
                return;
            }
            double hectares = 0;
            String a = area.getText().toString().trim().replace(',', '.');
            if (!a.isEmpty()) {
                try {
                    hectares = Double.parseDouble(a);
                } catch (NumberFormatException e) {
                    area.setError("Enter a number");
                    return;
                }
            }
            long cropId = data.crops.get(crop.getSelectedItemPosition()).id;
            String note = notes.getText().toString().trim();
            double size = hectares;
            Store.of(c).write(s -> s.crops().addField(n, cropId, size, note.isEmpty() ? null : note));
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void confirmDelete(Field f) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + f.name + "?")
                .setMessage("Its scans are kept but no longer tied to a field. Its alerts are removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    Store.of(requireContext()).write(s -> {
                        s.crops().deleteField(f.id);
                        return null;
                    });
                    Toast.makeText(requireContext(), f.name + " deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
