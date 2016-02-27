#pragma optionNV(strict on)

#ifdef GL_ES
    precision mediump float;
#endif

/*
Based on martinsh's (Martins Upitis) Depth Of Field shader from the BlenderArtists forums.
*/

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D depthTexture;
uniform float screenWidth;
uniform float screenHeight;

const float depthRangeNear = 0.3; // The shader will blur pixels closer than this (in a range, 0-1, 0 being near to the camera, 1 being far)
const float depthRangeFar = 0.7;  // And pixels farther than this (in the same range as above)
const float nearThick = 2.0;  // How quickly the blur ramps up for the near plane
const float farThick = 2.0;  // Similarly for the far plane

const int sample_count = 8;	// Number of samples for the blur
const float sample_size = 0.001;	// Sample size for the blur
const bool only_depth = false;	// If it should only render the depth result (useful for debugging; when optimizing, take out this and the if statement below

float getDepth(vec4 rgba)
{
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    float z = dot(rgba, bitShifts);
    return clamp(z, 0.0, 1.0);
}

void main() {

	vec2 aspectCorrect = vec2(1.0, screenWidth / screenHeight);

	vec4 depthTex = texture2D(depthTexture, v_texCoords);

	float ln = 1.0 / depthRangeNear;
	float lf = 1.0 / abs(1.0 - depthRangeFar);

	float factor = -(getDepth(depthTex) - depthRangeNear);
	factor = clamp(factor * ln * nearThick, 0.0, 1.0);
	float farPlane = getDepth(depthTex) - depthRangeFar;
	farPlane = clamp(farPlane * lf * farThick, 0.0, 1.0);
	factor += farPlane;

	vec4 originalCol = texture2D(u_texture, v_texCoords);

	vec4 col = vec4(0.0, 0.0, 0.0, 1.0);

	if (only_depth)
		gl_FragColor = vec4(factor);
	else {
		for (int y = -sample_count; y < sample_count; y++) {

			for (int x = -sample_count; x < sample_count; x++) {
				col += texture2D(u_texture, (v_texCoords.xy + vec2(x * sample_size, y * sample_size) * aspectCorrect));
			}

		}
		col /= (sample_count * 2.0) * (sample_count * 2.0) + 1.0;

		gl_FragColor = mix(originalCol, col, factor);
	}

	gl_FragColor.a = originalCol.a;
}
