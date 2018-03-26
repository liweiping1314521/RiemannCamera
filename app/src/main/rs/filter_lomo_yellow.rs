#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)
#include "utils.rsh"

void root(const uchar4 *v_color, uchar4 *v_out, uint32_t x, uint32_t y) {
    //unpack a color to a float4
    float4 f4 = rsUnpackColor8888(*v_color);
    float3 color1 = f4.rgb;

    float3 color2 = rsUnpackColor8888(*v_out).rgb;

    float r = BlendMultiply(color2.r, color1.r);
    float g = BlendMultiply(color2.g, color1.g);
    float b;
    if (color2.b < 0.2) {
        b = 0.0;
    } else {
        b = BlendMultiply(color2.b - 0.2, color1.b);
    }

    float3 color;
    color.r = r;
    color.g = g;
    color.b = b;

    color = clamp(color, 0.0f, 1.0f);
    *v_out = rsPackColorTo8888(color);
}