#pragma version(1)
#pragma rs java_package_name(com.riemann.camera)

#include "utils.rsh"

static float brightness_value;
static float contrast_value;
static float saturation_value;

static float corner_radius;
static float inv_corner_radius;

static float inv_width;
static float inv_height;

static float sqrt2 = 1.41421356f;

void setBrightness(float value) {
	brightness_value = value;
}

void setContrast(float value) {
	contrast_value = value;
}

void setSaturation(float value) {
	saturation_value = value;
}

void setCornerRadius(float value) {
	corner_radius = value;
	inv_corner_radius = 1.0f / value;
}

void setSize(float width, float height) {
	inv_width = 1.0f / width;
	inv_height = 1.0f / height;
}

void root(uchar4* v_color, uint32_t x, uint32_t y) {

	float3 color = rsUnpackColor8888(*v_color).rgb;

	// Adjust color brightness, contrast and saturation.
	color = brightness(color, brightness_value);
	color = contrast(color, contrast_value);
	color = saturation(color, saturation_value);

	// Calculate darker rounded corners.
	float2 tex_pos;
	tex_pos.x = x * inv_width;
	tex_pos.y = y * inv_height;

	float len = distance(tex_pos, 0.5f) * sqrt2;
	len = (len - 1.0f + corner_radius) * inv_corner_radius;
	len = clamp(len, 0.0f, 1.0f);
	len = len * len * (3.0f - 2.0f * len);
	color *= mix(0.5f, 1.0f, 1.0f - len);

	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}