package com.example.verbix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class DrawingView extends View {
    private Path path;
    private Paint paint;
    private ArrayList<Path> paths;
    private ArrayList<Paint> paints;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        path = new Path();
        paint = new Paint();
        paths = new ArrayList<>();
        paints = new ArrayList<>();

        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw all the previous paths
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }

        // Draw the current path
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                // Store the drawn path
                paths.add(path);
                paints.add(paint);
                // Start new path
                path = new Path();
                paint = new Paint(paint);
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    public void clear() {
        path = new Path();
        paths.clear();
        paints.clear();
        invalidate();
    }

    // Method to get the drawn paths for recognition
    public ArrayList<Path> getPaths() {
        ArrayList<Path> allPaths = new ArrayList<>(paths);
        allPaths.add(path);
        return allPaths;
    }
}