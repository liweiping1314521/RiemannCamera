#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static float3 COLOR1 = { 1.0f, 0.891f, 0.733f };

void root(uchar4 *v_color){
    float3 color = rsUnpackColor8888(*v_color).rgb;

    color.r = color.r * 0.843 + 0.157;
    color.b = color.b * 0.882 + 0.118;

    float3 hsv = rgbToHsv(color);
    hsv.y = hsv.y * 0.55f;
    color = hsvToRgb(hsv);

    color = saturation(color, 0.65f);
    color *= COLOR1;

    color = clamp(color, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
}