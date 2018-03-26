package com.riemann.camera.android;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.riemann.camera.CameraPhotoRS;
import com.riemann.camera.ui.RotateImageView;
import com.riemann.camera.ui.Storage;
import com.riemann.camera.ui.Thumbnail;
import com.riemann.camera.util.Util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.riemann.camera.android.Constant.CAMERA_ID_ANY;
import static com.riemann.camera.android.Constant.CAMERA_ID_BACK;

/**
 * SDK_INT < 21,使用新的相机接口方案,都继承CameraGLRendererBase
 * 采用新的相机方案接口，摄像头类，包括预览，拍照
 */
public class Camera2Renderer extends CameraGLRendererBase {

    private static final String TAG = "Camera2Renderer";
    /*代表系统摄像头。该类的功能类似于早期的Camera类。*/
    private CameraDevice mCameraDevice;
    /*当程序需要预览、拍照时，都需要先通过该类的实例创建Session。而且不管预览还是拍照，
    也都是由该对象的方法进行控制的，其中控制预览的方法为setRepeatingRequest()
    控制拍照的方法为capture()。*/
    private CameraCaptureSession mCaptureSession;
    /*当程序调用setRepeatingRequest()方法进行预览时，或调用capture()方法进行拍照时，都需要传入CameraRequest参数。
    CameraRequest代表了一次捕获请求，用于描述捕获图片的各种参数设置，比如对焦模式、曝光模式*/
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private String mCameraID;
    private Size mPreviewSize = new Size(-1, -1);

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Context mContext;

    private int mState = STATE_PREVIEW;

    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    private int mSensorOrientation;
    private boolean mFlashSupported;
    private ImageReader mImageReader;
    private Activity activity = null;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    //保存滤镜拍照后的照片
    private ImageTakeSaver mImageTakeSaver;
    //拍照后的缩略图显示
    private Thumbnail mThumbnail;
    //显示缩略图的的控件
    private RotateImageView mThumbImageView;
    private ProgressBar mThumbProgressBar;
    //保存到图库中使用的,返回数据库
    private ContentResolver mContentResolver;
    private int orientation;

    /**
     * RenderScirpt渲染脚本
     */
    private CameraPhotoRS mCameraPhotoRS;
    private WorkHandler workHandler;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * ImageReader的回调函数, 其中的onImageAvailable会在照片准备好可以被保存时调用
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver2(Camera2Renderer.this, reader.acquireNextImage(), reader.getWidth(), reader.getHeight()));
        }
    };

    Camera2Renderer(Context context, CameraGLSurfaceView view) {
        super(context, view);
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mCameraPhotoRS = new CameraPhotoRS(mContext);
        workHandler = new WorkHandler(this, Looper.getMainLooper());
    }

    public void setThumbImageView(RotateImageView imgview, ProgressBar bar) {
        mThumbImageView = imgview;
        mThumbProgressBar = bar;
        initThumbnailButton();
    }

    @Override
    protected void doStart() {
        Log.d(TAG, "doStart");
        startBackgroundThread();
        if(mImageTakeSaver == null) {
            mImageTakeSaver = new ImageTakeSaver();
        }
        //调用父类，开启相机预览
        super.doStart();
    }

    @Override
    protected void doStop() {
        Log.d(TAG, "doStop");
        super.doStop();
        stopBackgroundThread();
        if (mImageTakeSaver != null) {
            mImageTakeSaver.finish();
            mImageTakeSaver = null;
        }
        if (mThumbnail != null && !mThumbnail.fromFile()) {
            workHandler.post(new Runnable() {
                @Override
                public void run() {
                    mThumbnail.saveTo(new File(mContext.getFilesDir(), Thumbnail.LAST_THUMB_FILENAME));
                }
            });
        }
    }

    private boolean cacPreviewSize(final int width, final int height) {
        Log.i(TAG, "cacPreviewSize: " + width + "x" + height);
        if(mCameraID == null) {
            Log.e(TAG, "Camera isn't initialized!");
            return false;
        }
        /*摄像头管理器。这是一个全新的系统管理器，专门用于检测系统摄像头、打开系统摄像头。除此之外，
        调用CameraManager的getCameraCharacteristics(String)方法即可获取指定摄像头的相关特性*/
        CameraManager manager = (CameraManager) mView.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            /*摄像头特性。该对象通过CameraManager来获取，用于描述特定摄像头所支持的各种特性*/
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            mImageReader = ImageReader.newInstance(1600, 1200, ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            int bestWidth = 0;
            int bestHeight = 0;
            float aspect = (float) width / height;

            for (Size psize : map.getOutputSizes(SurfaceTexture.class)) {
                //  int w = psize.getWidth();
                //  int h = psize.getHeight();
                int h = psize.getWidth();
                int w = psize.getHeight();
                Log.d(TAG, "trying size: " + w + " x " + h);
                if ( width >= w && height >= h && bestWidth <= w && bestHeight <= h &&
                        Math.abs(aspect - (float) w/h ) < 0.2 ) {
                    bestWidth = w;
                    bestHeight = h;
                }
            }
            Log.i(TAG, "best size: " + bestWidth + "x" + bestHeight + " previewSize w " + mPreviewSize.getWidth() + " h " + mPreviewSize.getHeight());
            if( bestWidth == 0 || bestHeight == 0 || mPreviewSize.getWidth() == bestWidth &&
                    mPreviewSize.getHeight() == bestHeight ) {
                return false;
            } else {
                mPreviewSize = new Size(bestWidth, bestHeight);
                return true;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "cacPreviewSize - Camera Access Exception");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "cacPreviewSize - Illegal Argument Exception");
        } catch (SecurityException e) {
            Log.e(TAG, "cacPreviewSize - Security Exception");
        }
        return false;
    }

    /*打开相机，使用5.0的新接口*/
    @Override
    protected void openCamera(int id) {
        Log.i(TAG, "openCamera");
        /*摄像头管理器。这是一个全新的系统管理器，专门用于检测系统摄像头、打开系统摄像头。除此之外，
        调用CameraManager的getCameraCharacteristics(String)方法即可获取指定摄像头的相关特性*/
        CameraManager manager = (CameraManager) mView.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String camList[] = manager.getCameraIdList();
            if(camList.length == 0) {
                Log.e(TAG, "Error: camera isn't detected.");
                return;
            }

            if(id == Constant.CAMERA_ID_ANY) {
                mCameraID = camList[0];
            } else {
                for (String cameraID : camList) {
                    /*摄像头特性。该对象通过CameraManager来获取，用于描述特定摄像头所支持的各种特性*/
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                    if( id == CAMERA_ID_BACK && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK ||
                            id == Constant.CAMERA_ID_FRONT && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraID = cameraID;
                        break;
                    }
                }
            }

            if(mCameraID != null) {
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException(
                            "Time out waiting to lock camera opening.");
                }
                Log.i(TAG, "Opening camera: " + mCameraID);
                /*调用manager.openCamera的接口来打开相机,然后进入mStateCallback的回调*/
                manager.openCamera(mCameraID, mStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "OpenCamera - Camera Access Exception");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "OpenCamera - Illegal Argument Exception");
        } catch (SecurityException e) {
            Log.e(TAG, "OpenCamera - Security Exception");
        } catch (InterruptedException e) {
            Log.e(TAG, "OpenCamera - Interrupted Exception");
        }
    }

    /*关闭相机*/
    @Override
    protected void closeCamera() {
        Log.i(TAG, "closeCamera");
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        /**
         * onOpened回调 进入createCameraPreviewSession
         * @param cameraDevice
         */
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            mCameraOpenCloseLock.release();
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
            mCameraOpenCloseLock.release();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
            mCameraOpenCloseLock.release();
        }

    };

    private void createCameraPreviewSession() {
        int w = mPreviewSize.getWidth();
        int h = mPreviewSize.getHeight();
        Log.i(TAG, "createCameraPreviewSession(" + w + "x" + h + ")");
        if(w < 0 || h < 0) {
            return;
        }
        try {
            /*设置默认的图像缓冲区大小*/
            mSurfaceTexture.setDefaultBufferSize(w, h);

            Surface surface = new Surface(mSurfaceTexture);

            if (null == mCameraDevice) {
                mCameraOpenCloseLock.release();
                Log.e(TAG, "createCameraPreviewSession: camera isn't opened");
                return;
            }

            // 预览请求构建
            mPreviewRequestBuilder = mCameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            if (mImageReader == null) {
                return;
            }
            // 创建预览的捕获会话
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured( CameraCaptureSession cameraCaptureSession) {
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // 自动对焦
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // 自动闪光
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                // 构建上述的请求
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                // 重复进行上面构建的请求, 以便显示预览
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                                Log.i(TAG, "CameraPreviewSession has been started");
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "createCaptureSession failed");
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "createCameraPreviewSession failed");
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreviewSession");
        }
    }

    private void startBackgroundThread() {
        Log.i(TAG, "startBackgroundThread");
        stopBackgroundThread();
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        Log.i(TAG, "stopBackgroundThread");
        if(mBackgroundThread == null)
            return;
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "stopBackgroundThread");
        }
    }

    /*设置预览尺寸大小*/
    @Override
    protected void setCameraPreviewSize(int width, int height) {
        Log.i(TAG, "setCameraPreviewSize ( " + width + " x " + height + ")");
        if(mMaxCameraWidth  > 0 && mMaxCameraWidth  < width){
            width  = mMaxCameraWidth;
        }
        if(mMaxCameraHeight > 0 && mMaxCameraHeight < height){
            height = mMaxCameraHeight;
        }

        boolean needReconfig = cacPreviewSize(width, height);

        mCameraWidth  = mPreviewSize.getWidth();
        mCameraHeight = mPreviewSize.getHeight();

        if (null != mCaptureSession) {
            Log.d(TAG, "closing existing previewSession");
            mCaptureSession.close();
            mCaptureSession = null;
        }
        createCameraPreviewSession();
    }

    /*设置预览尺寸大小*/
    @Override
    protected void setCameraOrientation(int orientation) {
        this.orientation = orientation;
    }

    /*拍照*/
    @Override
    protected void takePhoto() {
        mThumbProgressBar.setVisibility(View.VISIBLE);
        lockFocus();
    }

    @Override
    protected void setActivity(Activity activity) {
        this.activity = activity;
    }

    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private void showToast(final String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else /*if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState)*/ {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }
    };

    public void initThumbnailButton() {
        mThumbnail = Thumbnail.loadFrom(new File(mContext.getFilesDir(), Thumbnail.LAST_THUMB_FILENAME));
        updateThumbnailButton();
    }

    private void updateThumbnailButton() {
        if ((mThumbnail == null || !Util.isUriValid(mThumbnail.getUri(), mContentResolver))) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mThumbnail = Thumbnail.getLastThumbnail(mContentResolver);
                }
            }).start();
        }
        if (mThumbnail != null) {
            mThumbImageView.setBitmap(mThumbnail.getBitmap());
        }
        else {
            mThumbImageView.setBitmap(null);
        }
    }

    @Override
    public void goToCameraGallery() {
        if (mThumbnail != null) {
            Util.viewUri(mThumbnail.getUri(), mContext);
        }
    }

    private static class SaveRequest {
        byte[] data;
        int width, height;
        long dateTaken;
        int previewWidth;
        int orientation;
        int datalength;
    }

    private static final int UPDATE_THUMBNAIL = 7;
    private static class WorkHandler extends Handler {

        private Camera2Renderer camera2Renderer;
        public WorkHandler(Camera2Renderer camera2Renderer, Looper looper) {
            super(looper);
            this.camera2Renderer = camera2Renderer;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_THUMBNAIL: {
                    camera2Renderer.mImageTakeSaver.updateThumbnail();
                    camera2Renderer.mThumbProgressBar.setVisibility(View.GONE);
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };

    /**
     * 保存拍照后的图片,然后保存函数里面，用RS渲染脚本来处理图片
     */
    private class ImageTakeSaver extends Thread {
        private static final int QUEUE_LIMIT = 3;

        private ArrayList<SaveRequest> mQueue;
        private Thumbnail mPendingThumbnail;
        private Object mUpdateThumbnailLock = new Object();
        private boolean mStop;

        // Runs in main thread
        public ImageTakeSaver() {
            mQueue = new ArrayList<SaveRequest>();
            start();
        }

        // Runs in main thread
        public void addImage(final byte[] data, int width, int height, int orientation, int datalength) {
            SaveRequest r = new SaveRequest();
            r.data = data;
            r.width = width;
            r.height = height;
            r.dateTaken = System.currentTimeMillis();
            r.orientation = orientation;
            r.datalength = datalength;
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                r.previewWidth = mCameraHeight;
            } else {
                r.previewWidth = mCameraWidth;
            }
            synchronized (this) {
                while (mQueue.size() >= QUEUE_LIMIT) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                    }
                }
                mQueue.add(r);
                notifyAll();  // Tell saver thread there is new work to do.
            }
        }

        @Override
        public void run() {
            while (true) {
                SaveRequest r;
                synchronized (this) {
                    if (mQueue.isEmpty()) {
                        notifyAll();

                        if (mStop) break;

                        try {
                            wait();
                        } catch (InterruptedException ex) {
                        }
                        continue;
                    }
                    r = mQueue.get(0);
                }
                storeImage(r.data, r.width, r.height, r.dateTaken, r.previewWidth, r.orientation, r.datalength);
                synchronized(this) {
                    mQueue.remove(0);
                    notifyAll();
                }
            }
        }

        // Runs in main thread
        public void waitDone() {
            synchronized (this) {
                while (!mQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                }
            }
            updateThumbnail();
        }

        // Runs in main thread
        public void finish() {
            waitDone();
            synchronized (this) {
                mStop = true;
                notifyAll();
            }
            try {
                join();
            } catch (InterruptedException ex) {
                // ignore.
            }
        }

        // Runs in main thread (because we need to update mThumbnailView in the
        // main thread)
        public void updateThumbnail() {
            Thumbnail t;
            synchronized (mUpdateThumbnailLock) {
                workHandler.removeMessages(UPDATE_THUMBNAIL);
                t = mPendingThumbnail;
                mPendingThumbnail = null;
            }

            if (t != null) {
                mThumbnail = t;
                mThumbImageView.setBitmap(mThumbnail.getBitmap());
            }
        }

        // Runs in saver thread
        private void storeImage(final byte[] data, int width, int height, long dateTaken, int previewWidth, int orientation, int datalength) {
            String title = "riemann_" + Util.createJpegName(dateTaken);

            Uri uri = Storage.addImage(mCameraPhotoRS, mContentResolver, title, dateTaken, orientation, data, width, height, datalength, mFilterIndex);
            if (uri != null) {
                boolean needThumbnail;
                synchronized (this) {
                    // If the number of requests in the queue (include the
                    // current one) is greater than 1, we don't need to generate
                    // thumbnail for this image. Because we'll soon replace it
                    // with the thumbnail for some image later in the queue.
                    needThumbnail = (mQueue.size() <= 1);
                }
                if (needThumbnail) {
                    // Create a thumbnail whose width is equal or bigger than
                    // that of the preview.
                    int ratio = (int) Math.ceil((double) width / previewWidth);
                    int inSampleSize = Integer.highestOneBit(ratio);
                    Thumbnail t = Thumbnail.createThumbnail(Storage.generateFilepath(title), orientation, inSampleSize, uri);
                    synchronized (mUpdateThumbnailLock) {
                        // We need to update the thumbnail in the main thread,
                        // so send a message to run updateThumbnail().
                        mPendingThumbnail = t;
                        workHandler.sendEmptyMessage(UPDATE_THUMBNAIL);
                    }
                }
                Util.broadcastNewPicture(mContext, uri);
            }
        }
    }

    /**
     * 保存照片
     */
    private static class ImageSaver2 implements Runnable {

        private final Image mImage;
        private Camera2Renderer renderer;
        private final int width;
        private final int height;

        public ImageSaver2(Camera2Renderer renderer, Image image, int width, int height) {
            mImage = image;
            this.renderer = renderer;
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            int orientation = 0;
            if (renderer.mCameraIndex == CAMERA_ID_BACK || renderer.mCameraIndex == CAMERA_ID_ANY) {
                orientation = renderer.orientation + 90;
            } else {
                if(renderer.orientation == 0 || renderer.orientation == 180) {
                    orientation = renderer.orientation - 90;
                } else {
                    orientation = renderer.orientation + 90;
                }
            }
            int length = bytes.length;
            renderer.mImageTakeSaver.addImage(bytes, width, height, orientation, length);

            mImage.close();
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
