/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brucetoo.activityanimation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.brucetoo.activityanimation.fullscreen.FullScreenFragmentUtil;
import com.brucetoo.activityanimation.fullscreen.FullScreenGalleryFragment;
import com.brucetoo.activityanimation.small.SmallGalleryView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> imgList = new ArrayList<>();
    private SmallGalleryView smallGalleryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgList.add(0, "http://www.hdwallpapery.com/static/images/free-wallpaper-download1_YGS0iqi.jpg");
        imgList.add(1, "http://www.hdwallpapery.com/static/images/free-wallpaper-3_RctdU7Y.jpg");
        imgList.add(2, "http://www.hdwallpapery.com/static/images/814410-free-wallpaper.jpg");

        smallGalleryView = (SmallGalleryView) findViewById(R.id.small_gallery_view);
        smallGalleryView.setImages(imgList);
        setupGalleriesInteraction();
    }

    private void setupGalleriesInteraction() {
        smallGalleryView.setOnItemClickListener((currentItemPosition, currentItemImageInfo) -> {

            FullScreenGalleryFragment fullScreenGallery = FullScreenGalleryFragment
                    .getInstance(imgList, currentItemImageInfo,
                            currentItemPosition);
            fullScreenGallery.setSmallGallery(smallGalleryView);
            FullScreenFragmentUtil.showFullScreenFragment(MainActivity.this, fullScreenGallery, FullScreenGalleryFragment.TAG);
        });

        setupExistingFullScreenGalleryFragment();
    }

    private void setupExistingFullScreenGalleryFragment() {
        FullScreenGalleryFragment fullScreenGallery = (FullScreenGalleryFragment) getSupportFragmentManager()
                .findFragmentByTag(FullScreenGalleryFragment.TAG);

        if (fullScreenGallery != null) {
            fullScreenGallery.setSmallGallery(smallGalleryView);
            smallGalleryView.hideAllImages();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        FullScreenGalleryFragment fullScreenGallery = (FullScreenGalleryFragment) getSupportFragmentManager()
                .findFragmentByTag(FullScreenGalleryFragment.TAG);
        fullScreenGallery.setSmallGallery(null);
    }
}
