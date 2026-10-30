
package com.example.ecovisionai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetectionOverlay extends View {

    private Bitmap bitmap;
    private List<YoloDetector.Detection> detections =
            new ArrayList<>();

    private int imageWidth = 1;
    private int imageHeight = 1;

    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DetectionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public DetectionOverlay(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setFakeBoldText(true);

        backgroundPaint.setColor(Color.rgb(20, 120, 50));
        backgroundPaint.setStyle(Paint.Style.FILL);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void showImage(
            Bitmap bitmap,
            List<YoloDetector.Detection> detections
    ) {
        this.bitmap = bitmap;
        this.imageWidth = bitmap.getWidth();
        this.imageHeight = bitmap.getHeight();
        this.detections = detections;
        invalidate();
    }

    public void showCameraDetections(
            int width,
            int height,
            List<YoloDetector.Detection> detections
    ) {
        this.bitmap = null;
        this.imageWidth = width;
        this.imageHeight = height;
        this.detections = detections;
        invalidate();
    }

    public void clear() {
        bitmap = null;
        detections.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        float scale = Math.min(
                (float) getWidth() / imageWidth,
                (float) getHeight() / imageHeight
        );

        float displayWidth = imageWidth * scale;
        float displayHeight = imageHeight * scale;

        float offsetX = (getWidth() - displayWidth) / 2f;
        float offsetY = (getHeight() - displayHeight) / 2f;

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        if (bitmap != null) {
            canvas.drawBitmap(
                    bitmap, 0, 0, imagePaint
            );
        }

        for (YoloDetector.Detection detection : detections) {

            RectF box = detection.box;

            canvas.drawRect(box, boxPaint);

            String label = String.format(
                    Locale.US,
                    "%s %.0f%%",
                    detection.label,
                    detection.confidence * 100f
            );

            float textWidth = textPaint.measureText(label);
            float labelTop = Math.max(0, box.top - 42);

            canvas.drawRect(
                    box.left,
                    labelTop,
                    box.left + textWidth + 16,
                    labelTop + 42,
                    backgroundPaint
            );

            canvas.drawText(
                    label,
                    box.left + 8,
                    labelTop + 32,
                    textPaint
            );
        }

        canvas.restore();
    }
}
