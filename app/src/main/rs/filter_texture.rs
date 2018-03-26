#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)
#include "utils.rsh"

static uchar4 *photo_color;

void setMutilImage(uchar4 *v_color) {
    photo_color = v_color;
}

void root(const uchar4 *v_color, uchar4 *v_out, uint32_t x, uint32_t y) {
    //unpack a color to a float4
    float4 f4 = rsUnpackColor8888(*v_color);
    float3 baseColor = f4.rgb;

    float3 blendColor = rsUnpackColor8888(*v_out).rgb;

    float r = BlendOverLay(baseColor.r, blendColor.r);
    float g = BlendOverLay(baseColor.g, blendColor.g);
    float b = BlendOverLay(baseColor.b, blendColor.b);

    float3 color;
    color.r = r;
    color.g = g;
    color.b = b;

    color = clamp(color, 0.0f, 1.0f);
    *v_out = rsPackColorTo8888(color);
}