package com.brucetoo.activityanimation.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.Scroller;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_UP;
import static com.brucetoo.activityanimation.tools.Preconditions.checkNotNull;

/**
 * Class from https://github.com/brucetoo/ActivityAnimation
 * We have added dragging possibilities, done simple refactoring and fixed some issues
 */
public class PhotoView extends ImageView {
    public interface TransformAnimationListener {
        void onTransformAnimationStart(PhotoView photoView);
    }

    private static final TransformAnimationListener EMPTY_TRANSFORM_ANIMATION_LISTENER = photoView -> {
        // empty listener
    };
    private static final int TRANSFORM_DURATION = 300;
    private static final int CLIP_ANIMATION_DIVISION_FACTOR = 3;
    private static final int ON_START_CLIP_ANIMATION_DURATION = TRANSFORM_DURATION / CLIP_ANIMATION_DIVISION_FACTOR;
    private static final float SCALING_PRECISION = 0.01f;
    private static final float MAX_SCALE = 2.5f;
    private static final int MAX_ANIM_FROM_WAITE = 500;
    private static final int SINGLE_TAP_DELAY = 250;
    private static final int MAX_FLING_OVER_SCROLL_FACTOR = 30;
    private static final int MAX_OVER_RESISTANCE_FACTOR = 140;
    public static final int VALUES_SIZE = 16;
    private int maxFlingOverScroll = 0;
    private int maxOverResistance = 0;
    private Matrix baseMatrix = new Matrix();
    private Matrix animMatrix = new Matrix();
    private Matrix synthesisMatrix = new Matrix();
    private Matrix tempMatrix = new Matrix();
    private GestureDetectorCompat gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private OnClickListener onClickListener;
    private ScaleType scaleType;
    private boolean hasMultiTouch;
    private boolean hasDrawable;
    private boolean isKnowSize;
    private boolean hasOverTranslate;
    private boolean isEnabled = false;
    private boolean isInit;
    private boolean imageLargeWidth;
    private boolean imageLargeHeight;
    private float scale = 1.0f;
    private int translateX;
    private int translateY;
    private RectF widgetRect = new RectF();
    private RectF baseRect = new RectF();
    private RectF imageRect = new RectF();
    private RectF tempRect = new RectF();
    private RectF commonRect = new RectF();
    private PointF screenCenter = new PointF();
    private PointF doubleTap = new PointF();
    private Transform transformAnimation = new Transform();
    private RectF clip;
    private ImageInfo imageInfo;
    private long infoTime;
    private Runnable completeCallBack;
    private float[] values = new float[VALUES_SIZE];
    private ImageDragger imageDragger;
    private boolean isDraggingEnabled;
    private float draggedXDistance;
    private float draggedYDistance;

    private TransformAnimationListener transformAnimationListener = EMPTY_TRANSFORM_ANIMATION_LISTENER;

    public PhotoView(Context context) {
        super(context);
        init();
    }

    public PhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        super.setScaleType(ScaleType.MATRIX);
        imageDragger = new ImageDragger(getContext());
        if (scaleType == null) {
            scaleType = ScaleType.CENTER_INSIDE;
        }
        gestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), scaleListener);
        float density = getResources().getDisplayMetrics().density;
        maxFlingOverScroll = (int) (density * MAX_FLING_OVER_SCROLL_FACTOR);
        maxOverResistance = (int) (density * MAX_OVER_RESISTANCE_FACTOR);
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        super.setOnClickListener(onClickListener);
        this.onClickListener = onClickListener;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        ScaleType oldScaleType = this.scaleType;
        this.scaleType = scaleType;

        if (oldScaleType != scaleType) {
            initBase();
        }
    }

    public void touchEnable(boolean isEnable) {
        this.isEnabled = isEnable;
    }

    public void setTransformAnimationListener(@NonNull TransformAnimationListener transformAnimationListener) {
        this.transformAnimationListener = checkNotNull(transformAnimationListener);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);

        if (drawable == null) {
            hasDrawable = false;
        } else {
            hasDrawable = true;
            initBase();
        }
    }


    private int getDrawableWidth(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        if (width <= 0) {
            width = drawable.getMinimumWidth();
        }
        if (width <= 0) {
            width = drawable.getBounds().width();
        }
        return width;
    }

    private int getDrawableHeight(Drawable drawable) {
        int height = drawable.getIntrinsicHeight();
        if (height <= 0) {
            height = drawable.getMinimumHeight();
        }
        if (height <= 0) {
            height = drawable.getBounds().height();
        }
        return height;
    }

    private void initBase() {
        if (!hasDrawable) {
            return;
        }
        if (!isKnowSize) {
            return;
        }

        baseMatrix.reset();
        animMatrix.reset();

        Drawable img = getDrawable();

        int width = getWidth();
        int height = getHeight();
        int drawableWidth = getDrawableWidth(img);
        int drawableHeight = getDrawableHeight(img);

        baseRect.set(0, 0, drawableWidth, drawableHeight);

        int tx = (width - drawableWidth) / 2;
        int ty = (height - drawableHeight) / 2;

        float sx = 1;
        float sy = 1;

        if (drawableWidth > width) {
            sx = (float) width / drawableWidth;
        }

        if (drawableHeight > height) {
            sy = (float) height / drawableHeight;
        }

        float scale = sx < sy ? sx : sy;

        baseMatrix.reset();
        baseMatrix.postTranslate(tx, ty);
        baseMatrix.postScale(scale, scale, screenCenter.x, screenCenter.y);
        baseMatrix.mapRect(baseRect);

        doubleTap.set(screenCenter);

        executeTranslate();

        switch (scaleType) {
            case CENTER:
                initCenter();
                break;
            case CENTER_CROP:
                initCenterCrop();
                break;
            case CENTER_INSIDE:
                initCenterInside();
                break;
            case FIT_CENTER:
                initFitCenter();
                break;
            case FIT_START:
                initFitStart();
                break;
            case FIT_END:
                initFitEnd();
                break;
            case FIT_XY:
                initFitXY();
                break;
            default:
                //nop
        }

        isInit = true;

        if (imageInfo != null && System.currentTimeMillis() - infoTime < MAX_ANIM_FROM_WAITE) {
            animateFrom(imageInfo);
        }

        imageInfo = null;
    }

    private void initCenter() {
        if (!hasDrawable) {
            return;
        }
        if (!isKnowSize) {
            return;
        }

        Drawable drawable = getDrawable();

        int drawableIntrinsicWidth = drawable.getIntrinsicWidth();
        int drawableIntrinsicHeight = drawable.getIntrinsicHeight();

        if (drawableIntrinsicWidth > widgetRect.width() || drawableIntrinsicHeight > widgetRect.height()) {
            float scaleX = drawableIntrinsicWidth / imageRect.width();
            float scaleY = drawableIntrinsicHeight / imageRect.height();

            scale = scaleX > scaleY ? scaleX : scaleY;

            animMatrix.postScale(scale, scale, screenCenter.x, screenCenter.y);

            executeTranslate();
        }
    }

    private void initCenterCrop() {
        if (imageRect.width() < widgetRect.width() || imageRect.height() < widgetRect.height()) {
            float scaleX = widgetRect.width() / imageRect.width();
            float scaleY = widgetRect.height() / imageRect.height();

            scale = scaleX > scaleY ? scaleX : scaleY;

            animMatrix.postScale(scale, scale, screenCenter.x, screenCenter.y);

            executeTranslate();
        }
    }

    private void initCenterInside() {
        if (imageRect.width() > widgetRect.width() || imageRect.height() > widgetRect.height()) {
            float scaleX = widgetRect.width() / imageRect.width();
            float scaleY = widgetRect.height() / imageRect.height();

            scale = scaleX < scaleY ? scaleX : scaleY;

            animMatrix.postScale(scale, scale, screenCenter.x, screenCenter.y);

            executeTranslate();
        }
    }

    private void initFitCenter() {
        if (imageRect.width() < widgetRect.width()) {
            scale = widgetRect.width() / imageRect.width();

            animMatrix.postScale(scale, scale, screenCenter.x, screenCenter.y);

            executeTranslate();
        }
    }

    private void initFitStart() {
        initFitCenter();

        float ty = -imageRect.top;
        translateY += ty;
        animMatrix.postTranslate(0, ty);
        executeTranslate();
    }

    private void initFitEnd() {
        initFitCenter();

        float ty = (widgetRect.bottom - imageRect.bottom);
        translateY += ty;
        animMatrix.postTranslate(0, ty);
        executeTranslate();
    }

    private void initFitXY() {
        float scaleX = widgetRect.width() / imageRect.width();
        float scaleY = widgetRect.height() / imageRect.height();

        animMatrix.postScale(scaleX, scaleY, screenCenter.x, screenCenter.y);

        executeTranslate();
    }

    private void executeTranslate() {
        synthesisMatrix.set(baseMatrix);
        synthesisMatrix.postConcat(animMatrix);
        setImageMatrix(synthesisMatrix);

        animMatrix.mapRect(imageRect, baseRect);

        imageLargeWidth = imageRect.width() > widgetRect.width();
        imageLargeHeight = imageRect.height() > widgetRect.height();
        transformAnimationListener.onTransformAnimationStart(PhotoView.this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!hasDrawable) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        Drawable d = getDrawable();
        int drawableW = getDrawableWidth(d);
        int drawableH = getDrawableHeight(d);

        int pWidth = MeasureSpec.getSize(widthMeasureSpec);
        int pHeight = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0;
        int height = 0;

        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if (layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            if (widthMode == MeasureSpec.UNSPECIFIED) {
                width = drawableW;
            } else {
                width = pWidth;
            }
        } else {
            if (widthMode == MeasureSpec.EXACTLY) {
                width = pWidth;
            } else if (widthMode == MeasureSpec.AT_MOST) {
                width = drawableW > pWidth ? pWidth : drawableW;
            } else {
                width = drawableW;
            }
        }

        if (layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            if (heightMode == MeasureSpec.UNSPECIFIED) {
                height = drawableH;
            } else {
                height = pHeight;
            }
        } else {
            if (heightMode == MeasureSpec.EXACTLY) {
                height = pHeight;
            } else if (heightMode == MeasureSpec.AT_MOST) {
                height = drawableH > pHeight ? pHeight : drawableH;
            } else {
                height = drawableH;
            }
        }

        if (getAdjustViewBounds() && (float) drawableW / drawableH != (float) width / height) {

            float hScale = (float) height / drawableH;
            float wScale = (float) width / drawableW;

            float scale = hScale < wScale ? hScale : wScale;
            width = layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT ? width : (int) (drawableW * scale);
            height = layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT ? height : (int) (drawableH * scale);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        widgetRect.set(0, 0, width, height);
        screenCenter.set(width / 2, height / 2);

        if (!isKnowSize) {
            isKnowSize = true;
            initBase();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (clip != null) {
            canvas.clipRect(clip);
            clip = null;
        }
        super.draw(canvas);
    }

    public void setOnImageDragListener(ImageDragger.OnImageDragListener onDragListener) {
        imageDragger.setOnImageDragListener(onDragListener);
    }

    public void setDraggingEnabled(boolean isDraggingEnabled) {
        this.isDraggingEnabled = isDraggingEnabled;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isEnabled) {
            if (isDraggingEnabled && Math.abs(scale - 1) < SCALING_PRECISION && !transformAnimation.isRunning) {
                imageDragger.handleDraggingTouchEvent(event, this);
            } else if (!imageDragger.isDragging()) {
                final int action = event.getAction();
                getParent().requestDisallowInterceptTouchEvent(true);
                if (action == ACTION_UP || action == ACTION_CANCEL) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }

            if (!imageDragger.isDragging()) {
                gestureDetector.onTouchEvent(event);
                scaleGestureDetector.onTouchEvent(event);
                final int action = event.getAction();
                if (action == ACTION_UP || action == ACTION_CANCEL) {
                    onUp();
                }
            } else {
                return super.dispatchTouchEvent(event);
            }
            return true;
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    private void onUp() {
        if (transformAnimation.isRunning) {
            return;
        }

        float scale = this.scale;

        if (this.scale < 1) {
            scale = 1;
            transformAnimation.withScale(this.scale, 1);
        } else if (this.scale > MAX_SCALE) {
            scale = MAX_SCALE;
            transformAnimation.withScale(this.scale, MAX_SCALE);
        }

        animMatrix.getValues(values);
        float s = values[Matrix.MSCALE_X];
        float tx = values[Matrix.MTRANS_X];
        float ty = values[Matrix.MTRANS_Y];

        float cpx = tx - translateX;
        float cpy = ty - translateY;

        doubleTap.x = -cpx / (s - 1);
        doubleTap.y = -cpy / (s - 1);

        tempRect.set(imageRect);

        if (scale != this.scale) {
            tempMatrix.setScale(scale, scale, doubleTap.x, doubleTap.y);
            tempMatrix.postTranslate(translateX, translateY);
            tempMatrix.mapRect(tempRect, baseRect);
        }

        doTranslateReset(tempRect);
        transformAnimation.start();
    }

    private void doTranslateReset(RectF imgRect) {
        int tx = 0;
        int ty = 0;

        if (imgRect.width() < widgetRect.width()) {
            if (!isImageCenterWidth()) {
                tx = -(int) ((widgetRect.width() - imgRect.width()) / 2 - imgRect.left);
            }
        } else {
            if (imgRect.left > widgetRect.left) {
                tx = (int) (imgRect.left - widgetRect.left);
            } else if (imgRect.right < widgetRect.right) {
                tx = (int) (imgRect.right - widgetRect.right);
            }
        }

        if (imgRect.height() < widgetRect.height()) {
            if (!isImageCenterHeight()) {
                ty = -(int) ((widgetRect.height() - imgRect.height()) / 2 - imgRect.top);
            }
        } else {
            if (imgRect.top > widgetRect.top) {
                ty = (int) (imgRect.top - widgetRect.top);
            } else if (imgRect.bottom < widgetRect.bottom) {
                ty = (int) (imgRect.bottom - widgetRect.bottom);
            }
        }

        if (tx != 0 || ty != 0) {
            if (!transformAnimation.flingScroller.isFinished()) {
                transformAnimation.flingScroller.abortAnimation();
            }
            transformAnimation.withTranslate(translateX, translateY, -tx, -ty);
        }
    }

    private boolean isImageCenterHeight() {
        return Math.round(imageRect.top) == (widgetRect.height() - imageRect.height()) / 2;
    }

    private boolean isImageCenterWidth() {
        return Math.round(imageRect.left) == (widgetRect.width() - imageRect.width()) / 2;
    }

    private ScaleGestureDetector.OnScaleGestureListener scaleListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();

            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor)) {
                return false;
            }

            scale *= scaleFactor;
            doubleTap.set(detector.getFocusX(), detector.getFocusY());
            animMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            executeTranslate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            hasMultiTouch = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };

    public void dragBy(float dx, float dy) {
        animMatrix.postTranslate(dx, dy);
        draggedXDistance += dx;
        draggedYDistance += dy;
        executeTranslate();
    }

    public void revertDragWithAnimation() {
        transformAnimation.stop();
        translateX = (int) draggedXDistance;
        translateY = (int) draggedYDistance;
        transformAnimation.withTranslate(0, 0, -translateX, -translateY);
        transformAnimation.start();
        draggedYDistance = 0;
        draggedXDistance = 0;
    }

    public float getDraggedXDistance() {
        return draggedXDistance;
    }

    public float getDraggedYDistance() {
        return draggedYDistance;
    }

    private float resistanceScrollByX(float overScroll, float deltaX) {
        return deltaX * (Math.abs(Math.abs(overScroll) - maxOverResistance) / (float) maxOverResistance);
    }

    private float resistanceScrollByY(float overScroll, float deltaY) {
        return deltaY * (Math.abs(Math.abs(overScroll) - maxOverResistance) / (float) maxOverResistance);
    }

    private void mapRect(RectF firstRect, RectF secondRect, RectF out) {

        float left, right, top, bottom;

        left = firstRect.left > secondRect.left ? firstRect.left : secondRect.left;
        right = firstRect.right < secondRect.right ? firstRect.right : secondRect.right;

        if (left > right) {
            out.set(0, 0, 0, 0);
            return;
        }

        top = firstRect.top > secondRect.top ? firstRect.top : secondRect.top;
        bottom = firstRect.bottom < secondRect.bottom ? firstRect.bottom : secondRect.bottom;

        if (top > bottom) {
            out.set(0, 0, 0, 0);
            return;
        }

        out.set(left, top, right, bottom);
    }

    private void checkRect() {
        if (!hasOverTranslate) {
            mapRect(widgetRect, imageRect, commonRect);
        }
    }

    private Runnable clickRunnable = new Runnable() {
        @Override
        public void run() {
            if (onClickListener != null) {
                onClickListener.onClick(PhotoView.this);
            }
        }
    };

    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent event) {
            hasOverTranslate = false;
            hasMultiTouch = false;
            removeCallbacks(clickRunnable);
            return super.onDown(event);
        }

        @Override
        public boolean onFling(MotionEvent firstEvent, MotionEvent secondEvent, float velocityX, float velocityY) {
            if (hasMultiTouch) {
                return false;
            }
            if (!imageLargeWidth && !imageLargeHeight) {
                return false;
            }
            if (!transformAnimation.flingScroller.isFinished()) {
                return false;
            }

            float vx = velocityX;
            float vy = velocityY;

            if (Math.round(imageRect.left) >= widgetRect.left || Math.round(imageRect.right) <= widgetRect.right) {
                vx = 0;
            }

            if (Math.round(imageRect.top) >= widgetRect.top || Math.round(imageRect.bottom) <= widgetRect.bottom) {
                vy = 0;
            }

            doTranslateReset(imageRect);
            transformAnimation.withFling(vx, vy);
            transformAnimation.start();
            return super.onFling(firstEvent, secondEvent, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (transformAnimation.isRunning) {
                transformAnimation.stop();
            }

            if (canScrollHorizontallySelf(distanceX)) {
                if (distanceX < 0 && imageRect.left - distanceX > widgetRect.left) {
                    distanceX = imageRect.left;
                }
                if (distanceX > 0 && imageRect.right - distanceX < widgetRect.right) {
                    distanceX = imageRect.right - widgetRect.right;
                }

                animMatrix.postTranslate(-distanceX, 0);
                translateX -= distanceX;
            } else if (imageLargeWidth || hasMultiTouch || hasOverTranslate) {
                checkRect();
                if (!hasMultiTouch) {
                    if (distanceX < 0 && imageRect.left - distanceX > commonRect.left) {
                        distanceX = resistanceScrollByX(imageRect.left - commonRect.left, distanceX);
                    }
                    if (distanceX > 0 && imageRect.right - distanceX < commonRect.right) {
                        distanceX = resistanceScrollByX(imageRect.right - commonRect.right, distanceX);
                    }
                }

                translateX -= distanceX;
                animMatrix.postTranslate(-distanceX, 0);
                hasOverTranslate = true;
            }

            if (canScrollVerticallySelf(distanceY)) {
                if (distanceY < 0 && imageRect.top - distanceY > widgetRect.top) {
                    distanceY = imageRect.top;
                }
                if (distanceY > 0 && imageRect.bottom - distanceY < widgetRect.bottom) {
                    distanceY = imageRect.bottom - widgetRect.bottom;
                }

                animMatrix.postTranslate(0, -distanceY);
                translateY -= distanceY;
            } else if (imageLargeHeight || hasOverTranslate || hasMultiTouch) {
                checkRect();
                if (!hasMultiTouch) {
                    if (distanceY < 0 && imageRect.top - distanceY > commonRect.top) {
                        distanceY = resistanceScrollByY(imageRect.top - commonRect.top, distanceY);
                    }
                    if (distanceY > 0 && imageRect.bottom - distanceY < commonRect.bottom) {
                        distanceY = resistanceScrollByY(imageRect.bottom - commonRect.bottom, distanceY);
                    }
                }

                animMatrix.postTranslate(0, -distanceY);
                translateY -= distanceY;
                hasOverTranslate = true;
            }

            executeTranslate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            postDelayed(clickRunnable, SINGLE_TAP_DELAY);
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            transformAnimation.stop();

            float from = 1;
            float to = 1;

            if (scale == 1) {
                from = 1;
                to = MAX_SCALE;

                if (imageRect.width() < widgetRect.width()) {
                    doubleTap.set(screenCenter.x, screenCenter.y);
                } else {
                    doubleTap.set(e.getX(), screenCenter.y);
                }
            } else {
                from = scale;
                to = 1;

                transformAnimation.withTranslate(translateX, translateY, -translateX, -translateY);
            }

            transformAnimation.withScale(from, to);
            transformAnimation.start();

            return false;
        }
    };

    public boolean canScrollHorizontallySelf(float direction) {
        if (imageRect.width() <= widgetRect.width()) {
            return false;
        }
        if (direction < 0 && Math.round(imageRect.left) - direction >= widgetRect.left) {
            return false;
        }
        if (direction > 0 && Math.round(imageRect.right) - direction <= widgetRect.right) {
            return false;
        }
        return true;
    }

    public boolean canScrollVerticallySelf(float direction) {
        if (imageRect.height() <= widgetRect.height()) {
            return false;
        }
        if (direction < 0 && Math.round(imageRect.top) - direction >= widgetRect.top) {
            return false;
        }
        if (direction > 0 && Math.round(imageRect.bottom) - direction <= widgetRect.bottom) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return hasMultiTouch || canScrollHorizontallySelf(direction);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return hasMultiTouch || canScrollVerticallySelf(direction);
    }

    private class Transform implements Runnable {
        private static final float FACTOR = 10000f;
        boolean isRunning;

        OverScroller translateScroller;
        OverScroller flingScroller;
        Scroller scaleScroller;
        Scroller clipScroll;

        ClipCalculate clipCalculate;

        int lastFlingX;
        int lastFlingY;

        int lastTranslateX;
        int lastTranslateY;

        RectF clipRect = new RectF();

        Transform() {
            Context ctx = getContext();
            DecelerateInterpolator i = new DecelerateInterpolator();
            translateScroller = new OverScroller(ctx, i);
            scaleScroller = new Scroller(ctx, i);
            flingScroller = new OverScroller(ctx, i);
            clipScroll = new Scroller(ctx, i);
        }

        void withTranslate(int startX, int startY, int deltaX, int deltaY) {
            lastTranslateX = 0;
            lastTranslateY = 0;
            translateScroller.startScroll(0, 0, deltaX, deltaY, TRANSFORM_DURATION);
        }

        void withScale(float form, float to) {
            scaleScroller.startScroll((int) (form * FACTOR), 0, (int) ((to - form) * FACTOR), 0, TRANSFORM_DURATION);
        }

        void withClip(float fromX, float fromY, float deltaX, float deltaY, int d, ClipCalculate c) {
            clipScroll.startScroll((int) (fromX * FACTOR), (int) (fromY * FACTOR),
                    (int) (deltaX * FACTOR), (int) (deltaY * FACTOR), d);
            clipCalculate = c;
        }

        void withFling(float velocityX, float velocityY) {
            lastFlingX = velocityX < 0 ? Integer.MAX_VALUE : 0;
            int distanceX = (int) (velocityX > 0 ? Math.abs(imageRect.left) : imageRect.right - widgetRect.right);
            distanceX = velocityX < 0 ? Integer.MAX_VALUE - distanceX : distanceX;
            int minX = velocityX < 0 ? distanceX : 0;
            int maxX = velocityX < 0 ? Integer.MAX_VALUE : distanceX;
            int overX = velocityX < 0 ? Integer.MAX_VALUE - minX : distanceX;

            lastFlingY = velocityY < 0 ? Integer.MAX_VALUE : 0;
            int distanceY = (int) (velocityY > 0 ? Math.abs(imageRect.top) : imageRect.bottom - widgetRect.bottom);
            distanceY = velocityY < 0 ? Integer.MAX_VALUE - distanceY : distanceY;
            int minY = velocityY < 0 ? distanceY : 0;
            int maxY = velocityY < 0 ? Integer.MAX_VALUE : distanceY;
            int overY = velocityY < 0 ? Integer.MAX_VALUE - minY : distanceY;

            if (velocityX == 0) {
                maxX = 0;
                minX = 0;
            }

            if (velocityY == 0) {
                maxY = 0;
                minY = 0;
            }

            flingScroller.fling(lastFlingX, lastFlingY, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY,
                    Math.abs(overX) < maxFlingOverScroll * 2 ? 0 : maxFlingOverScroll,
                    Math.abs(overY) < maxFlingOverScroll * 2 ? 0 : maxFlingOverScroll);
        }

        void start() {
            isRunning = true;
            post(this);
        }

        void stop() {
            removeCallbacks(this);
            translateScroller.abortAnimation();
            scaleScroller.abortAnimation();
            flingScroller.abortAnimation();

            isRunning = false;
        }

        @Override
        public void run() {

            if (!isRunning) {
                return;
            }

            boolean endAnima = true;

            if (scaleScroller.computeScrollOffset()) {
                scale = scaleScroller.getCurrX() / FACTOR;
                endAnima = false;
            }

            if (translateScroller.computeScrollOffset()) {
                int tx = translateScroller.getCurrX() - lastTranslateX;
                int ty = translateScroller.getCurrY() - lastTranslateY;
                translateX += tx;
                translateY += ty;
                lastTranslateX = translateScroller.getCurrX();
                lastTranslateY = translateScroller.getCurrY();
                endAnima = false;
            }

            if (flingScroller.computeScrollOffset()) {
                int x = flingScroller.getCurrX() - lastFlingX;
                int y = flingScroller.getCurrY() - lastFlingY;

                lastFlingX = flingScroller.getCurrX();
                lastFlingY = flingScroller.getCurrY();

                translateX += x;
                translateY += y;
                endAnima = false;
            }

            if (clipScroll.computeScrollOffset() || clip != null) {
                float sx = clipScroll.getCurrX() / FACTOR;
                float sy = clipScroll.getCurrY() / FACTOR;
                tempMatrix.setScale(sx, sy, (imageRect.left + imageRect.right) / 2, clipCalculate.calculateTop());
                tempMatrix.mapRect(clipRect, imageRect);
                clip = clipRect;
            }

            if (!endAnima) {
                animMatrix.reset();
                animMatrix.postScale(scale, scale, doubleTap.x, doubleTap.y);
                animMatrix.postTranslate(translateX, translateY);
                executeTranslate();
                post(this);
            } else {
                isRunning = false;
                invalidate();

                if (completeCallBack != null) {
                    completeCallBack.run();
                    completeCallBack = null;
                }
            }
        }
    }

    public ImageInfo getInfo() {
        Rect globalVisibleRect = new Rect();
        getGlobalVisibleRect(globalVisibleRect);
        RectF clipRect = new RectF();
        clipRect.set(0, 0, globalVisibleRect.width(), globalVisibleRect.height());
        RectF rect = new RectF();
        RectF local = new RectF();
        int[] p = new int[2];
        getLocationInWindow(p);
        rect.set(p[0] + imageRect.left, p[1] + imageRect.top, p[0] + imageRect.right, p[1] + imageRect.bottom);
        local.set(p[0], p[1], p[0] + imageRect.width(), p[1] + imageRect.height());
        return new ImageInfo(rect, local, imageRect, widgetRect, clipRect, scale, scaleType);
    }

    private void reset() {
        animMatrix.reset();
        executeTranslate();
        scale = 1;
        translateX = 0;
        translateY = 0;
    }

    public interface ClipCalculate {
        float calculateTop();
    }

    public class START implements ClipCalculate {
        public float calculateTop() {
            return imageRect.top;
        }
    }

    public class END implements ClipCalculate {
        public float calculateTop() {
            return imageRect.bottom;
        }
    }

    public class OTHER implements ClipCalculate {
        public float calculateTop() {
            return (imageRect.top + imageRect.bottom) / 2;
        }
    }

    public void animateFrom(ImageInfo imageInfo) {
        if (isInit) {
            reset();

            ImageInfo mine = getInfo();

            float scaleX = imageInfo.imageRect.width() / mine.imageRect.width();
            float scaleY = imageInfo.imageRect.height() / mine.imageRect.height();

            float scale = scaleX < scaleY ? scaleX : scaleY;
            float tx = imageInfo.rect.left - mine.rect.left;
            float ty = imageInfo.rect.top - mine.rect.top;

            animMatrix.reset();
            animMatrix.postScale(scale, scale, imageRect.left, imageRect.top);
            animMatrix.postTranslate(tx, ty);

            executeTranslate();

            translateX += tx;
            translateY += ty;

            doubleTap.x = imageRect.left - tx;
            doubleTap.y = imageRect.top - ty;

            animMatrix.getValues(values);
            float s = values[Matrix.MSCALE_X];

            transformAnimation.withScale(s, this.scale);
            transformAnimation.withTranslate(translateX, translateY, (int) -tx, (int) -ty);

            if (shouldClip(imageInfo.clipRect, imageInfo.imageRect)) {
                float clipX = calculateClipX(imageInfo.clipRect, imageInfo.imageRect);
                float clipY = calculateClipY(imageInfo.clipRect, imageInfo.imageRect);

                ClipCalculate c = imageInfo.scaleType == ScaleType.FIT_START ? new START()
                        : imageInfo.scaleType == ScaleType.FIT_END ? new END() : new OTHER();

                transformAnimation.withClip(clipX, clipY, 1 - clipX, 1 - clipY, ON_START_CLIP_ANIMATION_DURATION, c);

                tempMatrix.setScale(clipX, clipY, (imageRect.left + imageRect.right) / 2, c.calculateTop());
                tempMatrix.mapRect(transformAnimation.clipRect, imageRect);
                clip = transformAnimation.clipRect;
            }

            transformAnimation.start();
        } else {
            this.imageInfo = imageInfo;
            infoTime = System.currentTimeMillis();
        }
    }

    public void animateTo(ImageInfo imageInfo, Runnable completeCallBack) {
        if (isInit) {
            executeTranslate();
            ImageInfo mine = getInfo();

            doubleTap.x = 0;
            doubleTap.y = 0;

            animMatrix.getValues(values);
            scale = values[Matrix.MSCALE_X];
            translateX = (int) values[Matrix.MTRANS_X];
            translateY = (int) values[Matrix.MTRANS_Y];

            float scaleX = imageInfo.imageRect.width() / mine.imageRect.width();
            float scaleY = imageInfo.imageRect.height() / mine.imageRect.height();
            float scale = scaleX > scaleY ? scaleX : scaleY;

            tempMatrix.set(animMatrix);
            tempMatrix.postScale(scale, scale, doubleTap.x, doubleTap.y);
            tempMatrix.getValues(values);
            scale = values[Matrix.MSCALE_X];

            baseMatrix.getValues(values);

            int tx = (int) (imageInfo.localRect.left - mine.localRect.left
                    + imageInfo.imageRect.left - values[Matrix.MTRANS_X] * scale);
            int ty = (int) (imageInfo.localRect.top - mine.localRect.top
                    + imageInfo.imageRect.top - values[Matrix.MTRANS_Y] * scale);

            transformAnimation.withScale(this.scale, scale);
            transformAnimation.withTranslate(translateX, translateY, -translateX + tx, -translateY + ty);

            if (shouldClip(imageInfo.clipRect, imageInfo.rect)) {
                float clipX = calculateClipX(imageInfo.clipRect, imageInfo.rect);
                float clipY = calculateClipY(imageInfo.clipRect, imageInfo.rect);

                final ClipCalculate clipCalculate = imageInfo.scaleType == ScaleType.FIT_START ? new START()
                        : imageInfo.scaleType == ScaleType.FIT_END ? new END() : new OTHER();

                postDelayed(() -> transformAnimation.withClip(1, 1, -1 + clipX, -1 + clipY,
                        TRANSFORM_DURATION / 2, clipCalculate), TRANSFORM_DURATION / 2);
            }

            this.completeCallBack = completeCallBack;
            transformAnimation.start();
        } else {
            postDelayed(completeCallBack, TRANSFORM_DURATION);
        }
    }

    private float calculateClipX(RectF clipRect, RectF currentImageRect) {
        return Math.min(clipRect.width() / currentImageRect.width(), 1);
    }

    private float calculateClipY(RectF clipRect, RectF currentImageRect) {
        return Math.min(clipRect.height() / currentImageRect.height(), 1);
    }

    private boolean shouldClip(RectF clipRect, RectF currentImageRect) {
        return clipRect.width() < currentImageRect.width()
                || clipRect.height() < currentImageRect.height();
    }
}