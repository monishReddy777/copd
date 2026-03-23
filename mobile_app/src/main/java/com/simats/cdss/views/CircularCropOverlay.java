package com.simats.cdss.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

/**
 * Draws a semi-transparent dark overlay with a transparent circular hole
 * in the center, indicating the crop area for profile images.
 */
public class CircularCropOverlay extends View {

    private Paint overlayPaint;
    private Paint circlePaint;
    private int circleRadius = 0;

    public CircularCropOverlay(Context context) {
        super(context);
        init();
    }

    public CircularCropOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularCropOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Enable hardware layer for PorterDuff to work properly
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#AA000000")); // Semi-transparent dark

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public int getCircleRadius() {
        return circleRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        circleRadius = (int) (Math.min(w, h) * 0.42f); // 84% of smallest dimension

        // Draw full dark overlay
        canvas.drawRect(0, 0, w, h, overlayPaint);

        // Clear a circle in the center for the crop area
        canvas.drawCircle(cx, cy, circleRadius, circlePaint);

        // Draw a thin white border around the circle
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
        borderPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, circleRadius, borderPaint);
    }
}
