#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static float3 COLOR1 = { 0.299f, 0.587f, 0.114f };

rs_allocation inBitmap;

void root(uchar4* v_color, uint32_t x, uint32_t y) {

    float3 color = rsUnpackColor8888(*v_color).rgb;
    uint32_t x_step = x + 5;
    uint32_t y_step = y + 5;

    const uchar4 *element = rsGetElementAt(inBitmap, x_step, y_step);
    float3 pixel_center3 = rsUnpackColor8888(*element).rgb;

    float3 newRgb;
    newRgb.r = color.r - pixel_center3.r + 0.5;
    newRgb.g = color.g - pixel_center3.g + 0.5;
    newRgb.b = color.b - pixel_center3.b + 0.5;

    float gray = dot(newRgb, COLOR1);

    float3 grayRgb = {gray, gray, gray};

    color = clamp(grayRgb, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
}