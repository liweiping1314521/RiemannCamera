package com.riemann.camera.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.riemann.camera.R;
import com.riemann.camera.android.Constant;
import com.riemann.camera.util.TextResourceReader;
import com.riemann.camera.util.Util;

public class OESFilter extends AbstractFilter{

    private static final String TAG = "OESFilter";
    private Context mContext;
    private String cameraVs, cameraFs;
    private int progOES = -1;
    private int vPosOES, vTCOES;
    private int[] cameraTexture = null;
    private int mCameraId = Constant.CAMERA_ID_ANY;
    private int mOldCameraId = Constant.CAMERA_ID_ANY;


    public OESFilter(Context context){
        super(TAG);
        mContext = context;
        cameraTexture = new int[1];
    }

    @Override
    public void init() {
        //初始化着色器
        initOESShader();
        //初始化纹理
        loadTexOES();
    }

    /**
     *
     */
    private void initOESShader(){
        //读取顶点做色器
        cameraVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.camera_oes_vs);
        //读取片段着色器
        cameraFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.camera_oes_fs);
        //载入顶点着色器和片段着色器
        progOES = Util.loadShader(cameraVs, cameraFs);
        //获取顶点着色器中attribute location属性vPosition， vTexCoord
        vPosOES = GLES20.glGetAttribLocation(progOES, "vPosition");
        vTCOES  = GLES20.glGetAttribLocation(progOES, "vTexCoord");
        //开启顶点属性数组
        GLES20.glEnableVertexAttribArray(vPosOES);
        GLES20.glEnableVertexAttribArray(vTCOES);
    }

    private void loadTexOES() {
        //生成一个纹理
        GLES20.glGenTextures(1, cameraTexture, 0);
        //绑定纹理,值得注意的是，纹理绑定的目标(target)并不是通常的GL_TEXTURE_2D，而是GL_TEXTURE_EXTERNAL_OES,
        //这是因为Camera使用的输出texture是一种特殊的格式。同样的，在shader中我们也必须使用SamperExternalOES 的变量类型来访问该纹理。
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    public int getTextureId() {
        return cameraTexture[0];
    }

    @Override
    public void onPreDrawElements() {
        super.onPreDrawElements();
        //加载着色器
        GLES20.glUseProgram(progOES);
        //关联属性与顶点数据的数组，告诉OPENGL再缓冲区vert中0的位置读取数据
        GLES20.glVertexAttribPointer(vPosOES, 2, GLES20.GL_FLOAT, false, 4 * 2, vert);
        if (mCameraId == Constant.CAMERA_ID_FRONT) {
            GLES20.glVertexAttribPointer(vTCOES, 2, GLES20.GL_FLOAT, false, 4 * 2, texOESFont);
        } else {
            GLES20.glVertexAttribPointer(vTCOES, 2, GLES20.GL_FLOAT, false, 4 * 2, texOES);
        }
    }

    @Override
    public void onDrawFrame(int textureId) {
        if (mOldCameraId == mCameraId) {
            onPreDrawElements();
            //设置窗口可视区域
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
            //激活纹理，当有多个纹理的时候，可以依次递增GLES20.GL_TEXTUREi
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            //绑定纹理
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture[0]);
            //设置samplerExternalOES"sTexture1"到纹理 unit 0
            GLES20.glUniform1i(GLES20.glGetUniformLocation(progOES, "sTexture"), 0);
            //绘制
            draw();
        }
        mOldCameraId = mCameraId;
    }

    @Override
    public void destroy() {
        GLES20.glDeleteProgram(progOES);
        GLES20.glDeleteTextures(cameraTexture.length, cameraTexture, 0);
    }

    public void switchCameraIndex(int cameraId) {
        mCameraId = cameraId;
    }
}
