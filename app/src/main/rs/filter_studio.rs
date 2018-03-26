#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static float r_color;
static float g_color;
static float b_color;

void setRColor(float value) {
    r_color = value;
}

void setGColor(float value) {
    g_color = value;
}

void setBColor(float value) {
    b_color = value;
}

void root(uchar4 *v_color){
    float3 baseColor = rsUnpackColor8888(*v_color).rgb;

    float temp = 0.392 * baseColor.r + 0.608 * BlendScreen(baseColor.r, baseColor.r);
    float r = 0.176 * temp + 0.824 * BlendOverLay(temp, r_color);

    temp = 0.392 * baseColor.g + 0.608 * BlendScreen(baseColor.g, baseColor.g);
    float g = 0.176 * temp + 0.824 * BlendOverLay(temp, g_color);

    temp = 0.392 * baseColor.b + 0.608 * BlendScreen(baseColor.b, baseColor.b);
    float b = 0.176 * temp + 0.824 * BlendOverLay(temp, b_color);

    float3 color;
    color.r = r;
    color.g = g;
    color.b = b;

    baseColor = clamp(color, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(baseColor);
}