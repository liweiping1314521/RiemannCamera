package com.riemann.camera.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ProgressBar;

import com.riemann.camera.R;
import com.riemann.camera.ui.RotateImageView;

/**
 * 我们使用GLSurfaceView来显示Camera中预览的数据，所有的滤镜的处理，都是利用OPENGL，然后渲染到GLSurfaceView上
 * 继承至SurfaceView，它内嵌的surface专门负责OpenGL渲染,绘制功能由GLSurfaceView.Renderer完成
 */
public class CameraGLSurfaceView extends GLSurfaceView {

    private static final String LOGTAG = "CameraGLSurfaceView";

    //渲染器
    private CameraGLRendererBase mRenderer;

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray styledAttrs = getContext().obtainStyledAttributes(attrs, R.styleable.CameraBridgeViewBase);
        int cameraIndex = styledAttrs.getInt(R.styleable.CameraBridgeViewBase_camera_id, -1);
        styledAttrs.recycle();

        if(android.os.Build.VERSION.SDK_INT >= 21) {
            mRenderer = new Camera2Renderer(context, this);
        } else {
            mRenderer = new CameraRenderer(context, this);
        }

        setEGLContextClientVersion(2);
        //设置渲染器
        setRenderer(mRenderer);
        /**
         * RENDERMODE_CONTINUOUSLY模式就会一直Render，如果设置成RENDERMODE_WHEN_DIRTY，
         * 就是当有数据时才rendered或者主动调用了GLSurfaceView的requestRender.默认是连续模式，
         * 很显然Camera适合脏模式，一秒30帧，当有数据来时再渲染,RENDERMODE_WHEN_DIRTY时只有在
         * 创建和调用requestRender()时才会刷新
         * 这样不会让CPU一直处于高速运转状态，提高手机电池使用时间和软件整体性能
         */
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * 设置前置或者后置摄像头
     * @param cameraIndex
     */
    public void setCameraIndex(int cameraIndex) {
        mRenderer.setCameraIndex(cameraIndex);
    }

    /**
     * 获取摄像头ID
     * @return
     */
    public int getCameraIndex(){
        return mRenderer.mCameraIndex;
    }

    public void setMaxCameraPreviewSize(int maxWidth, int maxHeight) {
        mRenderer.setMaxCameraPreviewSize(maxWidth, maxHeight);
    }

    /**
     * 设置摄像头的滤镜ID
     * @param position
     */
    public void setFilterIndex(int position){
        mRenderer.setFilterIndex(position);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mRenderer.mHaveSurface = false;
        super.surfaceDestroyed(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void onResume() {
        Log.i(LOGTAG, "onResume");
        super.onResume();
        mRenderer.onResume();
    }

    @Override
    public void onPause() {
        Log.i(LOGTAG, "onPause");
        mRenderer.onPause();
        super.onPause();
    }

    /**
     * 渲染器enable
     */
    public void enableView() {
        mRenderer.enableView();
    }

    /**
     * 渲染器disable
     */
    public void disableView() {
        mRenderer.disableView();
    }

    /**
     * 销毁渲染器
     */
    public void onDestory(){
        mRenderer.destory();
    }

    /**
     * 拍照时设置角度
     * @param orientation
     */
    public void setOrientation(int orientation){
        mRenderer.setOrientation(orientation);
    }

    public void takePhoto(){
        mRenderer.takePhoto();
    }

    public void setActivity(Activity activity) {
        mRenderer.setActivity(activity);
    }

    public void setThumbImageView(RotateImageView thumbImageView, ProgressBar progressBar) {
        mRenderer.setThumbImageView(thumbImageView, progressBar);
    }

    public void goToCameraGallery(){
        mRenderer.goToCameraGallery();
    }
}