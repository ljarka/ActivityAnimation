package com.brucetoo.activityanimation.tools;

import android.support.annotation.CallSuper;
import android.widget.ProgressBar;

import static android.view.View.GONE;
import static com.brucetoo.activityanimation.tools.Preconditions.checkNotNull;

public class ProgressBarRequestListener extends SimpleRequestListener {

    private final ProgressBar progressBar;

    public ProgressBarRequestListener(ProgressBar progressBar) {
        this.progressBar = checkNotNull(progressBar);
    }

    @CallSuper
    @Override
    public void onLoadingEnds() {
        progressBar.setVisibility(GONE);
    }
}
