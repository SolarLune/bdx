#pragma optionNV(strict on)

#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

const float strength = 1.0;

void main() {

    vec4 color = texture2D(u_texture, v_texCoords);

    vec4 inverted = vec4(color);
    inverted.r = 1.0 - inverted.r;
    inverted.g = 1.0 - inverted.g;
    inverted.b = 1.0 - inverted.b;

    gl_FragColor = mix(color, inverted, strength);

}
