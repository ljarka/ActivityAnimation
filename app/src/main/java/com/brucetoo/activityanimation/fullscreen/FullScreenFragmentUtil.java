package com.brucetoo.activityanimation.fullscreen;

import android.support.annotation.AnimRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

public class FullScreenFragmentUtil {

    private FullScreenFragmentUtil() {
        // no instances
    }

    public static void showFullScreenFragment(AppCompatActivity activity, Fragment fragment, String tag) {
        showFullScreenFragmentWithAnimation(activity, fragment, tag, 0, 0);
    }

    private static void showFullScreenFragmentWithAnimation(AppCompatActivity activity, Fragment fragment, String tag,
                                                            @AnimRes int enterAnimation, @AnimRes int exitAnimation) {
        try {
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(enterAnimation, exitAnimation)
                    .add(Window.ID_ANDROID_CONTENT, fragment, tag)
                    .addToBackStack(null)
                    .commit();
        } catch (IllegalStateException exception) {
            // commit after onSaveInstanceState
            // we just catch exception because we don't want commit with state loss
        }
    }
}
