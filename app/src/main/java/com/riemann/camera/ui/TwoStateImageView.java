package com.riemann.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TwoStateImageView extends ImageView {
    private final float DISABLED_ALPHA = 0.4f;
    private boolean mFilterEnabled = true;

    public TwoStateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwoStateImageView(Context context) {
        this(context, null);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mFilterEnabled) {
            if (enabled) {
                setAlpha(1.0f);
            }
            else {
                setAlpha(DISABLED_ALPHA);
            }
        }
    }

    public void enableFilter(boolean enabled) {
        mFilterEnabled = enabled;
    }
}
