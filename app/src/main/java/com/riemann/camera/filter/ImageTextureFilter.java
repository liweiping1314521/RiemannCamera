package com.riemann.camera.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.riemann.camera.R;
import com.riemann.camera.util.TextResourceReader;
import com.riemann.camera.util.Util;

public class ImageTextureFilter extends AbstractFilter{

    private static final String TAG = "ImageGradualFilter";
    private Context mContext;
    private int[] mTexture = new int[1];
    private String filterVs, filterFs;
    private int prog2D = -1;
    private int vPos2D, vTC2D;
    private final int resId;

    public ImageTextureFilter(Context context, int resId) {
        super(TAG);
        mContext = context;
        this.resId = resId;
    }

    @Override
    public void init() {
        initShader(resId);
    }

    private void initShader(int resId){
        genTexture(resId);
        filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
        filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_texture_fs);
        prog2D  = Util.loadShader(filterVs, filterFs);
        vPos2D = GLES20.glGetAttribLocation(prog2D, "vPosition");
        vTC2D  = GLES20.glGetAttribLocation(prog2D, "vTexCoord");
        GLES20.glEnableVertexAttribArray(vPos2D);
        GLES20.glEnableVertexAttribArray(vTC2D);
    }

    @Override
    public void onPreDrawElements() {
        super.onPreDrawElements();
        GLES20.glUseProgram(prog2D);
        GLES20.glVertexAttribPointer(vPos2D, 2, GLES20.GL_FLOAT, false, 4 * 2, vert);
        GLES20.glVertexAttribPointer(vTC2D,  2, GLES20.GL_FLOAT, false, 4 * 2, tex2D);
    }

    @Override
    public void onDrawFrame(int textureId) {
        onPreDrawElements();
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog2D, "sTexture1"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog2D, "sTexture2"), 1);

        draw();
    }

    @Override
    public void destroy() {
        GLES20.glDeleteProgram(prog2D);
        GLES20.glDeleteTextures(mTexture.length, mTexture, 0);
    }

    private void genTexture(int resId) {
        //生成纹理
        GLES20.glGenTextures(1, mTexture, 0);
        //加载Bitmap

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
        if (bitmap != null) {
            //如果bitmap加载成功，则生成此bitmap的纹理映射
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
            //设置纹理映射的属性
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            //生成纹理映射
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            //释放bitmap资源
            bitmap.recycle();
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
    }
}