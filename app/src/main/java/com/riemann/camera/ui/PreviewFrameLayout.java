package com.riemann.camera.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class PreviewFrameLayout extends RelativeLayout {
    private static final String TAG = "PreviewFrameLayout";
    private DisplayMetrics displayMetrics;

    public interface OnSizeChangedListener {
        public void onSizeChanged();
    }

    private double mAspectRatio;

    public PreviewFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAspectRatio(4.0 / 3.0);
        displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
    }

    public void setAspectRatio(double ratio) {
        if (ratio <= 0.0)
            throw new IllegalArgumentException();

        if (((Activity) getContext()).getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ratio = 1 / ratio;
        }

        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
        }
    }

    public void showBorder(boolean enabled) {
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        final int width = MeasureSpec.getSize(widthSpec);
        final int height = MeasureSpec.getSize(heightSpec);

        Log.i(TAG, " width = " + width + " height = " + height);

        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingBottom() + getPaddingTop();
        
        //LogUtil.log(TAG, " horizontalPadding = " + horizontalPadding + " verticalPadding = " + verticalPadding);

        int previewHeight = height - verticalPadding;
        int previewWidth = width - horizontalPadding;

        // resize frame and preview for aspect ratio
        //if (previewWidth > previewHeight * mAspectRatio) {
        //previewWidth = (int) (previewHeight * mAspectRatio + .5);
        //} else {
        //previewHeight = (int) (previewWidth / mAspectRatio + .5);
        //}

        Log.i(TAG, " previewWidth = " + previewWidth + " previewHeight = " +previewHeight);

        int frameWidth = previewWidth + horizontalPadding;
        int frameHeight = previewHeight + verticalPadding;
        
        super.onMeasure(MeasureSpec.makeMeasureSpec(frameWidth, MeasureSpec.getMode(widthSpec)), MeasureSpec.makeMeasureSpec(frameHeight, MeasureSpec.getMode(heightSpec)));
    }
}
