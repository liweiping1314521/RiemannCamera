package com.riemann.camera.ui;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

public class ViewAnimation extends Animation {
    int mCenterX;
    int mCenterY;
    Camera camera = new Camera();

    public ViewAnimation()
    {
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);
        mCenterX = width/2;
        mCenterY = height/2;
        setDuration(500);
        setFillAfter(true);
        setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t)
    {
        final Matrix matrix = t.getMatrix();
        camera.save();
        //camera.translate(0.0f, 0.0f, (13000 - 13000.0f * interpolatedTime));
        camera.rotateY(180 * interpolatedTime);
        camera.getMatrix(matrix);
        matrix.preTranslate(-mCenterX, -mCenterY);
        matrix.postTranslate(mCenterX, mCenterY);
        camera.restore();
    }
}
