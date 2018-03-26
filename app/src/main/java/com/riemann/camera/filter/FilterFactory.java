package com.riemann.camera.filter;

import android.content.Context;

import com.riemann.camera.R;

/**
 * 滤镜工厂方法
 */
public class FilterFactory {

    public static AbstractFilter createFilter(int index, Context context){
        switch (index){
            case 1:{
                return new ImageRetroFilter(context, index);
            }
            case 2: {
                return new ImageGradualFilter(context, R.drawable.change_rainbow, index);
            }
            case 3: {
                return new ImageGradualFilter(context, R.drawable.change_nosta, index);
            }
            case 4: {
                return new ImageGradualFilter(context, R.drawable.change_pink_blue, index);
            }
            case 5: {
                return new ImageGradualFilter(context, R.drawable.change_light, index);
            }
            case 6:{
                return new ImageGradualFilter(context, R.drawable.change_yellow, index);
            }
            case 7:{
                return new ImageGradualFilter(context, R.drawable.change_cold, index);
            }
            case 8:{
                return new ImageGradualFilter(context, R.drawable.change_four_color, index);
            }
            case 9:{
                return new ImageGradualFilter(context, R.drawable.change_retro, index);
            }
            case 10:{
                return new ImageGradualFilter(context, R.drawable.lomo, index);
            }
            case 11:{
                return new ImageGradualFilter(context, R.drawable.lomo_yellow, index);
            }
            case 12:{
                return new ImageTextureFilter(context, R.drawable.texture_puzzle);
            }
            case 13:{
                return new ImageTextureFilter(context, R.drawable.texture_brown_marble);
            }
            case 14:{
                return new ImageRetroFilter(context, index);
            }
            case 15:{
                return new ImageRetroFilter(context, index);
            }
            case 16:{
                return new ImageStudioFilter(context, 119 / 255f, 60 / 255f, 100 / 255f);
            }
            case 17: {
                return new ImageStudioFilter(context, 24 / 255f, 85 / 255f, 126 / 255f);
            }
            case 18 :{
                return new ImageStudioFilter(context, 46 / 255f, 129 / 255f, 87 / 255f);
            }
            case 19: {
                return new ImageStudioFilter(context, 98 / 255f, 46 / 255f, 128 / 255f);
            }
            case 20:{
                return new ImageStudioFilter(context, 103 / 255f, 118 / 255f, 77 / 255f);
            }
            case 21:{
                return new AnselFilter(context, R.raw.filter_last_vs, R.raw.filter_ansel_fs);
            }
            case 22:{
                return new AnselFilter(context, R.raw.filter_last_vs, R.raw.filter_sepia_fs);
            }
            case 23:{
                return new AnselFilter(context, R.raw.filter_last_vs, R.raw.filter_retro_fs);
            }
            case 24:{
                return new AnselFilter(context, R.raw.filter_last_vs, R.raw.filter_georgia_fs);
            }
            case 25:{
                return new AnselFilter(context, R.raw.filter_last_vs, R.raw.filter_sahara_fs);
            }
            case 26:{
                return new AnselFilter(context, R.raw.filter_last_vs, R.raw.filter_polaroid_fs);
            }
        }
        return new OriginFilter(context);
    }
}
