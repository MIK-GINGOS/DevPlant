package com.mixesgates.devplant.mixes.mm_shell;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The control-centre menu at the bottom: three tiles plus More, and More opens a grid of eight.
 * The More circle glides to the close button's corner while spinning, then the close button
 * spins in there; grid tiles fan out from that corner and fold back into it.
 */
final class ControlMenu extends FrameLayout {

    /** One button. A tile with a tab lights up while that page shows; one with no action is not built yet. */
    static final class Tile {
        final String label;
        final int icon;
        final int tab;
        final Runnable action;

        private Tile(String label, int icon, int tab, Runnable action) {
            this.label = label;
            this.icon = icon;
            this.tab = tab;
            this.action = action;
        }

        static Tile page(String label, int icon, int tab, Runnable action) {
            return new Tile(label, icon, tab, action);
        }

        static Tile action(String label, int icon, Runnable action) {
            return new Tile(label, icon, -1, action);
        }

        static Tile planned(String label, int icon) {
            return new Tile(label, icon, -1, null);
        }
    }

    interface ExpandListener {
        void onExpandedChanged(boolean expanded);
    }

    private static final int MAX_WIDTH_DP = 380;
    private static final int TILE_DP = 52;
    private static final int ICON_DP = 20;
    private static final int CLOSE_DP = 28;

    private static final Interpolator EASE_IN = new PathInterpolator(0.4f, 0f, 1f, 1f);
    private static final Interpolator GLIDE = new PathInterpolator(0.32f, 0.72f, 0f, 1f);
    private static final Interpolator SPIN = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
    private static final Interpolator SPRING = new PathInterpolator(0.34f, 1.45f, 0.64f, 1f);

    private final int accent;
    private final int onAccent;
    private final int idleBg;
    private final int idleIcon;
    private final int labelColor;
    private final ImageView backdrop;
    private final Runnable recapture = this::frost;
    private final ViewTreeObserver.OnScrollChangedListener onScroll = this::frostSoon;
    private final ViewTreeObserver.OnGlobalLayoutListener onLayout = this::frostSoon;
    private Bitmap shot;
    private View host;
    private boolean frostQueued;
    private final FrameLayout inner;
    private final FrameLayout body;
    private final FrameLayout close;
    private final LinearLayout mainGrid;
    private final LinearLayout fullGrid;
    private final List<Cell> mainCells = new ArrayList<>();
    private final List<Cell> gridCells = new ArrayList<>();
    private final List<Animator> running = new ArrayList<>();
    private final Runnable pending = this::finishPhase;

    private final Cell more;
    private ExpandListener expandListener;
    private boolean expanded;
    private boolean animating;
    private int currentTab = -1;

    ControlMenu(Context c, Tile[] mainTiles, Tile[] gridTiles) {
        super(c);
        accent = Ui.color(c, R.color.mm_glass_accent);
        onAccent = Ui.color(c, R.color.mm_glass_on_accent);
        idleBg = isDark() ? 0x26FFFFFF : 0x14000000;
        idleIcon = withAlpha(accent, 0.7f);
        labelColor = withAlpha(accent, 0.8f);

        setClipChildren(false);
        setElevation(Ui.dp(c, 8));
        float radius = Ui.dp(c, 36);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        setClipToOutline(true);

        // Frosted glass: a blurred picture of the page under the card, then a faint tint and hairline.
        backdrop = new ImageView(c);
        backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(backdrop, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        GradientDrawable tint = Ui.rounded(Ui.color(c, R.color.mm_glass_bg), radius);
        tint.setStroke(Math.max(1, Ui.dp(c, 1)), Ui.color(c, R.color.mm_glass_border));

        inner = new FrameLayout(c);
        inner.setBackground(tint);
        int pad = Ui.dp(c, 24);
        inner.setPadding(pad, Ui.dp(c, 20), pad, Ui.dp(c, 20));
        inner.setClipChildren(false);
        addView(inner, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        body = new FrameLayout(c);
        body.setClipChildren(false);
        inner.addView(body, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        Tile[] withMore = new Tile[mainTiles.length + 1];
        System.arraycopy(mainTiles, 0, withMore, 0, mainTiles.length);
        withMore[mainTiles.length] = Tile.action("More", R.drawable.mm_ic_cc_more, this::expand);
        mainGrid = grid(withMore, mainCells, 0);
        more = mainCells.get(mainCells.size() - 1);
        // Room above the grid so the close button never sits on the top-right tile.
        fullGrid = grid(gridTiles, gridCells, Ui.dp(c, 22));
        body.addView(mainGrid);

        close = new FrameLayout(c);
        close.setBackground(Ui.rounded(idleBg, Ui.dp(c, CLOSE_DP / 2f)));
        close.setContentDescription("Close menu");
        ImageView x = new ImageView(c);
        x.setImageResource(R.drawable.mm_ic_cc_close_more);
        x.setImageTintList(ColorStateList.valueOf(accent));
        close.addView(x, new LayoutParams(Ui.dp(c, 14), Ui.dp(c, 14), Gravity.CENTER));
        close.setOnClickListener(v -> collapse());
        pressable(close);
        LayoutParams closeLp = new LayoutParams(Ui.dp(c, CLOSE_DP), Ui.dp(c, CLOSE_DP), Gravity.TOP | Gravity.END);
        closeLp.topMargin = Ui.dp(c, 12);
        closeLp.rightMargin = Ui.dp(c, 12);
        addView(close, closeLp);
        close.setVisibility(GONE);
    }

    /** The view drawn, blurred, behind the card. It must not contain the card itself. */
    void setBackdropSource(View source) {
        host = source;
        frostSoon();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(onScroll);
        getViewTreeObserver().addOnGlobalLayoutListener(onLayout);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        frostSoon();
    }

    /** Re-captures the page behind the card, at most every 50ms while it scrolls or changes. */
    void frostSoon() {
        if (frostQueued) return;
        frostQueued = true;
        postDelayed(recapture, 50);
    }

    /**
     * Captures the part of the page under the card, saturated and blurred by 23 like crystal_io's
     * glass menu. Before Android 12 there is no blur, so a near-solid tint keeps the tiles readable.
     */
    private void frost() {
        frostQueued = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || host == null
                || getWidth() <= 0 || getHeight() <= 0) {
            backdrop.setImageDrawable(null);
            backdrop.setBackgroundColor(isDark() ? 0xE6101116 : 0xF2F5F6F8);
            return;
        }
        int[] me = new int[2];
        int[] page = new int[2];
        getLocationInWindow(me);
        host.getLocationInWindow(page);
        float scale = 0.5f; // the blur hides the detail, so half size is plenty
        int w = Math.max(1, Math.round(getWidth() * scale));
        int h = Math.max(1, Math.round(getHeight() * scale));
        if (shot == null || shot.getWidth() != w || shot.getHeight() != h) {
            shot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        shot.eraseColor(Ui.color(getContext(), R.color.mm_bg));
        Canvas canvas = new Canvas(shot);
        canvas.scale(scale, scale);
        canvas.translate(page[0] - me[0], page[1] - me[1]);
        try {
            host.draw(canvas);
        } catch (RuntimeException e) {
            return;
        }
        backdrop.setBackground(null);
        backdrop.setImageBitmap(shot);
        backdrop.invalidate();
        ColorMatrix saturation = new ColorMatrix();
        saturation.setSaturation(1.4f);
        float sigma = Ui.dp(getContext(), 23);
        backdrop.setRenderEffect(RenderEffect.createChainEffect(
                RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.CLAMP),
                RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(saturation))));
    }

    private boolean isDark() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(alpha * 255), Color.red(color), Color.green(color), Color.blue(color));
    }

    void setExpandListener(ExpandListener listener) {
        expandListener = listener;
    }

    boolean isExpanded() {
        return expanded;
    }

    /** Lights the tile for the page now showing. */
    void select(int tab) {
        currentTab = tab;
        for (Cell cell : mainCells) cell.setActive(cell.tile.tab >= 0 && cell.tile.tab == tab, 300);
        for (Cell cell : gridCells) cell.setActive(cell.tile.tab >= 0 && cell.tile.tab == tab, 200);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int max = Ui.dp(getContext(), MAX_WIDTH_DP);
        if (MeasureSpec.getSize(widthMeasureSpec) > max) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(max, MeasureSpec.getMode(widthMeasureSpec));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // The card is as tall as its tiles; the blurred picture only fills it.
        int w = getMeasuredWidth();
        int h = inner.getMeasuredHeight();
        backdrop.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(pending);
        removeCallbacks(recapture);
        frostQueued = false;
        getViewTreeObserver().removeOnScrollChangedListener(onScroll);
        getViewTreeObserver().removeOnGlobalLayoutListener(onLayout);
        stopRunning();
        super.onDetachedFromWindow();
    }

    // --- building -------------------------------------------------------------------------

    private LinearLayout grid(Tile[] tiles, List<Cell> cells, int topPad) {
        Context c = getContext();
        LinearLayout grid = Ui.column(c);
        grid.setClipChildren(false);
        grid.setPadding(0, topPad, 0, 0);
        LinearLayout row = null;
        for (int i = 0; i < tiles.length; i++) {
            if (i % 4 == 0) {
                row = Ui.row(c);
                row.setClipChildren(false);
                grid.addView(row, i == 0 ? new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT) : Ui.below(c, 20));
            }
            Cell cell = new Cell(c, tiles[i]);
            LinearLayout.LayoutParams lp = Ui.weight(1);
            if (i % 4 != 0) lp.leftMargin = Ui.dp(c, 12);
            row.addView(cell.root, lp);
            cells.add(cell);
        }
        // Keep partial rows in four columns.
        int rest = (4 - tiles.length % 4) % 4;
        for (int i = 0; i < rest && row != null; i++) {
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.leftMargin = Ui.dp(c, 12);
            row.addView(new View(c), lp);
        }
        return grid;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void pressable(View v) {
        v.setOnTouchListener((view, e) -> {
            if (animating) return false;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(150).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            }
            return false;
        });
    }

    private void onTap(Cell cell) {
        if (animating) return;
        Tile tile = cell.tile;
        if (tile.action == null) {
            Toast.makeText(getContext(), tile.label + " is coming soon", Toast.LENGTH_SHORT).show();
            return;
        }
        tile.action.run();
        if (expanded) collapse();
    }

    // --- motion ---------------------------------------------------------------------------

    private void expand() {
        if (animating || expanded) return;
        animating = true;
        stopRunning();
        Context c = getContext();
        Pose out = new Pose(0f, 0, Ui.dp(c, 6), 0.8f, 0, Ui.dp(c, 6));
        for (int i = 0; i < mainCells.size() - 1; i++) {
            Cell cell = mainCells.get(i);
            run(cell.root, Pose.REST, out, 260, (2 - i) * 40L, EASE_IN, false);
        }
        run(more.circle, Pose.REST, cornerPose(0.9f), 450, 0, GLIDE, false);
        ValueAnimator spin = ValueAnimator.ofFloat(0f, 1f);
        spin.setDuration(450);
        spin.addUpdateListener(a -> {
            float t = a.getAnimatedFraction();
            more.icon.setRotation(270f * SPIN.getInterpolation(t));
            more.icon.setAlpha(t < 0.7f ? 1f : 1f - SPIN.getInterpolation((t - 0.7f) / 0.3f));
        });
        start(spin);
        more.label.animate().alpha(0f).setDuration(200).start();
        postPhase(450);
    }

    void collapse() {
        if (animating || !expanded) return;
        animating = true;
        stopRunning();
        if (expandListener != null) expandListener.onExpandedChanged(false);
        for (int i = 0; i < gridCells.size(); i++) {
            Cell cell = gridCells.get(i);
            run(cell.root, Pose.REST, fanPose(i), 240, (gridCells.size() - 1 - i) * 20L, EASE_IN, false);
        }
        run(close, Pose.REST, closedX(), 220, 0, EASE_IN, false);
        postPhase(400);
    }

    private void postPhase(long ms) {
        removeCallbacks(pending);
        postDelayed(pending, ValueAnimator.areAnimatorsEnabled() ? ms : 0);
    }

    /** Swaps the grids once the outgoing tiles are gone, and plays the incoming ones. */
    private void finishPhase() {
        stopRunning();
        Context c = getContext();
        if (!expanded) {
            swapTo(fullGrid);
            expanded = true;
            for (int i = 0; i < gridCells.size(); i++) {
                int col = i % 4;
                int row = i / 4;
                run(gridCells.get(i).root, fanPose(i), Pose.REST, 520,
                        60L + (3 - col) * 35L + row * 60L, SPRING, true);
            }
            close.setVisibility(VISIBLE);
            run(close, closedX(), Pose.REST, 420, 0, SPRING, true);
            if (expandListener != null) expandListener.onExpandedChanged(true);
        } else {
            close.setVisibility(GONE);
            swapTo(mainGrid);
            expanded = false;
            Pose out = new Pose(0f, 0, Ui.dp(c, 6), 0.8f, 0, Ui.dp(c, 6));
            for (int i = 0; i < mainCells.size() - 1; i++) {
                run(mainCells.get(i).root, out, Pose.REST, 460, 120L + (2 - i) * 45L, SPRING, true);
            }
            more.label.setAlpha(1f);
            run(more.circle, cornerPose(0f), Pose.REST, 500, 0, GLIDE, true);
            run(more.icon, new Pose(0f, 0, 0, 1f, 270f, 0), Pose.REST, 500, 0, GLIDE, true);
        }
        animating = false;
    }

    /** Eases the card between one and two rows instead of snapping. */
    private void swapTo(View next) {
        int from = body.getHeight();
        body.removeAllViews();
        body.addView(next);
        next.measure(MeasureSpec.makeMeasureSpec(body.getWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int to = next.getMeasuredHeight();
        ViewGroup.LayoutParams lp = body.getLayoutParams();
        lp.height = from;
        body.setLayoutParams(lp);
        ValueAnimator height = ValueAnimator.ofInt(from, to);
        height.setDuration(500);
        height.setInterpolator(GLIDE);
        height.addUpdateListener(a -> {
            lp.height = (int) a.getAnimatedValue();
            body.setLayoutParams(lp);
        });
        height.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                lp.height = LayoutParams.WRAP_CONTENT;
                body.setLayoutParams(lp);
            }
        });
        start(height);
    }

    /** Where grid tile {@code index} folds to: toward the top-right corner. */
    private Pose fanPose(int index) {
        Context c = getContext();
        int col = index % 4;
        int row = index / 4;
        return new Pose(0f, Ui.dp(c, (3 - col) * 20), Ui.dp(c, -row * 24 - 8), 0.6f, 0, Ui.dp(c, 6));
    }

    private Pose closedX() {
        return new Pose(0f, 0, 0, 0.4f, -90f, 0);
    }

    /** The More circle shrunk onto the close button's spot. */
    private Pose cornerPose(float alpha) {
        Context c = getContext();
        float[] at = centreInCard(more.circle);
        float cornerX = getWidth() - Ui.dp(c, 12) - Ui.dp(c, CLOSE_DP / 2f);
        float cornerY = Ui.dp(c, 12) + Ui.dp(c, CLOSE_DP / 2f);
        return new Pose(alpha, cornerX - at[0], cornerY - at[1], (float) CLOSE_DP / TILE_DP, 0, 0);
    }

    private float[] centreInCard(View v) {
        float x = v.getWidth() / 2f;
        float y = v.getHeight() / 2f;
        View cur = v;
        while (cur != this && cur != null) {
            x += cur.getLeft();
            y += cur.getTop();
            cur = (View) cur.getParent();
        }
        return new float[]{x, y};
    }

    private void run(View v, Pose from, Pose to, long duration, long delay, Interpolator curve,
                     boolean holdFrom) {
        if (holdFrom) from.applyTo(v, 0f, to);
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(duration);
        a.setStartDelay(delay);
        a.setInterpolator(curve);
        a.addUpdateListener(an -> from.applyTo(v, (float) an.getAnimatedValue(), to));
        start(a);
    }

    private void start(Animator a) {
        running.add(a);
        a.start();
    }

    private void stopRunning() {
        for (Animator a : new ArrayList<>(running)) a.cancel();
        running.clear();
    }

    /** Opacity, offset, scale, spin and blur of a view at one end of a motion. */
    private static final class Pose {
        static final Pose REST = new Pose(1f, 0, 0, 1f, 0, 0);

        final float alpha;
        final float x;
        final float y;
        final float scale;
        final float rotation;
        final float blur;

        Pose(float alpha, float x, float y, float scale, float rotation, float blur) {
            this.alpha = alpha;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotation = rotation;
            this.blur = blur;
        }

        void applyTo(View v, float t, Pose to) {
            v.setAlpha(Math.max(0f, Math.min(1f, mix(alpha, to.alpha, t))));
            v.setTranslationX(mix(x, to.x, t));
            v.setTranslationY(mix(y, to.y, t));
            float s = mix(scale, to.scale, t);
            v.setScaleX(s);
            v.setScaleY(s);
            v.setRotation(mix(rotation, to.rotation, t));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                float b = mix(blur, to.blur, t);
                v.setRenderEffect(b > 0.5f
                        ? RenderEffect.createBlurEffect(b, b, Shader.TileMode.DECAL) : null);
            }
        }

        private static float mix(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    /** One tile: a round icon with its icon, and a label underneath. */
    private final class Cell {
        final Tile tile;
        final LinearLayout root;
        final FrameLayout circle;
        final ImageView icon;
        final TextView label;
        final GradientDrawable disc;
        private boolean active;
        private ValueAnimator fade;

        Cell(Context c, Tile tile) {
            this.tile = tile;
            root = Ui.column(c);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setClipChildren(false);
            root.setContentDescription(tile.label);

            circle = new FrameLayout(c);
            disc = new GradientDrawable();
            disc.setShape(GradientDrawable.OVAL);
            disc.setColor(idleBg);
            circle.setBackground(disc);
            // the lit tile glows in its own colour
            circle.setOutlineAmbientShadowColor(accent);
            circle.setOutlineSpotShadowColor(accent);
            icon = new ImageView(c);
            icon.setImageResource(tile.icon);
            icon.setImageTintList(ColorStateList.valueOf(idleIcon));
            circle.addView(icon, new LayoutParams(Ui.dp(c, ICON_DP), Ui.dp(c, ICON_DP), Gravity.CENTER));
            root.addView(circle, new LinearLayout.LayoutParams(Ui.dp(c, TILE_DP), Ui.dp(c, TILE_DP)));

            label = new TextView(c);
            label.setText(tile.label);
            label.setTextSize(11);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            label.setTextColor(labelColor);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setMaxWidth(Ui.dp(c, 70));
            label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = Ui.dp(c, 6);
            root.addView(label, labelLp);

            root.setOnClickListener(v -> onTap(this));
            pressable(root);
        }

        void setActive(boolean on, long duration) {
            if (on == active) return;
            active = on;
            root.setSelected(on);
            circle.setElevation(on ? Ui.dp(getContext(), 6) : 0);
            int bgFrom = on ? idleBg : accent;
            int bgTo = on ? accent : idleBg;
            int iconFrom = on ? idleIcon : onAccent;
            int iconTo = on ? onAccent : idleIcon;
            if (fade != null) fade.cancel();
            ArgbEvaluator argb = new ArgbEvaluator();
            fade = ValueAnimator.ofFloat(0f, 1f);
            fade.setDuration(isLaidOut() ? duration : 0);
            fade.addUpdateListener(a -> {
                float t = a.getAnimatedFraction();
                disc.setColor((int) argb.evaluate(t, bgFrom, bgTo));
                icon.setImageTintList(ColorStateList.valueOf((int) argb.evaluate(t, iconFrom, iconTo)));
            });
            fade.start();
        }
    }
}
