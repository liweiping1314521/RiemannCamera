package com.riemann.camera.filter;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * 抽象公共类，滤镜用到的所有数据结构都在这里完成
 *
 * opengl作为本地系统库，运行在本地环境,应用层的JAVA代码运行在Dalvik虚拟机上面
 * android应用层的代码运行环境和opengl运行的环境不同,如何通信呢？
 * 一是通过NDK去调用OPENGL接口，二是通过JAVA层封装好的类直接使用OPENGL接口，实际上它也是一个NDK，
 * 但是使用这些接口就必须用到JAVA层中特殊的类，比如FloatBuffer
 * 它为我们分配OPENGL环境中所使用的本地内存块，而不是使用JAVA虚拟机中的内存，因为OPGNGL不是运行在JAVA虚拟机中的
 *
 */
public abstract class AbstractFilter {

    private static final String TAG = "AbstractFilter";
    private String filterTag;
    protected int surfaceWidth, surfaceHeight;
    protected FloatBuffer vert, texOES, tex2D, texOESFont;
    //顶点着色器使用
    protected final float vertices[] = {
            -1, -1,
            -1,  1,
            1, -1,
            1,  1 };
    //片段着色器纹理坐标 OES 后置相机
    protected final float texCoordOES[] = {
            1,  1,
            0,  1,
            1,  0,
            0,  0 };
    //片段着色器纹理坐标
    private final float texCoord2D[] = {
            0,  0,
            0,  1,
            1,  0,
            1,  1 };
    //片段着色器纹理坐标 前置相机
    private final float texCoordOESFont[] = {
            0,  1,
            1,  1,
            0,  0,
            1,  0 };

    private final LinkedList<Runnable> mPreDrawTaskList;

    public AbstractFilter(String filterTag){
        this.filterTag = filterTag;

        mPreDrawTaskList = new LinkedList<>();

        int bytes = vertices.length * Float.SIZE / Byte.SIZE;
        //allocateDirect分配本地内存,order按照本地字节序组织内容,asFloatBuffer我们不想操作单独的字节，而是想操作浮点数
        vert   = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texOES = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer();
        tex2D  = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texOESFont = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer();
        //put数据，将实际顶点坐标传入buffer，position将游标置为0，否则会从最后一次put的下一个位置读取
        vert.put(vertices).position(0);
        texOES.put(texCoordOES).position(0);
        tex2D.put(texCoord2D).position(0);
        texOESFont.put(texCoordOESFont).position(0);

        String strGLVersion = GLES20.glGetString(GLES20.GL_VERSION);
        if (strGLVersion != null) {
            Log.i(TAG, "OpenGL ES version: " + strGLVersion);
        }
    }

    public void onPreDrawElements(){
        //清除颜色
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        //清除屏幕
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    public void onFilterChanged(int surfaceWidth, int surfaceHeight){
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public void setWidthHeight(int width , int height ) {
        this.surfaceWidth = width;
        this.surfaceHeight = height;
    }

    protected void draw(){
        //使用顶点索引法来绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
    }

    abstract public void init();

    abstract public void onDrawFrame(final int textureId);

    abstract public void destroy();

    //从链表中取出，由于链表里面保存的都是一个个runnable，即取出来运行起来
    public void runPreDrawTasks() {
        while (!mPreDrawTaskList.isEmpty()) {
            mPreDrawTaskList.removeFirst().run();
        }
    }

    //添加要执行的runnable到链表
    public void addPreDrawTask(final Runnable runnable) {
        synchronized (mPreDrawTaskList) {
            mPreDrawTaskList.addLast(runnable);
        }
    }
}
