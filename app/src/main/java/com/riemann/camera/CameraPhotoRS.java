package com.riemann.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlend;
import android.util.Log;

public class CameraPhotoRS {

    private ScriptC_filter_gary mFilterGary;
    private ScriptC_filter_ansel mFilterAnsel;
    private ScriptC_filter_default mFilterDefault;
    private ScriptC_filter_georgia mFilterGeorgia;
    private ScriptC_filter_polaroid mFilterPolaroid;
    private ScriptC_filter_retro mFilterRetro;
    private ScriptC_filter_sahara mFilterSahara;
    private ScriptC_filter_sepia mFilterSepia;
    private ScriptC_filter_gradual_color mFilterGradualColor;
    private ScriptC_filter_gradual_color_default mFilterGradualColorDefault;
    private ScriptC_filter_lomo mFilterLomo;
    private ScriptC_filter_lomo_yellow mFilterLomoYellow;
    private ScriptC_filter_texture mFilterTexture;
    private ScriptC_filter_retro2 mFilterRetro2;
    private ScriptC_filter_studio mFilterStudio;
    private ScriptIntrinsicBlend scriptIntrinsicBlend;
    private ScriptC_filter_carv mFilterCarv;

    private RenderScript mRS;
    private Context mContext;

    public CameraPhotoRS(Context context){
        mContext = context;
        mRS = RenderScript.create(context);

        mFilterGary = new ScriptC_filter_gary(mRS);
        mFilterAnsel = new ScriptC_filter_ansel(mRS);
        mFilterSepia = new ScriptC_filter_sepia(mRS);
        mFilterRetro = new ScriptC_filter_retro(mRS);
        mFilterGeorgia = new ScriptC_filter_georgia(mRS);
        mFilterSahara = new ScriptC_filter_sahara(mRS);
        mFilterPolaroid = new ScriptC_filter_polaroid(mRS);

        mFilterDefault = new ScriptC_filter_default(mRS);
        mFilterGradualColor = new ScriptC_filter_gradual_color(mRS);
        mFilterGradualColorDefault = new ScriptC_filter_gradual_color_default(mRS);

        mFilterLomo = new ScriptC_filter_lomo(mRS);
        mFilterLomoYellow = new ScriptC_filter_lomo_yellow(mRS);

        mFilterTexture = new ScriptC_filter_texture(mRS);
        mFilterRetro2 = new ScriptC_filter_retro2(mRS);
        mFilterStudio = new ScriptC_filter_studio(mRS);

        scriptIntrinsicBlend = ScriptIntrinsicBlend.create(mRS, Element.U8_4(mRS));
        mFilterCarv = new ScriptC_filter_carv(mRS);
    }

    public void applyFilter(Bitmap bitmapIn, int index) {
        Allocation inAllocation = Allocation.createFromBitmap(mRS, bitmapIn,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

        switch (index) {
            case 1: {
                mFilterGary.forEach_root(inAllocation);
                break;
            }
            case 2: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_rainbow);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                //scriptIntrinsicBlend.forEachDstIn(inAllocation, allocationCutter);
                //scriptIntrinsicBlend.forEachSrcAtop(allocationCutter, inAllocation);
                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 3: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_nosta);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 4: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_pink_blue);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 5: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_light);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 6: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_yellow);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 7: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_cold);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 8: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_four_color);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 9: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.change_retro);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterGradualColor.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 10: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.lomo);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterLomo.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 11: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.lomo_yellow);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

                mFilterLomoYellow.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 12: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.texture_puzzle);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
                mFilterTexture.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 13: {
                Bitmap mBitmapCutter = getCutterBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(), R.drawable.texture_brown_marble);

                Allocation allocationCutter = Allocation.createFromBitmap(mRS, mBitmapCutter,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
                mFilterTexture.forEach_root(allocationCutter, inAllocation);
                allocationCutter.copyTo(mBitmapCutter);
                allocationCutter.destroy();

                break;
            }
            case 14: {
                mFilterRetro2.forEach_root(inAllocation);
                break;
            }
            case 15: {
                mFilterCarv.set_inBitmap(inAllocation);
                mFilterCarv.forEach_root(inAllocation);
                break;
            }
            case 16: {
                mFilterStudio.invoke_setRColor(119 / 255f);
                mFilterStudio.invoke_setGColor(60 / 255f);
                mFilterStudio.invoke_setBColor(100 / 255f);
                mFilterStudio.forEach_root(inAllocation);
                break;
            }
            case 17: {
                mFilterStudio.invoke_setRColor(24 / 255f);
                mFilterStudio.invoke_setGColor(85 / 255f);
                mFilterStudio.invoke_setBColor(126 / 255f);
                mFilterStudio.forEach_root(inAllocation);
                break;
            }
            case 18: {
                mFilterStudio.invoke_setRColor(46 / 255f);
                mFilterStudio.invoke_setGColor(129 / 255f);
                mFilterStudio.invoke_setBColor(87 / 255f);
                mFilterStudio.forEach_root(inAllocation);
                break;
            }
            case 19: {
                mFilterStudio.invoke_setRColor(98 / 255f);
                mFilterStudio.invoke_setGColor(46 / 255f);
                mFilterStudio.invoke_setBColor(128 / 255f);
                mFilterStudio.forEach_root(inAllocation);
                break;
            }
            case 20: {
                mFilterStudio.invoke_setRColor(103 / 255f);
                mFilterStudio.invoke_setGColor(118 / 255f);
                mFilterStudio.invoke_setBColor(77 / 255f);
                mFilterStudio.forEach_root(inAllocation);
                break;
            }
            case 21:
                mFilterAnsel.forEach_root(inAllocation);
                setDefaultRoot(bitmapIn, inAllocation);
                break;
            case 22:
                mFilterSepia.forEach_root(inAllocation);
                setDefaultRoot(bitmapIn, inAllocation);
                break;
            case 23:
                mFilterRetro.forEach_root(inAllocation);
                setDefaultRoot(bitmapIn, inAllocation);
                break;
            case 24:
                mFilterGeorgia.forEach_root(inAllocation);
                setDefaultRoot(bitmapIn, inAllocation);
                break;
            case 25:
                mFilterSahara.forEach_root(inAllocation);
                setDefaultRoot(bitmapIn, inAllocation);
                break;
            case 26:
                mFilterPolaroid.forEach_root(inAllocation);
                setDefaultRoot(bitmapIn, inAllocation);
                break;
        }

        // Copy inAllocation values back to Bitmap.
        inAllocation.copyTo(bitmapIn);
        inAllocation.destroy();
        mRS.destroy();
    }

    private void setDefaultRoot(Bitmap bitmap, Allocation allocation) {
        mFilterDefault.invoke_setBrightness(0);
        mFilterDefault.invoke_setContrast(0);
        mFilterDefault.invoke_setSaturation(3 / 10f);
        mFilterDefault.invoke_setCornerRadius(3 / 10f);
        mFilterDefault.invoke_setSize(bitmap.getWidth(), bitmap.getHeight());
        mFilterDefault.forEach_root(allocation);
    }

    private Bitmap getCutterBitmap(int width, int height, int id) {
        Bitmap mutilBitmap = BitmapFactory.decodeResource(mContext.getResources(), id);
        Matrix matrix = new Matrix();
        matrix.postRotate(180);
        Bitmap rotateBitmap = Bitmap.createBitmap(mutilBitmap, 0, 0, mutilBitmap.getWidth(), mutilBitmap.getHeight(), matrix, true);

        Bitmap bitmapCutter = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Rect baseRect = new Rect(0, 0, width, height);
        Canvas canvas = new Canvas(bitmapCutter);
        canvas.drawBitmap(rotateBitmap, null, baseRect, null);
        canvas.save();
        canvas.restore();

        return bitmapCutter;
    }
}










