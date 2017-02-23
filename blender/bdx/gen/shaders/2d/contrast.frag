#pragma optionNV(strict on)

#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

const float STRENGTH = 0.25;             // Higher values = more contrast
const vec3 GRAY = vec3(0.5, 0.5, 0.5);  // The base color that the contrast bases color on

void main() {

	vec4 color = texture2D(u_texture, v_texCoords);
	gl_FragColor = vec4(GRAY + ((color.rgb - GRAY) * STRENGTH), color.a);

}

