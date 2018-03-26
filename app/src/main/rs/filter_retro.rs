#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static float3 COLOR1 = { 0.299f, 0.587f, 0.114f };
static float3 COLOR2 = { 0.984f, 0.949f, 0.639f };
static float3 COLOR3 = { 0.909f, 0.396f, 0.702f };
static float3 COLOR4 = { 0.035f, 0.286f, 0.914f };

void root(uchar4 *v_color) {
    float3 color = rsUnpackColor8888(*v_color).rgb;

    float gray = dot(color, COLOR1);
    color = overlay(gray, color, 1.0f);
    color = multiplyWithAlpha(COLOR2, 0.588235f, color);
    color = screenPixelComponent(COLOR3, 0.2f, color);
    color = screenPixelComponent(COLOR4, 0.168627f, color);

    color = clamp(color, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
}