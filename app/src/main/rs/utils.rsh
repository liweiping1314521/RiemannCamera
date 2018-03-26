#include "rs_matrix.rsh"

static float3 brightness(float3 color, float brightness) {
	float scaled = brightness / 2.0;
	if (scaled < 0.0) {
		return color * (1.0f + scaled);
	} else {
		return color + ((1.0f - color) * scaled);
	}
}

static float BlendMultiply(float baseColor, float blendColor) {
    return baseColor * blendColor;
}

static float BlendHardLight(float baseColor, float blendColor) {
    if (blendColor < 0.5) {
        return 2.0 * baseColor * blendColor;
    } else {
        return (1.0 - 2.0 * (1.0 - baseColor) * (1.0 - blendColor));
    }
}

static float BlendOverLay(float baseColor, float blendColor) {
    if(baseColor < 0.5) {
        return 2.0 * baseColor * blendColor;
    }
    else {
        return 1.0 - ( 2.0 * ( 1.0 - baseColor) * ( 1.0 - blendColor));
    }
}

static float BlendScreen(float baseColor, float blendColor) {
    return 1.0 - ((1.0 - baseColor) * (1.0 - blendColor));
}

static float3 contrast(float3 color, float contrast) {
    const float PI_PER_4 = M_PI / 4.0f;
    return min(1.0f, ((color - 0.5f) * (tan((contrast + 1.0f) * PI_PER_4) ) + 0.5f));
}

static float3 overlay(float3 overlayComponent, float3 underlayComponent, float alpha) {
    float3 underlay = underlayComponent * alpha;
    return underlay * (underlay + (2.0f * overlayComponent * (1.0f - underlay)));
}

static float3 multiplyWithAlpha(float3 overlayComponent, float alpha, float3 underlayComponent) {
    return underlayComponent * overlayComponent * alpha;
}

static float3 screenPixelComponent(float3 maskPixelComponent, float alpha, float3 imagePixelComponent) {
    return 1.0f - (1.0f - (maskPixelComponent * alpha)) * (1.0f - imagePixelComponent);
}

static float3 rgbToHsv(float3 color) {
    float3 hsv;

    float mmin = min(color.r, min(color.g, color.b));
    float mmax = max(color.r, max(color.g, color.b));
    float delta = mmax - mmin;

    hsv.z = mmax;
    hsv.y = delta / mmax;

    if (color.r == mmax) {
        hsv.x = (color.g - color.b) / delta;
    } else if (color.g == mmax) {
        hsv.x = 2.0 + (color.b - color.r) / delta;
    } else {
        hsv.x = 4.0 + (color.r - color.g) / delta;
    }

    hsv.x *= 0.166667;
    if (hsv.x < 0.0) {
        hsv.x += 1.0;
    }

    return hsv;
}

static float3 hsvToRgb(float3 hsv) {
    if (hsv.y == 0.0) {
        return hsv.z;
    } else {
        float i;
        float aa, bb, cc, f;

        float h = hsv.x;
        float s = hsv.y;
        float b = hsv.z;

        if (h == 1.0) {
            h = 0.0;
        }

        h *= 6.0;
        i = floor(h);
        f = h - i;
        aa = b * (1.0 - s);
        bb = b * (1.0 - (s * f));
        cc = b * (1.0 - (s * (1.0 - f)));

        float3 rgb;
        if (i == 0) {
            rgb.r = b;
            rgb.g = cc;
            rgb.b = aa;
        }
        if (i == 1) {
            rgb.r = bb;
            rgb.g = b;
            rgb.b = aa;
        }
        if (i == 2) {
            rgb.r = aa;
            rgb.g = b;
            rgb.b = cc;
        }
        if (i == 3) {
            rgb.r = aa;
            rgb.g = bb;
            rgb.b = b;
        }
        if (i == 4) {
            rgb.r = cc;
            rgb.g = aa;
            rgb.b = b;
        }
        if (i == 5) {
            rgb.r = b;
            rgb.g = aa;
            rgb.b = bb;
        }
        return rgb;
    }
}

static float3 saturation(float3 color, float sat) {
    const float lumaR = 0.212671;
    const float lumaG = 0.715160;
    const float lumaB = 0.072169;

    float v = sat + 1.0;
    float i = 1.0 - v;
    float r = i * lumaR;
    float g = i * lumaG;
    float b = i * lumaB;

    rs_matrix3x3 mat = { r + v, r, r, g, g + v, g, b, b, b + v };

    return rsMatrixMultiply(&mat, color);
}
