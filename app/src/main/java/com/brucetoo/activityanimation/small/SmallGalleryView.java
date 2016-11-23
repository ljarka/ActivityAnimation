package com.brucetoo.activityanimation.small;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.brucetoo.activityanimation.R;
import com.brucetoo.activityanimation.tools.ImageInfo;
import com.brucetoo.activityanimation.tools.PhotoView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;

import static butterknife.ButterKnife.findById;

public class SmallGalleryView extends FrameLayout implements SmallGallery {

    private static final OnItemClickListener EMPTY_ON_ITEM_CLICK_LISTENER = (currentItemPosition, currentItemImageInfo) -> {
        // nop
    };

    @BindView(R.id.gallery_pager)
    ViewPager smallGalleryPager;

    @BindView(R.id.gallery_pager_indicator)
    CirclePageIndicator pageIndicator;

    private OnItemClickListener onItemClickListener = EMPTY_ON_ITEM_CLICK_LISTENER;
    private GestureDetector galleryTapGestureDetector;

    public SmallGalleryView(Context context) {
        super(context);
        init();
    }

    public SmallGalleryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmallGalleryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.small_gallery_content, this);

        if (isInEditMode()) {
            return;
        }

        ButterKnife.bind(this);
        galleryTapGestureDetector = createOnGalleryTapGestureDetector();
    }

    private GestureDetector createOnGalleryTapGestureDetector() {
        return new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                onItemClickListener.onItemClicked(smallGalleryPager.getCurrentItem(), getCurrentItemImageInfo());

                return false;
            }
        });
    }

    @OnTouch(R.id.gallery_pager)
    public boolean onGalleryTouched(MotionEvent event) {
        galleryTapGestureDetector.onTouchEvent(event);
        return false;
    }

    @Override
    public void hideAllImages() {
        for (int i = 0; i < smallGalleryPager.getChildCount(); i++) {
            smallGalleryPager.getChildAt(i).setVisibility(INVISIBLE);
        }
    }

    @Override
    public void showAllHiddenImages() {
        for (int i = 0; i < smallGalleryPager.getChildCount(); i++) {
            smallGalleryPager.getChildAt(i).setVisibility(VISIBLE);
        }
    }

    private ImageInfo getCurrentItemImageInfo() {
        return getImageInfo(smallGalleryPager.getCurrentItem());
    }

    @NonNull
    @Override
    public ImageInfo getImageInfo(int pageIndex) {
        View itemView = smallGalleryPager.findViewWithTag(pageIndex);
        PhotoView photoView = findById(itemView, R.id.photo_view);
        return photoView.getInfo();
    }

    @Override
    public void setCurrentItem(int page) {
        smallGalleryPager.setCurrentItem(page);
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener != null ? onItemClickListener
                : EMPTY_ON_ITEM_CLICK_LISTENER;
    }

    public void setImages(List<String> images) {
        smallGalleryPager.setAdapter(new SmallGalleryPagerAdapter(getContext(), images));
        pageIndicator.setViewPager(smallGalleryPager);
        invalidatePageIndicatorWidth();
    }

    // fix measuring problem with pageIndicator width set to wrap_content
    private void invalidatePageIndicatorWidth() {
        pageIndicator.requestLayout();
    }

    public interface OnItemClickListener {
        void onItemClicked(int currentItemPosition, ImageInfo currentItemImageInfo);
    }
}
