package com.brucetoo.activityanimation.small;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.brucetoo.activityanimation.tools.ProgressBarRequestListener;
import com.brucetoo.activityanimation.R;
import com.brucetoo.activityanimation.tools.PhotoView;
import com.bumptech.glide.Glide;

import java.util.List;

import static android.widget.ImageView.ScaleType.CENTER_INSIDE;
import static butterknife.ButterKnife.findById;

class SmallGalleryPagerAdapter extends PagerAdapter {

    private final List<String> items;

    public SmallGalleryPagerAdapter(Context context, List<String> items) {
        this.items = items;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.small_gallery_item, container, false);
        PhotoView photoView = findById(view, R.id.photo_view);
        ProgressBar progressBar = findById(view, R.id.progress_bar);
        photoView.setScaleType(CENTER_INSIDE);
        photoView.touchEnable(false);
        photoView.setEnabled(false);
        loadImage(photoView, progressBar, items.get(position));
        photoView.setTag(position);
        container.addView(view);
        return view;
    }

    protected void loadImage(ImageView imageView, ProgressBar progressBar, String photoUrl) {
        Glide.with(imageView.getContext())
                .load(photoUrl)
                .listener(new ProgressBarRequestListener(progressBar) {
                    @Override
                    public void onLoadingEnds() {
                        super.onLoadingEnds();
                        imageView.setEnabled(true);
                    }
                })
                .into(imageView);
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
