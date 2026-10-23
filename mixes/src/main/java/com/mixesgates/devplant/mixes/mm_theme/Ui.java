package com.mixesgates.devplant.mixes.mm_theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Severity;
import com.mixesgates.devplant.mixes.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Colours, text styles and the small building blocks every screen is made from. */
public final class Ui {

    /** Bottom padding for pager pages so content can scroll clear of the bottom menu. */
    public static final int ISLAND_CLEARANCE_DP = 160;

    public enum Type { TITLE, HEADING, BODY, CAPTION, NUMBER, LABEL }

    private Ui() {
    }

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static int color(Context c, int colorRes) {
        return ContextCompat.getColor(c, colorRes);
    }

    public static TextView text(Context c, CharSequence s, Type type) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextColor(color(c, R.color.mm_ink));
        switch (type) {
            case TITLE:
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                v.setTypeface(Typeface.DEFAULT_BOLD);
                break;
            case HEADING:
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                v.setTypeface(Typeface.DEFAULT_BOLD);
                break;
            case BODY:
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                v.setLineSpacing(0, 1.15f);
                break;
            case CAPTION:
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                v.setTextColor(color(c, R.color.mm_ink_soft));
                break;
            case NUMBER:
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                v.setTypeface(Typeface.DEFAULT_BOLD);
                break;
            case LABEL:
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                v.setTypeface(Typeface.DEFAULT_BOLD);
                v.setAllCaps(true);
                v.setLetterSpacing(0.08f);
                v.setTextColor(color(c, R.color.mm_ink_soft));
                break;
        }
        return v;
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    /** A rounded surface with a hairline border; add children to it. */
    public static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(c, 16);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = rounded(color(c, R.color.mm_surface), dp(c, 18));
        bg.setStroke(dp(c, 1), color(c, R.color.mm_line));
        card.setBackground(bg);
        card.setLayoutParams(below(c, 12));
        return card;
    }

    /** Full width, wrap height, with a top margin. */
    public static LinearLayout.LayoutParams below(Context c, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        return lp;
    }

    public static <V extends View> V gap(V view, int topMarginDp) {
        view.setLayoutParams(below(view.getContext(), topMarginDp));
        return view;
    }

    public static TextView pill(Context c, String text, int colorRes) {
        TextView v = new TextView(c);
        v.setText(text);
        int fg = color(c, colorRes);
        v.setTextColor(fg);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        v.setBackground(rounded(Color.argb(40, Color.red(fg), Color.green(fg), Color.blue(fg)), dp(c, 99)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(c, 6));
        v.setLayoutParams(lp);
        return v;
    }

    public static LinearLayout row(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    public static LinearLayout column(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        return col;
    }

    public static LinearLayout.LayoutParams weight(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    public static MaterialButton button(Context c, String text, boolean primary) {
        MaterialButton b = primary
                ? new MaterialButton(c)
                : new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    public static ImageView icon(Context c, int drawableRes, int tint, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(drawableRes);
        v.setImageTintList(ColorStateList.valueOf(tint));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return v;
    }

    public static ImageButton iconButton(Context c, int drawableRes, String description, View.OnClickListener click) {
        ImageButton b = new ImageButton(c);
        b.setImageResource(drawableRes);
        b.setImageTintList(ColorStateList.valueOf(color(c, R.color.mm_ink)));
        b.setContentDescription(description);
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        b.setBackgroundResource(tv.resourceId);
        b.setOnClickListener(click);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 44), dp(c, 44)));
        return b;
    }

    /** Makes a view respond to taps with the standard ripple. */
    public static <V extends View> V tappable(V view, View.OnClickListener click) {
        TypedValue tv = new TypedValue();
        view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        view.setForeground(ContextCompat.getDrawable(view.getContext(), tv.resourceId));
        view.setOnClickListener(click);
        return view;
    }

    /** A scrolling page: add content to {@link Page#body}. */
    public static final class Page {
        public final ScrollView root;
        public final LinearLayout body;

        Page(ScrollView root, LinearLayout body) {
            this.root = root;
            this.body = body;
        }
    }

    public static Page page(Context c, boolean underIsland) {
        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout body = column(c);
        int side = dp(c, 16);
        body.setPadding(side, dp(c, 8), side, dp(c, underIsland ? ISLAND_CLEARANCE_DP : 32));
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new Page(scroll, body);
    }

    /** Root for a screen opened over the pager: opaque and swallowing touches. */
    public static LinearLayout screen(Context c) {
        LinearLayout root = column(c);
        root.setBackgroundColor(color(c, R.color.mm_bg));
        root.setClickable(true);
        root.setFocusable(true);
        return root;
    }

    public static LinearLayout topBar(Context c, String title, Runnable onBack) {
        LinearLayout bar = row(c);
        bar.setPadding(dp(c, 4), dp(c, 4), dp(c, 12), dp(c, 4));
        bar.addView(iconButton(c, R.drawable.mm_ic_back, "Back", v -> onBack.run()));
        TextView t = text(c, title, Type.HEADING);
        t.setPadding(dp(c, 4), 0, 0, 0);
        bar.addView(t, weight(1));
        return bar;
    }

    public static View empty(Context c, String title, String hint) {
        LinearLayout box = column(c);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(c, 24), dp(c, 48), dp(c, 24), dp(c, 24));
        box.addView(icon(c, R.drawable.mm_logo, color(c, R.color.mm_line), 56));
        TextView t = text(c, title, Type.HEADING);
        t.setGravity(Gravity.CENTER);
        box.addView(gap(t, 12));
        TextView h = text(c, hint, Type.CAPTION);
        h.setGravity(Gravity.CENTER);
        box.addView(gap(h, 6));
        return box;
    }

    // ---- words and colours for the plain shapes ----

    public static int kindColor(Kind kind) {
        switch (kind) {
            case HEALTHY: return R.color.mm_healthy;
            case DISEASE: return R.color.mm_disease;
            case PEST: return R.color.mm_pest;
            default: return R.color.mm_unsure;
        }
    }

    public static String kindLabel(Kind kind) {
        switch (kind) {
            case HEALTHY: return "Healthy";
            case DISEASE: return "Disease";
            case PEST: return "Pest";
            default: return "Unsure";
        }
    }

    public static int severityColor(Severity severity) {
        switch (severity) {
            case SEVERE: return R.color.mm_severe;
            case MODERATE: return R.color.mm_moderate;
            case MILD: return R.color.mm_mild;
            default: return R.color.mm_healthy;
        }
    }

    public static String severityLabel(Severity severity) {
        switch (severity) {
            case SEVERE: return "Severe";
            case MODERATE: return "Moderate";
            case MILD: return "Mild";
            default: return "None";
        }
    }

    public static String percent(float fraction) {
        return Math.round(fraction * 100) + "%";
    }

    public static String time(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    public static String dateTime(long millis) {
        return new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    public static String day(long millis) {
        if (DateUtils.isToday(millis)) return "Today";
        if (DateUtils.isToday(millis + DateUtils.DAY_IN_MILLIS)) return "Yesterday";
        return new SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    public static String ago(long millis) {
        return DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString();
    }
}
