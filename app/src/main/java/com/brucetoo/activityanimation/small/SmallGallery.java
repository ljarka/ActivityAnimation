package com.brucetoo.activityanimation.small;

import android.support.annotation.NonNull;

import com.brucetoo.activityanimation.tools.ImageInfo;

public interface SmallGallery {

    @NonNull
    ImageInfo getImageInfo(int pageIndex);

    void setCurrentItem(int pageIndex);

    void hideAllImages();

    void showAllHiddenImages();
}
