package com.riemann.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.riemann.camera.R;
import com.riemann.camera.util.TextResourceReader;
import com.riemann.camera.util.Util;

public class OriginFilter extends AbstractFilter{

    private static final String TAG = "OESFilter";
    private Context mContext;
    private String originVs, originFs;
    private int prog2D = -1;
    private int vPos2D, vTC2D;

    public OriginFilter(Context context){
        super(TAG);
        mContext = context;
    }

    @Override
    public void init() {
        initOriginShade();
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
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog2D, "sTexture"), 0);
        draw();
    }

    @Override
    public void destroy() {
        GLES20.glDeleteProgram(prog2D);
    }

    private void initOriginShade(){
        originVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
        originFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_fs);
        prog2D  = Util.loadShader(originVs, originFs);
        vPos2D = GLES20.glGetAttribLocation(prog2D, "vPosition");
        vTC2D  = GLES20.glGetAttribLocation(prog2D, "vTexCoord");
        GLES20.glEnableVertexAttribArray(vPos2D);
        GLES20.glEnableVertexAttribArray(vTC2D);
    }
}
