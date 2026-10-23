package com.mixesgates.devplant.mixes.mm_shell;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

/** The floating pill at the bottom that switches between the main screens. */
final class Island extends LinearLayout {

    interface Listener {
        void onPick(int index);
    }

    private final LinearLayout[] items;
    private final int on;
    private final int off;

    Island(Context c, String[] labels, int[] icons, Listener listener) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        int pad = Ui.dp(c, 6);
        setPadding(pad, pad, pad, pad);
        setBackground(Ui.rounded(Ui.color(c, R.color.mm_island), Ui.dp(c, 32)));
        setElevation(Ui.dp(c, 8));

        on = Color.WHITE;
        off = Color.argb(170, 255, 255, 255);
        items = new LinearLayout[labels.length];
        for (int i = 0; i < labels.length; i++) {
            LinearLayout item = Ui.column(c);
            item.setGravity(Gravity.CENTER);
            item.setPadding(Ui.dp(c, 10), Ui.dp(c, 6), Ui.dp(c, 10), Ui.dp(c, 6));
            item.setContentDescription(labels[i]);
            item.addView(Ui.icon(c, icons[i], off, 22));
            TextView label = new TextView(c);
            label.setText(labels[i]);
            label.setTextSize(11);
            label.setTextColor(off);
            item.addView(label);
            int index = i;
            item.setOnClickListener(v -> listener.onPick(index));
            addView(item, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            items[i] = item;
        }
    }

    void select(int index) {
        for (int i = 0; i < items.length; i++) {
            boolean selected = i == index;
            LinearLayout item = items[i];
            item.setBackground(selected
                    ? Ui.rounded(Ui.color(getContext(), R.color.mm_island_active), Ui.dp(getContext(), 26))
                    : null);
            int tint = selected ? on : off;
            ((ImageView) item.getChildAt(0)).setImageTintList(ColorStateList.valueOf(tint));
            ((TextView) item.getChildAt(1)).setTextColor(tint);
            item.setSelected(selected);
        }
    }
}
