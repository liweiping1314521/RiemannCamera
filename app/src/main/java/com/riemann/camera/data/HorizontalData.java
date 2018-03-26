package com.riemann.camera.data;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

public class HorizontalData {
    private final String effectName;
    private final Drawable filterDrawable;
    private final String mTopFilter;
    private final Drawable coverDrawable;
    private final int mId;

    public HorizontalData(String name, Drawable filter, String topFilter, Drawable cover, int id) {
        effectName = name;
        filterDrawable = filter;
        mTopFilter = topFilter;
        coverDrawable = cover;
        mId = id;
    }

    public String getEffectName() {
        return effectName;
    }

    public Drawable getFilterDrawable() {
        return filterDrawable;
    }

    public String getTopFilter() {
        return mTopFilter;
    }

    public Drawable  getCoverDrawable() {
        return coverDrawable;
    }

    public int getId(){
        return mId;
    }
}
