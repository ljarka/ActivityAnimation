package com.brucetoo.activityanimation.fullscreen;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.brucetoo.activityanimation.tools.ProgressBarRequestListener;
import com.brucetoo.activityanimation.R;
import com.brucetoo.activityanimation.tools.ImageDragger;
import com.brucetoo.activityanimation.tools.ImageInfo;
import com.brucetoo.activityanimation.tools.PhotoView;
import com.bumptech.glide.Glide;

import java.util.List;

import static butterknife.ButterKnife.findById;

class FullScreenGalleryPagerAdapter extends PagerAdapter implements PhotoView.TransformAnimationListener {
    public static final OnImageAnimationStartListener EMPTY_IMAGE_ANIMATION_START_LISTENER = () -> {
        // empty listener
    };

    public interface OnBackPressedListener {

        void onBackPressed(@NonNull PhotoView photoView);
    }

    public interface OnImageAnimationStartListener {

        void onImageAnimationStart();
    }

    private ImageDragger.OnImageDragListener onImageDragListener = ImageDragger.EMPTY_IMAGE_DRAG_LISTENER;

    private OnBackPressedListener onBackPressedListener = photoView -> {
        // empty listener
    };
    private OnImageAnimationStartListener onImageAnimationStartListener = EMPTY_IMAGE_ANIMATION_START_LISTENER;

    private final List<String> images;
    private final int initPagePosition;
    private final ImageInfo initPageImageInfo;
    private boolean wasEnterAnimation;

    public FullScreenGalleryPagerAdapter(Context context, List<String> images, ImageInfo initPageImageInfo, int initPagePosition) {
        this.images = images;
        this.initPageImageInfo = initPageImageInfo;
        this.initPagePosition = initPagePosition;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.full_screen_gallery_image_detail, container, false);
        final PhotoView photoView = findById(view, R.id.image_detail);
        final ProgressBar progressBar = findById(view, R.id.progress_bar);
        photoView.setDraggingEnabled(true);
        photoView.setOnImageDragListener(onImageDragListener);
        loadImage(photoView, progressBar, images.get(position));
        if (!wasEnterAnimation && initPagePosition == position) {
            photoView.setTransformAnimationListener(this);
            photoView.animateFrom(initPageImageInfo);
            wasEnterAnimation = true;
        }
        photoView.setFocusableInTouchMode(true);
        photoView.setOnKeyListener(onBackPressKeyListener);
        photoView.requestFocus();
        photoView.setTag(position);
        photoView.touchEnable(true);
        container.addView(view);
        return view;
    }

    protected void loadImage(ImageView imageView, ProgressBar progressBar, String photoUrl) {
        Glide.with(imageView.getContext())
                .load(photoUrl)
                .dontAnimate()
                .listener(new ProgressBarRequestListener(progressBar))
                .into(imageView);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public void setOnImageDragListener(@NonNull ImageDragger.OnImageDragListener onImageDragListener) {
        this.onImageDragListener = onImageDragListener;
    }

    public void setOnBackPressedListener(@NonNull OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    public void setOnImageAnimationStartListener(@NonNull OnImageAnimationStartListener onImageAnimationStartListener) {
        this.onImageAnimationStartListener = onImageAnimationStartListener;
    }

    private View.OnKeyListener onBackPressKeyListener = (view, keyCode, event) -> {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() != KeyEvent.ACTION_UP) {
                return true;
            }
            onBackPressedListener.onBackPressed((PhotoView) view);
            return true;
        }
        return false;
    };

    @Override
    public void onTransformAnimationStart(PhotoView photoView) {
        onImageAnimationStartListener.onImageAnimationStart();
    }
}
