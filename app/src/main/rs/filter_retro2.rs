#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

static float3 COLOR1 = { 0.393f, 0.769f, 0.189f };
static float3 COLOR2 = { 0.349f, 0.686f, 0.168f };
static float3 COLOR3 = { 0.272f, 0.534f, 0.131f };

void root(uchar4 *v_color){
    float3 color = rsUnpackColor8888(*v_color).rgb;

    float newR = dot(color, COLOR1);
    float newG = dot(color, COLOR2);
    float newB = dot(color, COLOR3);

    newB = ( newB > 1.0 ? 1.0 : (newB < 0.0 ? 0.0 : newB));
    newG = ( newG > 1.0 ? 1.0 : (newG < 0.0 ? 0.0 : newG));
    newR = ( newR > 1.0 ? 1.0 : (newR < 0.0 ? 0.0 : newR));

    color.r = newR;
    color.g = newG;
    color.b = newB;

    color = clamp(color, 0.0f, 1.0f);
    *v_color = rsPackColorTo8888(color);
 }