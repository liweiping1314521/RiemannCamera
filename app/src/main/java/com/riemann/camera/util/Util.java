package com.riemann.camera.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.riemann.camera.R;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {

    public static final int ORIENTATION_HYSTERESIS = 5;

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public static int loadShader(String vss, String fss) {
        //创建一个顶点着色器类型
        int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        //将源码添加到shader并编译之
        GLES20.glShaderSource(vshader, vss);
        GLES20.glCompileShader(vshader);
        int[] status = new int[1];
        //获取顶点着色器编译状态
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e("CameraGLRendererBase", "Could not compile vertex shader: " + GLES20.glGetShaderInfoLog(vshader));
            GLES20.glDeleteShader(vshader);
            vshader = 0;
            return 0;
        }

        //创建一个片段着色器类型
        int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        //将源码添加到shader并编译之
        GLES20.glShaderSource(fshader, fss);
        GLES20.glCompileShader(fshader);
        //获取顶点着色器编译状态
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e("CameraGLRendererBase", "Could not compile fragment shader:" + GLES20.glGetShaderInfoLog(fshader));
            GLES20.glDeleteShader(vshader);
            GLES20.glDeleteShader(fshader);
            fshader = 0;
            return 0;
        }

        //创建一个空的OPENGL ES Program
        int program = GLES20.glCreateProgram();
        //将顶点着色器附着到programe上
        GLES20.glAttachShader(program, vshader);
        //将片段着色器附着到programe上
        GLES20.glAttachShader(program, fshader);
        //创建可执行的的Program
        GLES20.glLinkProgram(program);
        //删除
        GLES20.glDeleteShader(vshader);
        GLES20.glDeleteShader(fshader);
        //获取编译状态
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e("CameraGLRendererBase", "Could not link shader program: "+GLES20.glGetProgramInfoLog(program));
            program = 0;
            return 0;
        }
        GLES20.glValidateProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, status, 0);
        if (status[0] == 0)
        {
            Log.e("CameraGLRendererBase", "Shader program validation error: "+GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
            return 0;
        }

        Log.d("CameraGLRendererBase", "Shader program is built OK");

        return program;
    }

    private static ImageFileNamer sImageFileNamer;

    public static void initialize(Context context) {
        sImageFileNamer = new ImageFileNamer(context.getString(R.string.image_file_name_format));
    }

    public static String createJpegName(long dateTaken) {
        synchronized (sImageFileNamer) {
            return sImageFileNamer.generateName(dateTaken);
        }
    }

    private static class ImageFileNamer {
        private SimpleDateFormat mFormat;
        private long mLastDate;
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            mFormat = new SimpleDateFormat(format);
        }

        public String generateName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = mFormat.format(date);

            if (dateTaken / 1000 == mLastDate / 1000) {
                mSameSecondCount++;
                result += "_" + mSameSecondCount;
            }
            else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(Camera.ACTION_NEW_PICTURE, uri));
        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
    }

    public static void closeSilently(Closeable c) {
        if (c == null)
            return;
        try {
            c.close();
        }
        catch (Throwable t) {
            // do nothing
        }
    }

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        if (uri == null)
            return false;

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Log.d("Util", "Fail to open URI. URI=" + uri);
                return false;
            }
            pfd.close();
        }
        catch (IOException ex) {
            return false;
        }
        return true;
    }

    public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";

    public static void viewUri(Uri uri, Context context) {
        if (!isUriValid(uri, context.getContentResolver())) {
            return;
        }

        final int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        try {
            context.startActivity(new Intent(Util.REVIEW_ACTION, uri).setFlags(flags));
        }
        catch (ActivityNotFoundException ex) {
            try {
                if ("htc".equals((android.os.Build.MANUFACTURER).toLowerCase())) {
                    context.startActivity(new Intent("com.htc.album.action.VIEW_PHOTO_FROM_CAMERA", uri).setFlags(flags));
                }
                else {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, uri).setFlags(flags));
                }
            }
            catch (ActivityNotFoundException e) {
                Log.d("Util", "review image fail. uri=" + uri + e);
            }
        }
    }
}
