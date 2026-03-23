package com.simats.cdss.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight sparkline chart view for drawing ABG trend lines.
 * Draws a smooth line with gradient fill, data points, optional target range band,
 * time labels below the chart, and a tooltip when a data point is tapped.
 */
public class SparkLineView extends View {

    private List<Float> dataPoints = new ArrayList<>();
    private List<String> timeLabels = new ArrayList<>();
    private List<String> dateLabels = new ArrayList<>(); // Full date labels for tooltip
    private int lineColor = Color.parseColor("#EF4444");
    private int pointColor = Color.parseColor("#EF4444");
    private float targetMin = -1f;
    private float targetMax = -1f;

    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointPaint;
    private Paint pointStrokePaint;
    private Paint targetBandPaint;
    private Paint targetLinePaint;
    private Paint gridPaint;
    private Paint timeLabelPaint;
    private Paint tooltipBgPaint;
    private Paint tooltipTextPaint;
    private Paint noDataPaint;
    private Path linePath;
    private Path fillPath;

    private float padLeft = 8f;
    private float padRight = 16f;
    private float padTop = 24f;
    private float padBottom = 32f; // Extra space for time labels

    // Tooltip state
    private int selectedPointIndex = -1;
    private float[] cachedXPoints;
    private float[] cachedYPoints;

    public SparkLineView(Context context) {
        super(context);
        init();
    }

    public SparkLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SparkLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        padLeft *= density;
        padRight *= density;
        padTop *= density;
        padBottom *= density;

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);

        pointStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointStrokePaint.setStyle(Paint.Style.STROKE);
        pointStrokePaint.setStrokeWidth(2f * density);
        pointStrokePaint.setColor(Color.WHITE);

        targetBandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetBandPaint.setStyle(Paint.Style.FILL);
        targetBandPaint.setColor(Color.parseColor("#10139487"));

        targetLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetLinePaint.setStyle(Paint.Style.STROKE);
        targetLinePaint.setStrokeWidth(1f * density);
        targetLinePaint.setColor(Color.parseColor("#40139487"));
        targetLinePaint.setPathEffect(new DashPathEffect(new float[]{6 * density, 4 * density}, 0));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f * density);
        gridPaint.setColor(Color.parseColor("#15000000"));

        timeLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeLabelPaint.setTextSize(10f * density);
        timeLabelPaint.setColor(Color.parseColor("#94A3B8"));
        timeLabelPaint.setTextAlign(Paint.Align.CENTER);

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setStyle(Paint.Style.FILL);
        tooltipBgPaint.setColor(Color.parseColor("#333333"));

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setTextSize(11f * density);
        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        noDataPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noDataPaint.setTextSize(13f * density);
        noDataPaint.setColor(Color.parseColor("#94A3B8"));
        noDataPaint.setTextAlign(Paint.Align.CENTER);

        linePath = new Path();
        fillPath = new Path();
    }

    public void setData(List<Float> points, int color) {
        this.dataPoints = points != null ? points : new ArrayList<>();
        this.lineColor = color;
        this.pointColor = color;
        linePaint.setColor(color);
        pointPaint.setColor(color);
        selectedPointIndex = -1;
        invalidate();
    }

    public void setData(List<Float> points, int color, List<String> labels) {
        this.dataPoints = points != null ? points : new ArrayList<>();
        this.timeLabels = labels != null ? labels : new ArrayList<>();
        this.lineColor = color;
        this.pointColor = color;
        linePaint.setColor(color);
        pointPaint.setColor(color);
        selectedPointIndex = -1;
        invalidate();
    }

    public void setTimeLabels(List<String> labels) {
        this.timeLabels = labels != null ? labels : new ArrayList<>();
        invalidate();
    }

    public void setDateLabels(List<String> labels) {
        this.dateLabels = labels != null ? labels : new ArrayList<>();
    }

    public void setTargetRange(float min, float max) {
        this.targetMin = min;
        this.targetMax = max;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && cachedXPoints != null) {
            float touchX = event.getX();
            float touchY = event.getY();
            float density = getResources().getDisplayMetrics().density;
            float hitRadius = 24f * density; // Touch target

            int closest = -1;
            float closestDist = Float.MAX_VALUE;
            for (int i = 0; i < cachedXPoints.length; i++) {
                float dist = (float) Math.sqrt(
                    Math.pow(touchX - cachedXPoints[i], 2) +
                    Math.pow(touchY - cachedYPoints[i], 2));
                if (dist < hitRadius && dist < closestDist) {
                    closestDist = dist;
                    closest = i;
                }
            }

            if (closest >= 0) {
                selectedPointIndex = (selectedPointIndex == closest) ? -1 : closest;
                invalidate();
                return true;
            } else {
                if (selectedPointIndex >= 0) {
                    selectedPointIndex = -1;
                    invalidate();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth() - padLeft - padRight;
        float h = getHeight() - padTop - padBottom;
        float density = getResources().getDisplayMetrics().density;

        // Handle no data
        if (dataPoints == null || dataPoints.isEmpty()) {
            canvas.drawText("No data available for this time range",
                    getWidth() / 2f, getHeight() / 2f, noDataPaint);
            cachedXPoints = null;
            return;
        }

        // Calculate min/max with padding
        float dataMin = Float.MAX_VALUE;
        float dataMax = Float.MIN_VALUE;
        for (float val : dataPoints) {
            dataMin = Math.min(dataMin, val);
            dataMax = Math.max(dataMax, val);
        }
        // Include target range in min/max calculation
        if (targetMin >= 0) dataMin = Math.min(dataMin, targetMin);
        if (targetMax >= 0) dataMax = Math.max(dataMax, targetMax);

        float range = dataMax - dataMin;
        if (range == 0) range = 1f;
        // Add 15% padding to range
        float paddedMin = dataMin - range * 0.15f;
        float paddedMax = dataMax + range * 0.15f;
        float paddedRange = paddedMax - paddedMin;

        // Draw horizontal grid lines
        for (int i = 0; i <= 3; i++) {
            float y = padTop + h - (h * i / 3f);
            canvas.drawLine(padLeft, y, padLeft + w, y, gridPaint);
        }

        // Draw target range band
        if (targetMin >= 0 && targetMax >= 0) {
            float yMin = padTop + h - ((targetMin - paddedMin) / paddedRange * h);
            float yMax = padTop + h - ((targetMax - paddedMin) / paddedRange * h);
            canvas.drawRect(padLeft, yMax, padLeft + w, yMin, targetBandPaint);
            canvas.drawLine(padLeft, yMin, padLeft + w, yMin, targetLinePaint);
            canvas.drawLine(padLeft, yMax, padLeft + w, yMax, targetLinePaint);
        }

        // Handle single data point
        if (dataPoints.size() == 1) {
            float cx = padLeft + w / 2f;
            float cy = padTop + h / 2f;
            cachedXPoints = new float[]{cx};
            cachedYPoints = new float[]{cy};

            float outerRadius = 6f * density;
            canvas.drawCircle(cx, cy, outerRadius, pointPaint);
            canvas.drawCircle(cx, cy, outerRadius, pointStrokePaint);

            // Glow effect
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setColor(Color.argb(30, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)));
            canvas.drawCircle(cx, cy, outerRadius * 2.5f, glowPaint);

            // Draw time label
            if (timeLabels != null && !timeLabels.isEmpty() && timeLabels.get(0) != null) {
                float labelY = padTop + h + (18f * density);
                canvas.drawText(timeLabels.get(0), cx, labelY, timeLabelPaint);
            }

            // Draw tooltip for single point
            drawTooltip(canvas, 0, cx, cy, density);
            return;
        }

        // Calculate points (2 or more)
        float[] xPoints = new float[dataPoints.size()];
        float[] yPoints = new float[dataPoints.size()];
        float step = w / (dataPoints.size() - 1);

        for (int i = 0; i < dataPoints.size(); i++) {
            xPoints[i] = padLeft + i * step;
            yPoints[i] = padTop + h - ((dataPoints.get(i) - paddedMin) / paddedRange * h);
        }

        // Cache points for touch detection
        cachedXPoints = xPoints;
        cachedYPoints = yPoints;

        // Draw smooth line path
        linePath.reset();
        fillPath.reset();
        linePath.moveTo(xPoints[0], yPoints[0]);
        fillPath.moveTo(xPoints[0], padTop + h);
        fillPath.lineTo(xPoints[0], yPoints[0]);

        for (int i = 1; i < dataPoints.size(); i++) {
            float cx = (xPoints[i - 1] + xPoints[i]) / 2f;
            linePath.cubicTo(cx, yPoints[i - 1], cx, yPoints[i], xPoints[i], yPoints[i]);
            fillPath.cubicTo(cx, yPoints[i - 1], cx, yPoints[i], xPoints[i], yPoints[i]);
        }

        fillPath.lineTo(xPoints[dataPoints.size() - 1], padTop + h);
        fillPath.close();

        // Draw gradient fill
        int fillColorStart = Color.argb(40, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        int fillColorEnd = Color.argb(5, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        fillPaint.setShader(new LinearGradient(0, padTop, 0, padTop + h,
                fillColorStart, fillColorEnd, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // Draw line
        canvas.drawPath(linePath, linePaint);

        // Draw data points and time labels
        float outerRadius = 5f * density;
        for (int i = 0; i < dataPoints.size(); i++) {
            // Outer colored circle
            canvas.drawCircle(xPoints[i], yPoints[i], outerRadius, pointPaint);
            // White inner ring for the stroke effect
            canvas.drawCircle(xPoints[i], yPoints[i], outerRadius, pointStrokePaint);
            // Last point gets a larger highlight
            if (i == dataPoints.size() - 1) {
                Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                glowPaint.setColor(Color.argb(30, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)));
                canvas.drawCircle(xPoints[i], yPoints[i], outerRadius * 2, glowPaint);
            }

            // Draw time labels below chart
            if (timeLabels != null && i < timeLabels.size() && timeLabels.get(i) != null) {
                float labelY = padTop + h + (18f * density);
                canvas.drawText(timeLabels.get(i), xPoints[i], labelY, timeLabelPaint);
            }
        }

        // Draw tooltip for selected point
        if (selectedPointIndex >= 0 && selectedPointIndex < dataPoints.size()) {
            drawTooltip(canvas, selectedPointIndex, xPoints[selectedPointIndex], yPoints[selectedPointIndex], density);
        }
    }

    /**
     * Draws a tooltip bubble above the selected data point showing the value and date.
     */
    private void drawTooltip(Canvas canvas, int index, float px, float py, float density) {
        // Build tooltip text
        StringBuilder tooltipText = new StringBuilder();
        if (index < dataPoints.size()) {
            tooltipText.append(String.format("%.1f", dataPoints.get(index)));
        }

        // Add date if available
        if (dateLabels != null && index < dateLabels.size() && dateLabels.get(index) != null) {
            if (tooltipText.length() > 0) tooltipText.append("  ");
            tooltipText.append(dateLabels.get(index));
        } else if (timeLabels != null && index < timeLabels.size() && timeLabels.get(index) != null) {
            if (tooltipText.length() > 0) tooltipText.append("  ");
            tooltipText.append(timeLabels.get(index));
        }

        String text = tooltipText.toString();
        if (text.isEmpty()) return;

        float textWidth = tooltipTextPaint.measureText(text);
        float tooltipW = textWidth + 20f * density;
        float tooltipH = 28f * density;
        float tooltipY = py - 16f * density - tooltipH;
        float tooltipX = px - tooltipW / 2f;

        // Keep tooltip within bounds
        if (tooltipX < 4) tooltipX = 4;
        if (tooltipX + tooltipW > getWidth() - 4) tooltipX = getWidth() - 4 - tooltipW;
        if (tooltipY < 4) tooltipY = py + 16f * density; // Show below if no space above

        RectF tooltipRect = new RectF(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + tooltipH);
        canvas.drawRoundRect(tooltipRect, 6f * density, 6f * density, tooltipBgPaint);

        // Draw tooltip text
        float textY = tooltipY + tooltipH / 2f + tooltipTextPaint.getTextSize() / 3f;
        canvas.drawText(text, tooltipX + tooltipW / 2f, textY, tooltipTextPaint);

        // Draw selected point highlight ring
        Paint selPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selPaint.setStyle(Paint.Style.STROKE);
        selPaint.setStrokeWidth(3f * density);
        selPaint.setColor(lineColor);
        canvas.drawCircle(px, py, 8f * density, selPaint);
    }
}
