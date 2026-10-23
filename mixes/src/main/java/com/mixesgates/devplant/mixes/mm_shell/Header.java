package com.mixesgates.devplant.mixes.mm_shell;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

/** Logo, title, and the search, alerts and settings buttons. */
final class Header extends LinearLayout {

    private final TextView subtitle;
    private final TextView badge;

    Header(Context c, Nav nav) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(Ui.dp(c, 16), Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8));

        addView(Ui.icon(c, R.drawable.mm_logo, Ui.color(c, R.color.mm_leaf), 30));
        LinearLayout titles = Ui.column(c);
        titles.setPadding(Ui.dp(c, 10), 0, 0, 0);
        titles.addView(Ui.text(c, "DevPlant", Ui.Type.HEADING));
        subtitle = Ui.text(c, "", Ui.Type.CAPTION);
        titles.addView(subtitle);
        addView(titles, Ui.weight(1));

        addView(Ui.iconButton(c, R.drawable.mm_ic_search, "Search", v -> nav.openSearch()));

        FrameLayout bell = new FrameLayout(c);
        ImageButton bellButton = Ui.iconButton(c, R.drawable.mm_ic_bell, "Alerts", v -> nav.openAlerts());
        bell.addView(bellButton, new FrameLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44)));
        badge = new TextView(c);
        badge.setTextSize(10);
        badge.setTextColor(Ui.color(c, R.color.mm_surface));
        badge.setGravity(Gravity.CENTER);
        badge.setMinWidth(Ui.dp(c, 16));
        badge.setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0);
        badge.setBackground(Ui.rounded(Ui.color(c, R.color.mm_severe), Ui.dp(c, 8)));
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, Ui.dp(c, 16), Gravity.TOP | Gravity.END);
        badgeLp.topMargin = Ui.dp(c, 6);
        badgeLp.rightMargin = Ui.dp(c, 4);
        bell.addView(badge, badgeLp);
        badge.setVisibility(View.GONE);
        addView(bell);

        addView(Ui.iconButton(c, R.drawable.mm_ic_settings, "Settings", v -> nav.openSettings()));
    }

    void setSubtitle(String text) {
        subtitle.setText(text);
    }

    void setUnread(int count) {
        badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
    }
}
