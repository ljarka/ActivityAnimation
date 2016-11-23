package com.brucetoo.activityanimation.tools;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public abstract class SimpleRequestListener implements RequestListener<Object, GlideDrawable> {
    @Override
    public boolean onException(Exception e, Object model, Target<GlideDrawable> target, boolean isFirstResource) {
        onLoadingEnds();
        return false;
    }

    @Override
    public boolean onResourceReady(GlideDrawable resource, Object model, Target<GlideDrawable> target,
                                   boolean isFromMemoryCache, boolean isFirstResource) {
        onLoadingEnds();
        return false;
    }

    public abstract void onLoadingEnds();
}
