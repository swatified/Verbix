package com.example.verbix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.google.mlkit.vision.digitalink.Ink;
import java.util.ArrayList;

public class DrawingView extends View {
    private Path path;
    private Paint paint;
    private ArrayList<Path> paths;
    private ArrayList<Paint> paints;
    private ArrayList<ArrayList<Ink.Point>> strokePoints;
    private ArrayList<Ink.Point> currentStrokePoints;

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
        strokePoints = new ArrayList<>();

        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(8f);
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
        long t = System.currentTimeMillis();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentStrokePoints = new ArrayList<>();
                currentStrokePoints.add(Ink.Point.create(x, y, t));
                path.moveTo(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                currentStrokePoints.add(Ink.Point.create(x, y, t));
                path.lineTo(x, y);
                break;

            case MotionEvent.ACTION_UP:
                // Store the drawn path and points
                paths.add(new Path(path));
                paints.add(new Paint(paint));
                strokePoints.add(new ArrayList<>(currentStrokePoints));

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
        strokePoints.clear();
        invalidate();
    }

    public ArrayList<Path> getPaths() {
        ArrayList<Path> allPaths = new ArrayList<>(paths);
        if (!path.isEmpty()) {
            allPaths.add(path);
        }
        return allPaths;
    }

    public ArrayList<ArrayList<Ink.Point>> getStrokePoints() {
        return strokePoints;
    }

    public Ink getInk() {
        Ink.Builder inkBuilder = Ink.builder();

        for (ArrayList<Ink.Point> points : strokePoints) {
            if (!points.isEmpty()) {
                Ink.Stroke.Builder strokeBuilder = Ink.Stroke.builder();
                // Add points individually
                for (Ink.Point point : points) {
                    strokeBuilder.addPoint(point);
                }
                inkBuilder.addStroke(strokeBuilder.build());
            }
        }

        // Add current stroke if it exists
        if (currentStrokePoints != null && !currentStrokePoints.isEmpty()) {
            Ink.Stroke.Builder strokeBuilder = Ink.Stroke.builder();
            // Add points individually
            for (Ink.Point point : currentStrokePoints) {
                strokeBuilder.addPoint(point);
            }
            inkBuilder.addStroke(strokeBuilder.build());
        }

        return inkBuilder.build();
    }
}