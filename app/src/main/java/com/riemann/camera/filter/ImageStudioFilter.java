package com.riemann.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.riemann.camera.R;
import com.riemann.camera.util.TextResourceReader;
import com.riemann.camera.util.Util;

public class ImageStudioFilter extends AbstractFilter{

    private static final String TAG = "ImageGradualFilter";
    private Context mContext;
    private int[] mTexture = new int[1];
    private String filterVs, filterFs;
    private int prog2D = -1;
    private int vPos2D, vTC2D;
    private int iRColor, iGColor, iBColor;
    private final float r_Color, g_Color, b_Color;

    public ImageStudioFilter(Context context, float r_Color, float g_Color, float b_Color) {
        super(TAG);
        mContext = context;
        this.r_Color = r_Color;
        this.g_Color = g_Color;
        this.b_Color = b_Color;
    }

    @Override
    public void init() {
        initShader(r_Color, g_Color, b_Color);
    }

    private void initShader(float r, float g, float b){
        filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
        filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_studio_fs);

        prog2D  = Util.loadShader(filterVs, filterFs);
        vPos2D = GLES20.glGetAttribLocation(prog2D, "vPosition");
        vTC2D  = GLES20.glGetAttribLocation(prog2D, "vTexCoord");
        GLES20.glEnableVertexAttribArray(vPos2D);
        GLES20.glEnableVertexAttribArray(vTC2D);

        iRColor = GLES20.glGetUniformLocation(prog2D, "r_color");
        iGColor = GLES20.glGetUniformLocation(prog2D, "g_color");
        iBColor = GLES20.glGetUniformLocation(prog2D, "b_color");
    }

    @Override
    public void onPreDrawElements() {
        super.onPreDrawElements();
        GLES20.glUseProgram(prog2D);
        GLES20.glVertexAttribPointer(vPos2D, 2, GLES20.GL_FLOAT, false, 4 * 2, vert);
        GLES20.glVertexAttribPointer(vTC2D,  2, GLES20.GL_FLOAT, false, 4 * 2, tex2D);

        GLES20.glUniform1f(iRColor, r_Color);
        GLES20.glUniform1f(iGColor, g_Color);
        GLES20.glUniform1f(iBColor, b_Color);
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
        GLES20.glDeleteTextures(mTexture.length, mTexture, 0);
    }
}
