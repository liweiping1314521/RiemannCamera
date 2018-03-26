package com.riemann.camera.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.riemann.camera.util.TextResourceReader;
import com.riemann.camera.util.Util;

public class AnselFilter extends AbstractFilter{

    private static final String TAG = "AnselFilter";
    private Context mContext;
    private String filterVs, filterFs;
    private int filterProgram = -1;
    private int uAspectRatio2D, uAspectRatioPreview2D, aPosition2D;
    private int uBrightness, uContrast, uSaturation, uCornerRadius;
    private final float mAspectRatio[] = new float[2];
    private final int vs, fs;

    /**
     *
     * @param context
     * @param vs         顶点点着色器
     * @param fs         片段着色器
     */
    public AnselFilter(Context context, int vs, int fs){
        super(TAG);
        mContext = context;
        this.vs = vs;
        this.fs = fs;
    }

    @Override
    public void init() {
        //初始化着色器
        initShade();
    }

    @Override
    public void destroy() {
        GLES20.glDeleteProgram(filterProgram);
    }

    private void initShade(){
        //读取顶点着色器字段
        filterVs = TextResourceReader.readTextFileFromResource(mContext, vs);
        //读取片段着色器字段
        filterFs = TextResourceReader.readTextFileFromResource(mContext, fs);
        //载入顶点着色器和片段着色器
        filterProgram = Util.loadShader(filterVs, filterFs);
        //glGetUniformLocation获取顶点着色器中uniform字段uAspectRatio uniform对象可以在顶点着色器和片段着色器之间共享
        uAspectRatio2D = GLES20.glGetUniformLocation(filterProgram, "uAspectRatio");
        //glGetUniformLocation获取顶点着色器中uniform字段uAspectRatioPreview
        uAspectRatioPreview2D = GLES20.glGetUniformLocation(filterProgram, "uAspectRatioPreview");
        //glGetAttribLocation获取顶点着色器中attribute字段aPosition，attribute对象只能在顶点着色器中使用
        aPosition2D = GLES20.glGetAttribLocation(filterProgram, "aPosition");
        //开启顶点属性数组
        GLES20.glEnableVertexAttribArray(aPosition2D);

        //获取片段着色器中uniform字段字段uBrightness uContrast uSaturation uCornerRadius
        uBrightness = GLES20.glGetUniformLocation(filterProgram, "uBrightness");
        uContrast = GLES20.glGetUniformLocation(filterProgram, "uContrast");
        uSaturation = GLES20.glGetUniformLocation(filterProgram, "uSaturation");
        uCornerRadius = GLES20.glGetUniformLocation(filterProgram, "uCornerRadius");
    }

    @Override
    public void onPreDrawElements() {
        //清屏
        super.onPreDrawElements();
        //使用Program
        GLES20.glUseProgram(filterProgram);
        //uAspectRatio2D为locationlocation属性，指明要更改uniform变量的位置；
        //第二个参数count 1指明更改元素个数；第三个参数为更改的值
        GLES20.glUniform2fv(uAspectRatio2D, 1, mAspectRatio, 0);
        //uAspectRatioPreview2D为location属性，指明要更改uniform变量的位置；
        //第二个参数count 1指明更改元素个数；第三个参数为更改的值
        GLES20.glUniform2fv(uAspectRatioPreview2D, 1, mAspectRatio, 0);
        //uBrightness为location属性，第二个参数为元素的传递的值，以此类推
        GLES20.glUniform1f(uBrightness, 0);
        GLES20.glUniform1f(uContrast, 0);
        GLES20.glUniform1f(uSaturation, 3 / 10f);
        GLES20.glUniform1f(uCornerRadius, 3 / 10f);
        //关联属性与顶点数据的数组，告诉OPENGL再缓冲区vert中0的位置读取数据，找到aPosition2D对应的位置，2表示两个分量x，y
        GLES20.glVertexAttribPointer(aPosition2D, 2, GLES20.GL_FLOAT, false, 4 * 2, vert);
    }

    @Override
    public void onFilterChanged(int surfaceWidth, int surfaceHeight) {
        super.onFilterChanged(surfaceWidth, surfaceHeight);
        mAspectRatio[0] = (float) Math.min(surfaceWidth, surfaceHeight) / surfaceWidth;
        mAspectRatio[1] = (float) Math.min(surfaceWidth, surfaceHeight) / surfaceHeight;
    }

    @Override
    public void onDrawFrame(int textureId) {
        //赋值准备工作
        onPreDrawElements();
        //设置窗口可视区域
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
        //激活纹理，当有多个纹理的时候，可以依次递增GLES20.GL_TEXTUREi
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        //sTexture为location属性，设置它的值
        //uniform sampler2D sTexture;通过glGetUniformLocation获取到location
        //通过赋值，可以指定Sample与纹理单元之间的关系，想让sample对哪个纹理单元GLES20.GL_TEXTUREi
        //中纹理处理就赋值i，这里是GLES20.GL_TEXTURE0，就赋值0，依次类推
        GLES20.glUniform1i(GLES20.glGetUniformLocation(filterProgram, "sTexture"), 0);
        draw();
    }
}
