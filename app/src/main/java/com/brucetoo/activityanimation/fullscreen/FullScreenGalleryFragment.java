package com.brucetoo.activityanimation.fullscreen;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import com.brucetoo.activityanimation.R;
import com.brucetoo.activityanimation.small.SmallGallery;
import com.brucetoo.activityanimation.tools.ImageDragger;
import com.brucetoo.activityanimation.tools.ImageInfo;
import com.brucetoo.activityanimation.tools.PhotoView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;
import butterknife.Unbinder;

import static android.view.View.ALPHA;
import static android.view.View.GONE;
import static butterknife.ButterKnife.findById;

public class FullScreenGalleryFragment extends Fragment implements ImageDragger.OnImageDragListener,
        FullScreenGalleryPagerAdapter.OnBackPressedListener, FullScreenGalleryPagerAdapter.OnImageAnimationStartListener {

    public static final String TAG = FullScreenGalleryFragment.class.getSimpleName();
    private static final String EXTRA_ALL_IMAGES = "all_images";
    private static final String EXTRA_CURRENT_IMAGE_POSITION = "current_image_position";
    private static final String EXTRA_CURRENT_IMAGE_INFO = "current_image_info";
    private static final int ALPHA_ANIMATION_DURATION = 300;

    @Nullable
    private SmallGallery smallGallery;

    private boolean isFragmentExiting;

    private Unbinder unbinder;

    @BindView(R.id.mask)
    View mask;

    public static FullScreenGalleryFragment getInstance(List<String> allImages, ImageInfo currentImageInfo,
                                                        int currentImagePosition) {
        FullScreenGalleryFragment fragment = new FullScreenGalleryFragment();
        Bundle arguments = new Bundle();
        arguments.putStringArrayList(EXTRA_ALL_IMAGES, new ArrayList<>(allImages));
        arguments.putParcelable(EXTRA_CURRENT_IMAGE_INFO, currentImageInfo);
        arguments.putInt(EXTRA_CURRENT_IMAGE_POSITION, currentImagePosition);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_full_screen_gallery, container, false);
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        ViewPager viewPager = findById(view, R.id.viewpager);
        Bundle arguments = getArguments();
        List<String> images = arguments.getStringArrayList(EXTRA_ALL_IMAGES);
        ImageInfo imageInfo = arguments.getParcelable(EXTRA_CURRENT_IMAGE_INFO);
        initViewPager(viewPager, images, imageInfo, arguments.getInt(EXTRA_CURRENT_IMAGE_POSITION, 0));
        runEnterAnimation();
    }

    private void initViewPager(ViewPager viewPager, List<String> images, ImageInfo imageInfo, int position) {
        FullScreenGalleryPagerAdapter pagerAdapter = new FullScreenGalleryPagerAdapter(getContext(), images, imageInfo, position);
        pagerAdapter.setOnImageDragListener(this);
        pagerAdapter.setOnBackPressedListener(this);
        pagerAdapter.setOnImageAnimationStartListener(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(position);
    }

    @OnPageChange(R.id.viewpager)
    public void onPageSelected(int position) {
        if (smallGallery != null) {
            smallGallery.setCurrentItem(position);
            smallGallery.hideAllImages();
        }
    }

    private void exitFragment(PhotoView photoView) {
        isFragmentExiting = true;
        runExitAnimation(photoView);

        if (smallGallery != null) {
            photoView.animateTo(smallGallery.getImageInfo((int) photoView.getTag()), this::popFragment);
        }
    }

    private void popFragment() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getSupportFragmentManager().popBackStack();
        }
    }

    private void runEnterAnimation() {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mask, ALPHA, 0, 1);
        alphaAnimator.setDuration(ALPHA_ANIMATION_DURATION);
        alphaAnimator.setInterpolator(new AccelerateInterpolator());
        alphaAnimator.start();
    }

    public void runExitAnimation(final View view) {
        if (mask != null) {
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mask, ALPHA, mask.getAlpha(), 0);
            alphaAnimator.setDuration(ALPHA_ANIMATION_DURATION);
            alphaAnimator.setInterpolator(new AccelerateInterpolator());
            alphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    showHiddenImagesInSmallGallery();
                    view.setVisibility(GONE);
                }
            });
            alphaAnimator.start();
        } else {
            showHiddenImagesInSmallGallery();
        }
    }

    private void showHiddenImagesInSmallGallery() {
        if (smallGallery != null) {
            smallGallery.showAllHiddenImages();
        }
    }

    @Override
    public void onStartDragging() {
        //nop
    }

    @Override
    public void onEndDragging(View view, boolean wasLongDrag) {
        if (wasLongDrag) {
            exitFragment((PhotoView) view);
        }
    }

    @Override
    public void onLongDrag(View view) {
        ObjectAnimator.ofFloat(mask, ALPHA, 1, 0).start();
    }

    @Override
    public void onShortDrag(View view) {
        ObjectAnimator.ofFloat(mask, ALPHA, 0, 1).start();
    }

    @Override
    public void onBackPressed(@NonNull PhotoView photoView) {
        exitFragment(photoView);
    }

    @Override
    public void onImageAnimationStart() {
        if (!isFragmentExiting && smallGallery != null) {
            smallGallery.hideAllImages();
        }
    }

    public void setSmallGallery(@Nullable SmallGallery smallGallery) {
        this.smallGallery = smallGallery;
    }
}
