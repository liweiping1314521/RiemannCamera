#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static float3 COLOR1 = { 0.299f, 0.587f, 0.114f };

void root(uchar4 *v_color){
    float3 color = rsUnpackColor8888(*v_color).rgb;

    float gray = dot(color, COLOR1);
    if (gray > 0.5f) {
        color = 1.0f - (1.0f - 2.0f * (gray - 0.5f)) * (1.0f - gray);
    } else {
        color = 2.0f * gray * gray;
    }

    color = clamp(color, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
 }