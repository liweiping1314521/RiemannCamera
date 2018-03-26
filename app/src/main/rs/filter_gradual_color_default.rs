#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

void root(uchar4* v_color, uint32_t x, uint32_t y) {
    float3 color = rsUnpackColor8888(*v_color).rgb;
    color = clamp(color, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
}