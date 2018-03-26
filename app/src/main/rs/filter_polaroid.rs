#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static rs_matrix4x4 mat = { 1.438, -0.062, -0.062, 0.0,
		                    -0.122, 1.378, -0.122, 0.0,
		                    -0.016, -0.016, 1.483, 0.0,
		                    -0.03, 0.05, -0.02, 0.0 };

void root(uchar4* v_color) {
	float3 color = rsUnpackColor8888(*v_color).rgb;

	rsMatrixMultiply(&mat, color);

	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}
