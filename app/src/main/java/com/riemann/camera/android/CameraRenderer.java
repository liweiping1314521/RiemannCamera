package com.riemann.camera.android;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;

import com.riemann.camera.ui.RotateImageView;

import java.io.IOException;
import java.util.List;

/**
 * SDK_INT < 21,使用老的相机接口方案,都继承CameraGLRendererBase
 * 由于摄像头不是我们关注的重点，重点是滤镜和拍照后的图片处理，
 * 5.0以下的有些接口还没有完善，读者可以自行处理
 */
public class CameraRenderer extends CameraGLRendererBase {

    public static final String LOGTAG = "CameraRenderer";

    private Camera mCamera;
    private boolean mPreviewStarted = false;

    CameraRenderer(Context context, CameraGLSurfaceView view) {
        super(context, view);
    }

    /*打开相机，使用老的接口*/
    @Override
    protected synchronized void openCamera(int id) {
        Log.i(LOGTAG, "openCamera");
        closeCamera();
        if (id == Constant.CAMERA_ID_ANY) {
            Log.d(LOGTAG, "Trying to open camera with old open()");
            try {
                mCamera = Camera.open();
            }
            catch (Exception e){
                Log.e(LOGTAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
            }

            if(mCamera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                boolean connected = false;
                for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                    Log.d(LOGTAG, "Trying to open camera with new open(" + camIdx + ")");
                    try {
                        mCamera = Camera.open(camIdx);
                        connected = true;
                    } catch (RuntimeException e) {
                        Log.e(LOGTAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                    }
                    if (connected) break;
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                int localCameraIndex = mCameraIndex;
                if (mCameraIndex == Constant.CAMERA_ID_BACK) {
                    Log.i(LOGTAG, "Trying to open BACK camera");
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Camera.getCameraInfo( camIdx, cameraInfo );
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            localCameraIndex = camIdx;
                            break;
                        }
                    }
                } else if (mCameraIndex == Constant.CAMERA_ID_FRONT) {
                    Log.i(LOGTAG, "Trying to open FRONT camera");
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Camera.getCameraInfo( camIdx, cameraInfo );
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            localCameraIndex = camIdx;
                            break;
                        }
                    }
                }
                if (localCameraIndex == Constant.CAMERA_ID_BACK) {
                    Log.e(LOGTAG, "Back camera not found!");
                } else if (localCameraIndex == Constant.CAMERA_ID_FRONT) {
                    Log.e(LOGTAG, "Front camera not found!");
                } else {
                    Log.d(LOGTAG, "Trying to open camera with new open(" + localCameraIndex + ")");
                    try {
                        mCamera = Camera.open(localCameraIndex);
                    } catch (RuntimeException e) {
                        Log.e(LOGTAG, "Camera #" + localCameraIndex + "failed to open: " + e.getLocalizedMessage());
                    }
                }
            }
        }
        if(mCamera == null) {
            Log.e(LOGTAG, "Error: can't open camera");
            return;
        }
        //设置相机参数，比如对焦等
        Camera.Parameters params = mCamera.getParameters();
        List<String> FocusModes = params.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        mCamera.setParameters(params);

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException ioe) {
            Log.e(LOGTAG, "setPreviewTexture() failed: " + ioe.getMessage());
        }
    }

    /*关闭相机*/
    @Override
    protected synchronized void closeCamera() {
        Log.i(LOGTAG, "closeCamera");
        if(mCamera != null) {
            mCamera.stopPreview();
            mPreviewStarted = false;
            mCamera.release();
            mCamera = null;
        }
    }

    /*设置预览尺寸大小*/
    @Override
    public synchronized void setCameraPreviewSize(int width, int height) {
        Log.i(LOGTAG, "setCameraPreviewSize: "+width+"x"+height);
        if(mCamera == null) {
            Log.e(LOGTAG, "Camera isn't initialized!");
            return;
        }

        if(mMaxCameraWidth  > 0 && mMaxCameraWidth  < width)  width  = mMaxCameraWidth;
        if(mMaxCameraHeight > 0 && mMaxCameraHeight < height) height = mMaxCameraHeight;

        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        int bestWidth = 0, bestHeight = 0;
        if (psize.size() > 0) {
            float aspect = (float)width / height;
            for (Camera.Size size : psize) {
                int w = size.width, h = size.height;
                Log.d(LOGTAG, "checking camera preview size: "+w+"x"+h);
                if ( w <= width && h <= height &&
                        w >= bestWidth && h >= bestHeight &&
                        Math.abs(aspect - (float)w/h) < 0.2 ) {
                    bestWidth = w;
                    bestHeight = h;
                }
            }
            if(bestWidth <= 0 || bestHeight <= 0) {
                bestWidth  = psize.get(0).width;
                bestHeight = psize.get(0).height;
                Log.e(LOGTAG, "Error: best size was not selected, using "+bestWidth+" x "+bestHeight);
            } else {
                Log.i(LOGTAG, "Selected best size: "+bestWidth+" x "+bestHeight);
            }

            if(mPreviewStarted) {
                mCamera.stopPreview();
                mPreviewStarted = false;
            }
            mCameraWidth  = bestWidth;
            mCameraHeight = bestHeight;
            param.setPreviewSize(bestWidth, bestHeight);
        }
        param.set("orientation", "landscape");
        mCamera.setParameters(param);
        mCamera.startPreview();
        mPreviewStarted = true;
    }

    @Override
    protected void setCameraOrientation(int orientation) {
        //Matrix.setRotateM(mSharedData.mOrientationM, 0, orientation, 0f, 0f, 1f);
//        Camera.Size size = mCamera.getParameters().getPreviewSize();
//        if (orientation % 90 == 0) {
//            int w = size.width;
//            size.width = size.height;
//            size.height = w;
//        }
    }

    //由于现在一般的手机都是在5.0上操作，在这里，没有完善5.0之前的接口，
    @Override
    protected void takePhoto() {
    }

    @Override
    protected void setActivity(Activity activity) {
    }

    @Override
    protected void setThumbImageView(RotateImageView thumbImageView, ProgressBar progressBar) {
    }

    @Override
    public void goToCameraGallery() {
    }
}