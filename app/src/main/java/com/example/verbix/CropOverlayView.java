package com.example.verbix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CropOverlayView extends View {
    private Paint paint;
    private RectF cropRect;

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
    }

    public void setCropRect(RectF rect) {
        this.cropRect = rect;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cropRect != null) {
            // Draw semi-transparent overlay
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#80000000"));
            canvas.drawRect(0, 0, getWidth(), cropRect.top, paint);
            canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), paint);
            canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, paint);
            canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, paint);

            // Draw crop rectangle
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            canvas.drawRect(cropRect, paint);

            // Draw corner handles
            paint.setStyle(Paint.Style.FILL);
            float handleRadius = 20f;
            canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, paint);
            canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, paint);
            canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, paint);
            canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, paint);
        }
    }
}