#pragma optionNV(strict on)

#ifdef GL_ES
    precision mediump float;
#endif

/*
Based on the Toon Lines shader by Luiz Felipe M. Pereira (felipearts) on BlenderArtists forums.
*/

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D depthTexture;
uniform mat4 u_projTrans;

uniform float near;
uniform float far;
uniform float screenWidth;
uniform float screenHeight;

const float edgeStrengthNear = 1.0;	// How strong the outline is for close pixels
const float edgeStrengthFar = 1.0;  // How strong for far pixels
const float baseThresh = 5.0;  // How "easy" it is for outlines to appear. Lower numbers = more outlines
const int sampleNum = 3;  // How many samples to use when checking for outlines; generally should be fine at 3
const float sampleSize = 0.001;  // How far out to detect outlines

const vec4 outlineColor = vec4(0.0, 0.0, 0.0, 1.0);	  // What color outline to use

float getDepth(vec4 rgba)
{
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    float z = dot(rgba, bitShifts);
    return z;
}

void main() {

	float[sampleNum][sampleNum] sam;

    vec4 color = texture2D(u_texture, v_texCoords);

    float depth = getDepth(texture2D(depthTexture, v_texCoords));

	vec2 aspectCorrect = (1.0, screenHeight / screenWidth);

	aspectCorrect *= mix(edgeStrengthNear, edgeStrengthFar, depth);

	float colDifForce = 0.0;

    for (int y = 0; y < sampleNum; y++) {
    	for (int x = 0; x < sampleNum; x++) {
    		sam[y][x] = getDepth(texture2D(depthTexture, v_texCoords + vec2(x, y) * aspectCorrect * sampleSize));
			colDifForce += depth - sam[y][x];
    	}
    }

	if (abs(colDifForce) > ((sam[1][1] * (baseThresh / (far - near))) / near))
    	gl_FragColor = mix(color, outlineColor, outlineColor.a);
    else
		gl_FragColor = color;		// No edge, so just display the normal render
}