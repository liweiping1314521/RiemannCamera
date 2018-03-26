package com.riemann.camera.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.riemann.camera.filter.AbstractFilter;
import com.riemann.camera.filter.FilterFactory;
import com.riemann.camera.filter.FilterGroup;
import com.riemann.camera.filter.OESFilter;
import com.riemann.camera.ui.RotateImageView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 显示滤镜的核心类
 */
public abstract class CameraGLRendererBase implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    protected final String TAG = "CameraGLRendererBase";

    protected int mCameraWidth = -1, mCameraHeight = -1;
    protected int mMaxCameraWidth = -1, mMaxCameraHeight = -1;
    protected int mCameraIndex = Constant.CAMERA_ID_ANY;

    protected CameraGLSurfaceView mView;
    /*和SurfaceView不同的是，SurfaceTexture在接收图像流之后，不需要立即显示出来,SurfaceTexture不需要显示到屏幕上，
    因此我们可以用SurfaceTexture接收来自camera的图像流，然后从SurfaceTexture中取得图像帧的拷贝进行处理，
    处理完毕后再送给另一个SurfaceView或者GLSurfaceView用于显示即可*/
    protected SurfaceTexture mSurfaceTexture;

    protected boolean mHaveSurface = false;
    protected boolean mHaveFBO = false;
    protected boolean mUpdateST = false;
    protected boolean mEnabled = true;

    protected boolean mIsStarted = false;
    private Context mContext;

    //滤镜操作类
    private FilterGroup filterGroup;
    //摄像头原始预览数据
    private OESFilter oesFilter;
    //索引下标
    protected int mFilterIndex = 0;

    /*打开相机*/
    protected abstract void openCamera(int id);
    /*关闭相机*/
    protected abstract void closeCamera();
    /*设置预览尺寸大小*/
    protected abstract void setCameraPreviewSize(int width, int height);
    /*设置相机拍摄角度*/
    protected abstract void setCameraOrientation(int orientation);
    /*拍照*/
    protected abstract void takePhoto();
    protected abstract void setActivity(Activity activity);
    /*设置缩略图*/
    protected abstract void setThumbImageView(RotateImageView thumbImageView, ProgressBar progressBar);
    /*跳转到图片浏览界面*/
    protected abstract void goToCameraGallery();

    public CameraGLRendererBase(Context context, CameraGLSurfaceView view) {
        mContext = context;
        mView = view;

        filterGroup = new FilterGroup();
        oesFilter = new OESFilter(context);
        //OES是原始的摄像头数据纹理，然后再添加滤镜纹理
        // N+1个滤镜（其中第一个从外部纹理接收的无滤镜效果）
        filterGroup.addFilter(oesFilter);
        //索引下标为0，表示是原始数据，即滤镜保持原始数据，不做滤镜运算
        filterGroup.addFilter(FilterFactory.createFilter(0, context));
    }

    /**
     * SurfaceTexture.OnFrameAvailableListener 回调接口
     * @param surfaceTexture
     * 正因是RENDERMODE_WHEN_DIRTY所以就要告诉GLSurfaceView什么时候Render，
     * 也就是啥时候进到onDrawFrame()这个函数里。
     * SurfaceTexture.OnFrameAvailableListener这个接口就干了这么一件事，当有数据上来后会进到
     * 这里，然后执行requestRender()。
     * 这里的mView是CameraGLSurfaceView
     */
    @Override
    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mUpdateST = true;
        //有新的数据来了，可以渲染了
        mView.requestRender();
    }

    /**
     * GLSurfaceView.Renderer 回调接口, 初始化
     * @param gl
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        //初始化所有滤镜,一般都是初始化滤镜的顶点着色器和片段着色器
        filterGroup.init();
    }

    /**
     * GLSurfaceView.Renderer 回调接口，比如横竖屏切换
     * @param gl
     * @param surfaceWidth
     * @param surfaceHeight
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int surfaceWidth, int surfaceHeight) {
        Log.i(TAG, "onSurfaceChanged ( " + surfaceWidth + " x " + surfaceHeight + ")");
        mHaveSurface = true;
        //更新surface状态
        updateState();
        //设置预览界面大小
        setPreviewSize(surfaceWidth, surfaceHeight);
        //设置OPENGL视窗大小及位置
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
        //创建滤镜帧缓存的数据
        filterGroup.onFilterChanged(surfaceWidth, surfaceHeight);
    }

    /**
     * GLSurfaceView.Renderer 回调接口, 每帧更新
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mHaveFBO) {
            return;
        }

        synchronized(this) {
            //mUpdateST这个值设置是在每次有新的数据帧上来的时候设置为true，
            //我们需要从图像中提取最近一帧，然后可以设置值为false，每次来新的帧数据调用一次
            if (mUpdateST) {
                //更新纹理图像为从图像流中提取的最近一帧
                mSurfaceTexture.updateTexImage();
                mUpdateST = false;
            }
            //OES是原始的摄像头数据纹理，然后再添加滤镜纹理
            //N+1个滤镜（其中第一个从外部纹理接收的无滤镜效果）
            filterGroup.onDrawFrame(oesFilter.getTextureId());
        }
    }

    /**
     *初始化SurfaceTexture并监听回调
     */
    private void initSurfaceTexture() {
        Log.d(TAG, "initSurfaceTexture");
        deleteSurfaceTexture();
        mSurfaceTexture = new SurfaceTexture(oesFilter.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    /**
     *销毁SurfaceTexture
     */
    private void deleteSurfaceTexture() {
        Log.d(TAG, "deleteSurfaceTexture");
        if(mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    /**
     * 销毁所有滤镜
     */
    public void destory(){
        filterGroup.destroy();
    }

    public synchronized void enableView() {
        Log.d(TAG, "enableView");
        mEnabled = true;
        updateState();
    }

    public synchronized void disableView() {
        Log.d(TAG, "disableView");
        mEnabled = false;
        updateState();
    }

    //更新状态
    private void updateState() {
        Log.d(TAG, "updateState mEnabled = " + mEnabled + ", mHaveSurface = " + mHaveSurface);
        boolean willStart = mEnabled && mHaveSurface && mView.getVisibility() == View.VISIBLE;
        if (willStart != mIsStarted) {
            if(willStart) {
                doStart();
            } else {
                doStop();
            }
        } else {
            Log.d(TAG, "keeping State unchanged");
        }
        Log.d(TAG, "updateState end");
    }

    /**
     * 开启相机预览
     */
    protected synchronized void doStart() {
        Log.d(TAG, "doStart");
        initSurfaceTexture();
        openCamera(mCameraIndex);
        mIsStarted = true;
        if(mCameraWidth > 0 && mCameraHeight > 0) {
            //设置预览高度和高度
            setPreviewSize(mCameraWidth, mCameraHeight);
        }
    }

    protected void doStop() {
        Log.d(TAG, "doStop");
        synchronized(this) {
            mUpdateST = false;
            mIsStarted = false;
            mHaveFBO = false;
            closeCamera();
            deleteSurfaceTexture();
        }
    }

    protected void setPreviewSize(int width, int height) {
        synchronized(this) {
            mHaveFBO = false;
            mCameraWidth  = width;
            mCameraHeight = height;
            setCameraPreviewSize(width, height);
            mHaveFBO = true;
        }
    }

    /**
     *切换摄像头,是前置摄像头预览还是后置摄像头预览
     */
    public void setCameraIndex(int cameraIndex) {
        disableView();
        mCameraIndex = cameraIndex;
        enableView();
        updateVertex(cameraIndex);
    }

    /**
     * 由于前置摄像头和后置摄像头切换后，会出现角度不一样，不同的摄像头纹理坐标不一样，这样达到效果
     * @param cameraIndex
     */
    public void updateVertex(int cameraIndex){
        oesFilter.switchCameraIndex(cameraIndex);
    }

    public void setMaxCameraPreviewSize(int maxWidth, int maxHeight) {
        disableView();
        mMaxCameraWidth  = maxWidth;
        mMaxCameraHeight = maxHeight;
        enableView();
    }

    public void onResume() {
        Log.i(TAG, "onResume");
    }

    public void onPause() {
        Log.i(TAG, "onPause");
        mHaveSurface = false;
        updateState();
        mCameraWidth = mCameraHeight = -1;
    }

    /**
     * 设置角度
     * @param orientation
     */
    public void setOrientation(int orientation){
        setCameraOrientation(orientation);
    }

    /**
     * 切换滤镜,设置滤镜的索引，每个索引对应相应的滤镜
     * @param position
     */
    public void setFilterIndex(int position){
        AbstractFilter filter = FilterFactory.createFilter(position, mContext);
        filter.setWidthHeight(mCameraWidth, mCameraHeight);
        filterGroup.switchFilter(filter);
        mFilterIndex = position;
    }
}