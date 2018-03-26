package com.riemann.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.riemann.camera.R;
import com.riemann.camera.util.TextResourceReader;
import com.riemann.camera.util.Util;

public class ImageRetroFilter extends AbstractFilter{

    private static final String TAG = "ImageGradualFilter";
    private Context mContext;
    private int[] mTexture = new int[1];
    private String filterVs, filterFs;
    private int prog2D = -1;
    private int vPos2D, vTC2D;
    private int index;

    public ImageRetroFilter(Context context, int index) {
        super(TAG);
        mContext = context;
        this.index = index;
    }

    @Override
    public void init() {
        //初始化着色器
        initShader();
    }

    private void initShader(){
        if (index == 1) {
            //滤镜灰色
            filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
            filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_gray_fs);
        } else if (index == 14) {
            filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
            filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_retro2_fs);
        } else if(index == 15) {
            filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
            filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_carv_fs);
        }
        //载入顶点着色器和片段着色器
        prog2D  = Util.loadShader(filterVs, filterFs);
        //获取顶点着色器中attribute location属性vPosition， vTexCoord
        vPos2D = GLES20.glGetAttribLocation(prog2D, "vPosition");
        vTC2D  = GLES20.glGetAttribLocation(prog2D, "vTexCoord");
        //开启顶点属性数组
        GLES20.glEnableVertexAttribArray(vPos2D);
        GLES20.glEnableVertexAttribArray(vTC2D);
    }

    @Override
    public void onPreDrawElements() {
        super.onPreDrawElements();
        //使用Program
        GLES20.glUseProgram(prog2D);
        //关联属性与顶点数据的数组，告诉OPENGL再缓冲区vert中0的位置读取数据，找到aPosition2D对应的位置，2表示两个分量x，y
        GLES20.glVertexAttribPointer(vPos2D, 2, GLES20.GL_FLOAT, false, 4 * 2, vert);
        GLES20.glVertexAttribPointer(vTC2D,  2, GLES20.GL_FLOAT, false, 4 * 2, tex2D);
    }

    @Override
    public void onDrawFrame(int textureId) {
        onPreDrawElements();
        //设置窗口可视区域
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
        //激活纹理，当有多个纹理的时候，可以依次递增GLES20.GL_TEXTURE
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理，textureId为FBO处理完毕后的内部纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        //设置sampler2D"sTexture1"到纹理 unit 0
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog2D, "sTexture"), 0);

        draw();
    }

    @Override
    public void destroy() {
        //销毁
        GLES20.glDeleteProgram(prog2D);
        GLES20.glDeleteTextures(mTexture.length, mTexture, 0);
    }
}
