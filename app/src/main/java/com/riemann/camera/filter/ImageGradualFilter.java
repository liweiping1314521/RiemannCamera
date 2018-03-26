package com.riemann.camera.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.riemann.camera.R;
import com.riemann.camera.util.TextResourceReader;
import com.riemann.camera.util.Util;

/**
 * ImageGradualFilter滤镜为指定的图片作为纹理与摄像头纹理做强光算法处理
 */
public class ImageGradualFilter extends AbstractFilter{

    private static final String TAG = "ImageGradualFilter";
    private Context mContext;
    private int[] mTexture = new int[1];
    private String filterVs, filterFs;
    private int prog2D = -1;
    private int vPos2D, vTC2D;
    private final int resId;
    private final int index;

    /**
     * 这个滤镜是两张图片的纹理叠加，具体叠加算法见顶点着色器和片段着色器
     * @param context
     * @param resId     纹理图片资源id
     * @param index     滤镜索引
     */
    public ImageGradualFilter(Context context, int resId, int index) {
        super(TAG);
        mContext = context;
        this.resId = resId;
        this.index = index;
    }

    @Override
    public void init() {
        //初始化着色器
        initShader(resId);
    }

    private void initShader(int resId){
        //根据资源id生成纹理
        genTexture(resId);
        if (index <= 9) {
            //读取顶点着色器字段
            filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
            //读取片段着色器字段
            filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_gradual_fs);
        } else if (index == 10) {
            filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
            filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_lomo_fs);
        } else if (index == 11) {
            filterVs = TextResourceReader.readTextFileFromResource(mContext, R.raw.origin_vs);
            filterFs = TextResourceReader.readTextFileFromResource(mContext, R.raw.filter_lomo_yellow_fs);
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
    public void destroy() {
        GLES20.glDeleteProgram(prog2D);
        GLES20.glDeleteTextures(mTexture.length, mTexture, 0);
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
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog2D, "sTexture1"), 0);
        //绑定纹理，mTexture[0]为加载的纹理图片，两个纹理做叠加
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        //设置sampler2D"sTexture1"到纹理 unit 1
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog2D, "sTexture2"), 1);

        draw();
    }

    /**
     * 通过资源id获取bitmap，然后转化为纹理
     * @param resId
     */
    private void genTexture(int resId) {
        //生成纹理
        GLES20.glGenTextures(1, mTexture, 0);
        //加载Bitmap

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
        if (bitmap != null) {
            //glBindTexture允许我们向GLES20.GL_TEXTURE_2D绑定一张纹理
            //当把一张纹理绑定到一个目标上时，之前对这个目标的绑定就会失效
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
            //设置纹理映射的属性
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //如果bitmap加载成功，则生成此bitmap的纹理映射
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            //释放bitmap资源
            bitmap.recycle();
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
    }
}
