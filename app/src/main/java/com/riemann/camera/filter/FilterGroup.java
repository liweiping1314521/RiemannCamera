package com.riemann.camera.filter;

import android.opengl.GLES20;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有滤镜的操作类，所有的滤镜会在这里添加，比如，切换滤镜，添加滤镜
 */
public class FilterGroup extends AbstractFilter{

    private static final String TAG = "FilterGroup";
    //所有滤镜会保存在这个链表中
    protected List<AbstractFilter> filters;
    private int[] FBO = null;
    private int[] texture = null;
    protected boolean isRunning;

    public FilterGroup() {
        super(TAG);
        filters = new ArrayList<>();
    }

    public void addFilter(final AbstractFilter filter){
        if (filter == null) {
            return;
        }

        if (!isRunning){
            filters.add(filter);
        } else {
            addPreDrawTask(new Runnable() {
                @Override
                public void run() {
                    //由于执行runnable是在onDrawFrame中运行，当切换滤镜后，必须先初始化滤镜，然后添加到滤镜链表，
                    //再调用filterchange创建帧缓冲，bind纹理
                    filter.init();
                    filters.add(filter);
                    onFilterChanged(surfaceWidth, surfaceHeight);
                }
            });
        }
    }

    /**
     * 切换滤镜，切换滤镜的过程是这样的：
     * 1.当摄像头没有运行的时候，直接添加；
     * 2.当摄像头在运行的时候，先销毁最末尾的的滤镜，然后添加新的滤镜，并告知滤镜变化了，
     *   帧缓存的数据必须也要做相应的调整
     * @param filter
     */
    public void switchFilter(final AbstractFilter filter){
        if (filter == null) {
            return;
        }
        if (!isRunning){
            if(filters.size() > 0) {
                filters.remove(filters.size() - 1).destroy();
            }
            filters.add(filter);
        } else {
            addPreDrawTask(new Runnable() {
                @Override
                public void run() {
                    if (filters.size() > 0) {
                        filters.remove(filters.size() - 1).destroy();
                    }
                    //由于执行runnable是在onDrawFrame中运行，当切换滤镜后，必须先初始化滤镜，然后添加到滤镜链表，
                    //再调用filterchange创建帧缓冲，bind纹理
                    filter.init();
                    filters.add(filter);
                    onFilterChanged(surfaceWidth, surfaceHeight);
                }
            });
        }
    }

    public void clearAllFilter(){

        if (!isRunning){
            if(filters.size() > 0) {
                for (int i = 0; i < filters.size(); i++){
                    filters.remove(i).destroy();
                }
            }
        } else {
            addPreDrawTask(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < filters.size(); i++){
                        filters.remove(i).destroy();
                    }
                }
            });
        }
    }

    @Override
    public void init() {
        //GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        //遍历链表中的滤镜，然后初始化
        for (AbstractFilter filter : filters) {
            filter.init();
        }
        isRunning = true;
    }

    @Override
    public void onDrawFrame(int textureId) {
        //从链表中取出filter然后运行,在切换滤镜的时候运行，执行完后链表长度为0
        runPreDrawTasks();

        if (FBO == null || texture == null) {
            return ;
        }
        int size = filters.size();
        //oes无滤镜效果的纹理
        int previousTexture = textureId;
        for (int i = 0; i < size; i++) {
            AbstractFilter filter = filters.get(i);
            Log.d(TAG, "onDrawFrame: " + i + " / " + size + " "
                    + filter.getClass().getSimpleName() + " "
                    + filter.surfaceWidth + " " + filter.surfaceHeight);
            if (i < size - 1) {
                //先draw oesfilter中无滤镜效果的纹理,SurfaceTexture属于GL_TEXTURE_EXTERNAL_OES纹理
                //注意OpengES FBO 把GL_TEXTURE_EXTERNAL_OES转换为GL_TEXTURE_2D，即OES外部纹理转化为了GL_TEXTURE_2D内部纹理，
                //然后多个GL_TEXTURE_2D纹理叠加达到滤镜效果
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBO[i]);
                filter.onDrawFrame(previousTexture);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                //下一个滤镜的新的纹理id即上一个滤镜的缓冲区对象所对应的纹理id
                previousTexture = texture[i];
            } else {
                //draw滤镜纹理
                filter.onDrawFrame(previousTexture);
            }
        }
    }

    @Override
    public void destroy() {
        //deleteFBO();
        for (AbstractFilter filter : filters) {
            filter.destroy();
        }
        isRunning = false;
    }

    /**
     * 创建帧缓冲，bind数据
     * 创建帧缓冲对象：(目前，帧缓冲对象N为1)
     * 有N+1个滤镜（其中第一个从外部纹理接收的无滤镜效果），就需要分配N个帧缓冲对象，
     * 首先创建大小为N的两个数组mFrameBuffers和mFrameBufferTextures，分别用来存储缓冲区id和纹理id，
     * 通过GLES20.glGenFramebuffers(1, mFrameBuffers, i)来创建帧缓冲对象
     *
     * 对于SurfaceTexture，它是一个GL_TEXTURE_EXTERNAL_OES外部纹理，要想渲染相机预览到GL_TEXTURE_2D纹理上，
     * 唯一办法是采用帧缓冲FBO对象，可以将预览图像的外部纹理渲染到FBO的纹理中，
     * 剩下的滤镜再绑定到该纹理，这样的达到滤镜实现目的
     *
     * @param surfaceWidth
     * @param surfaceHeight
     */
    @Override
    public void onFilterChanged(int surfaceWidth, int surfaceHeight) {
        super.onFilterChanged(surfaceWidth, surfaceHeight);
        //由于相机滤镜就是OES原始数据+滤镜效果组成的，所以这个size永远是等于2的
        int size = filters.size();
        for (int i = 0; i < size; i++){
            filters.get(i).onFilterChanged(surfaceWidth, surfaceHeight);
        }

        if (FBO != null) {
            //如果帧缓存存在先前数据，先清除帧缓冲
            deleteFBO();
        }

        if (FBO == null) {
            FBO = new int[size - 1];
            texture = new int[size - 1];
            /**
             * 依次绘制:
             * 首先第一个一定是绘制与SurfaceTexture绑定的外部纹理处理后的无滤镜效果，之后的操作与第一个一样，都是绘制到纹理。
             * 首先与之前相同传入纹理id，并重新绑定到对应的缓冲区对象GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i])，
             * 之后draw对应的纹理id。若不是最后一个滤镜，需要解绑缓冲区，下一个滤镜的新的纹理id即上一个滤镜的缓冲区对象所对应的纹理id，
             * 同样执行上述步骤，直到最后一个滤镜。
             */
            for (int i = 0; i < size - 1; i++) {
                //创建帧缓冲对象
                GLES20.glGenFramebuffers(1, FBO, i);
                //创建纹理,当把一个纹理附着到FBO上后，所有的渲染操作就会写入到该纹理上，意味着所有的渲染操作会被存储到纹理图像上，
                //这样做的好处是显而易见的，我们可以在着色器中使用这个纹理。
                GLES20.glGenTextures(1, texture, i);
                //bind纹理
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[i]);
                //创建输出纹理，方法基本相同，不同之处在于glTexImage2D最后一个参数为null，不指定数据指针。
                //使用了glTexImage2D函数，使用GLUtils#texImage2D函数加载一幅2D图像作为纹理对象，
                //这里的glTexImage2D稍显复杂，这里重要的是最后一个参数，
                //如果为null就会自动分配可以容纳相应宽高的纹理，然后后续的渲染操作就会存储到这个纹理上了。
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, surfaceWidth, surfaceHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                //指定纹理格式
                //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

                //绑定帧缓冲区,第一个参数是target，指的是你要把FBO与哪种帧缓冲区进行绑定,此时创建的帧缓冲对象其实只是一个“空壳”，
                //它上面还包含一些附着，因此接下来还必须往它里面添加至少一个附着才可以,
                // 使用创建的帧缓冲必须至少添加一个附着点（颜色、深度、模板缓冲）并且至少有一个颜色附着点。
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBO[i]);
                /**
                 * 函数将2D纹理附着到帧缓冲对象
                 * glFramebufferTexture2D()把一幅纹理图像关联到一个FBO,第二个参数是关联纹理图像的关联点,一个帧缓冲区对象可以有多个颜色关联点0~n
                 * 第三个参数textureTarget在多数情况下是GL_TEXTURE_2D。第四个参数是纹理对象的ID号
                 * 最后一个参数是要被关联的纹理的mipmap等级 如果参数textureId被设置为0，那么纹理图像将会被从FBO分离
                 * 如果纹理对象在依然关联在FBO上时被删除，那么纹理对象将会自动从当前帮的FBO上分离。然而，如果它被关联到多个FBO上然后被删除，
                 * 那么它将只被从绑定的FBO上分离，而不会被从其他非绑定的FBO上分离。
                 */
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture[i], 0);
                //现在已经完成了纹理的加载，不需要再绑定此纹理了解绑纹理对象
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                //解绑帧缓冲对象
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
        Log.d(TAG, "initFBO error status: " + GLES20.glGetError());

        //在完成所有附着的添加后，需要使用函数glCheckFramebufferStatus函数检查帧缓冲区是否完整
        int FBOstatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (FBOstatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "initFBO failed, status: " + FBOstatus);
        }
    }

    private void deleteFBO() {
        if (texture != null) {
            //删除纹理
            GLES20.glDeleteTextures(texture.length, texture, 0);
            texture = null;
        }

        if (FBO != null) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            //删除Frame Buffer
            GLES20.glDeleteFramebuffers(FBO.length, FBO, 0);
            FBO = null;
        }
    }
}
