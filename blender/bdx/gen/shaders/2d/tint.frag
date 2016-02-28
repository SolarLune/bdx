#pragma optionNV(strict on)

#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

const float strength = 0.5;               // How strong the tinting effect is
const vec3 tint = vec3(1.0, 0.0, 0.0);    // The targeted tinting color

void main() {
	
	vec4 color = texture2D(u_texture, v_texCoords);
	color.rgb = vec3(color.rgb + (tint * strength));
	gl_FragColor = color;

}
