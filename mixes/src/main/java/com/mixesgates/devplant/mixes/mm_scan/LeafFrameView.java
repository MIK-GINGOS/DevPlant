package com.mixesgates.devplant.mixes.mm_scan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import com.mixesgates.devplant.mixes.mm_theme.Ui;

/**
 * Dims the camera preview outside a centred square and draws corner brackets, so the farmer
 * frames one leaf. The square matches the centre crop LeafPictures makes for the model.
 */
final class LeafFrameView extends View {

    private final Paint scrim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corners = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF frame = new RectF();

    LeafFrameView(Context c) {
        super(c);
        scrim.setColor(Color.argb(120, 0, 0, 0));
        corners.setColor(Color.WHITE);
        corners.setStyle(Paint.Style.STROKE);
        corners.setStrokeWidth(Ui.dp(c, 4));
        corners.setStrokeCap(Paint.Cap.ROUND);
        hint.setColor(Color.WHITE);
        hint.setTextSize(Ui.dp(c, 15));
        hint.setTextAlign(Paint.Align.CENTER);
        hint.setShadowLayer(Ui.dp(c, 3), 0, 0, Color.BLACK);
        setContentDescription("Fit one leaf inside the frame");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float side = Math.min(w, h) * 0.82f;
        float left = (w - side) / 2f, top = (h - side) / 2f;
        frame.set(left, top, left + side, top + side);
        float r = Ui.dp(getContext(), 24);

        path.reset();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.addRect(0, 0, w, h, Path.Direction.CW);
        path.addRoundRect(frame, r, r, Path.Direction.CW);
        canvas.drawPath(path, scrim);

        float len = side * 0.14f;
        float l = frame.left, t = frame.top, rr = frame.right, b = frame.bottom;
        canvas.drawLine(l, t + len, l, t + r, corners);
        canvas.drawLine(l + r, t, l + len, t, corners);
        canvas.drawLine(rr - len, t, rr - r, t, corners);
        canvas.drawLine(rr, t + r, rr, t + len, corners);
        canvas.drawLine(l, b - len, l, b - r, corners);
        canvas.drawLine(l + r, b, l + len, b, corners);
        canvas.drawLine(rr - len, b, rr - r, b, corners);
        canvas.drawLine(rr, b - r, rr, b - len, corners);
        canvas.drawArc(l, t, l + 2 * r, t + 2 * r, 180, 90, false, corners);
        canvas.drawArc(rr - 2 * r, t, rr, t + 2 * r, 270, 90, false, corners);
        canvas.drawArc(l, b - 2 * r, l + 2 * r, b, 90, 90, false, corners);
        canvas.drawArc(rr - 2 * r, b - 2 * r, rr, b, 0, 90, false, corners);

        canvas.drawText("Fit one leaf inside the frame", w / 2f, top - Ui.dp(getContext(), 16), hint);
    }
}
