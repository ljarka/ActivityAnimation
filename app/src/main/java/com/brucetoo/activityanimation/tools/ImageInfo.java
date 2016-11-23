package com.brucetoo.activityanimation.tools;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

public class ImageInfo implements Parcelable {
    RectF rect = new RectF();
    RectF localRect = new RectF();
    RectF imageRect = new RectF();
    RectF widgetRect = new RectF();
    RectF clipRect = new RectF();
    float scale;
    ImageView.ScaleType scaleType;

    public ImageInfo(RectF rect, RectF local, RectF img, RectF widget, RectF clipRect,
                     float scale, ImageView.ScaleType scaleType) {
        this.rect.set(rect);
        this.localRect.set(local);
        this.imageRect.set(img);
        this.widgetRect.set(widget);
        this.scale = scale;
        this.scaleType = scaleType;
        this.clipRect = clipRect;
    }

    protected ImageInfo(Parcel in) {
        rect = in.readParcelable(RectF.class.getClassLoader());
        localRect = in.readParcelable(RectF.class.getClassLoader());
        imageRect = in.readParcelable(RectF.class.getClassLoader());
        widgetRect = in.readParcelable(RectF.class.getClassLoader());
        clipRect = in.readParcelable(RectF.class.getClassLoader());
        scale = in.readFloat();
    }

    public static final Creator<ImageInfo> CREATOR = new Creator<ImageInfo>() {
        @Override
        public ImageInfo createFromParcel(Parcel in) {
            return new ImageInfo(in);
        }

        @Override
        public ImageInfo[] newArray(int size) {
            return new ImageInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(rect, flags);
        dest.writeParcelable(localRect, flags);
        dest.writeParcelable(imageRect, flags);
        dest.writeParcelable(widgetRect, flags);
        dest.writeParcelable(clipRect, flags);
        dest.writeFloat(scale);
    }
}