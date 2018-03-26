#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

static float3 COLOR1 = { 0.299f, 0.587f, 0.114f };

void root(uchar4 *v_color){
    float3 color = rsUnpackColor8888(*v_color).rgb;

    float gray = dot(color, COLOR1);

    float3 grayRgb = {gray, gray, gray};

    color = clamp(grayRgb, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
}