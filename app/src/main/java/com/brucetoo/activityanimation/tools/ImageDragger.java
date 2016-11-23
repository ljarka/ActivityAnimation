package com.brucetoo.activityanimation.tools;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static com.brucetoo.activityanimation.tools.Preconditions.checkNotNull;

public class ImageDragger {
    public interface OnImageDragListener {

        void onStartDragging();

        void onEndDragging(View view, boolean wasLongDrag);

        void onLongDrag(View view);

        void onShortDrag(View view);
    }

    public static final OnImageDragListener EMPTY_IMAGE_DRAG_LISTENER = new OnImageDragListener() {
        @Override
        public void onStartDragging() {
            //empty listener
        }

        @Override
        public void onEndDragging(View view, boolean wasLongDrag) {
            //empty listener
        }

        @Override
        public void onLongDrag(View view) {
            //empty listener
        }

        @Override
        public void onShortDrag(View view) {
            //empty listener
        }
    };

    private static final float INSENSITIVE_Y_DRAG_OFFSET = 10;
    private static final float OFF_X_DRAG_MARGIN_DP = 7;
    private static final float EXTERNAL_DRAG_AREA_DP = 85;
    private float startDragX;
    private float startDragY;
    private boolean isDragging;
    private boolean wasLongDrag;
    private OnImageDragListener imageDragListener = EMPTY_IMAGE_DRAG_LISTENER;
    private Context context;
    private final float offXDragMarginPx;
    private final float externalDragAreaPx;
    private float lastXValue;
    private float lastYValue;

    public ImageDragger(@NonNull Context context) {
        this.context = checkNotNull(context);
        offXDragMarginPx = convertDpToPixel(OFF_X_DRAG_MARGIN_DP);
        externalDragAreaPx = convertDpToPixel(EXTERNAL_DRAG_AREA_DP);
    }

    private float convertDpToPixel(float dp) {
        return TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public void setOnImageDragListener(@NonNull OnImageDragListener onImageDragListener) {
        this.imageDragListener = checkNotNull(onImageDragListener);
    }

    public void handleDraggingTouchEvent(@NonNull MotionEvent event, @NonNull PhotoView view) {
        switch (event.getActionMasked()) {
            case ACTION_DOWN: {
                onActionDown(event);
                break;
            }
            case ACTION_MOVE: {
                onActionMove(event, view);
                break;
            }
            case ACTION_UP: {
                onActionUp(view);
                break;
            }
            case ACTION_CANCEL: {
                onActionUp(view);
                break;
            }
            default: {
                // nop
                break;
            }
        }
    }

    private void onActionUp(PhotoView photoView) {
        if (!wasLongDrag && !isOutsideExternalDragArea(photoView)) {
            animateToInitialPosition(photoView);
        }

        if (isDragging) {
            imageDragListener.onEndDragging(photoView, wasLongDrag);
        }
        isDragging = false;
        wasLongDrag = false;
        photoView.getParent().requestDisallowInterceptTouchEvent(false);
    }

    private void onActionMove(MotionEvent event, PhotoView view) {
        if (isSingleTouch(event) || isDragging) {
            handleDragEvent(event, view);
        }
    }

    private void handleDragEvent(MotionEvent event, PhotoView view) {
        float y = calculateY(event);
        float x = !isDragging && isInsideInsensitiveDragXMargin(view) ? 0 : calculateX(event);

        if (y > INSENSITIVE_Y_DRAG_OFFSET || y < -INSENSITIVE_Y_DRAG_OFFSET) {
            dragToPoint(view, x, y);
        }

        if (!isDragging && !isInsideInsensitiveDragXMargin(view)) {
            imageDragListener.onStartDragging();
            isDragging = true;
        }

        if (!wasLongDrag && isOutsideExternalDragArea(view)) {
            imageDragListener.onLongDrag(view);
            wasLongDrag = true;
            view.getParent().requestDisallowInterceptTouchEvent(true);
        }

        if (wasLongDrag && !isOutsideExternalDragArea(view)) {
            wasLongDrag = false;
            imageDragListener.onShortDrag(view);
        }
    }

    private float calculateY(MotionEvent event) {
        return event.getRawY() - startDragY;
    }

    private float calculateX(MotionEvent event) {
        return event.getRawX() - startDragX;
    }

    private void onActionDown(MotionEvent event) {
        updateDragInitialPoint(event);
    }

    private void updateDragInitialPoint(MotionEvent event) {
        startDragX = event.getRawX();
        startDragY = event.getRawY();
    }

    private void dragToPoint(PhotoView photoView, float xValue, float yValue) {
        photoView.dragBy(xValue - lastXValue, yValue - lastYValue);
        lastXValue = xValue;
        lastYValue = yValue;
    }

    private void animateToInitialPosition(PhotoView view) {
        if (view.getDraggedXDistance() != 0 || view.getDraggedYDistance() != 0) {
            view.revertDragWithAnimation();
            lastXValue = 0;
            lastYValue = 0;
        }
    }

    public boolean isDragging() {
        return isDragging;
    }

    private boolean isSingleTouch(MotionEvent event) {
        return event.getPointerCount() == 1;
    }

    private boolean isInsideInsensitiveDragXMargin(PhotoView view) {
        return view.getDraggedYDistance() < offXDragMarginPx && view.getDraggedYDistance() > -offXDragMarginPx;
    }

    private boolean isOutsideExternalDragArea(PhotoView view) {
        return isOutsideArea(externalDragAreaPx, view);
    }

    private boolean isOutsideArea(float areaSideSize, PhotoView view) {
        return (view.getDraggedYDistance() > areaSideSize || view.getDraggedYDistance() < -areaSideSize)
                || (view.getDraggedXDistance() > areaSideSize || view.getDraggedXDistance() < -areaSideSize);
    }
}
