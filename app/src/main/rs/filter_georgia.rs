#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static float3 COLOR_MULT = { 0.981, 0.862, 0.686 };

void root(uchar4 *v_color) {
    float3 color = rsUnpackColor8888(*v_color).rgb;

    color = brightness(color, 0.4724f);
    color = contrast(color, 0.3149f);

    color.g = color.g * 0.87f + 0.13f;
    color.b = color.b * 0.439f + 0.561f;

    color *= COLOR_MULT;

    color = clamp(color, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
}
