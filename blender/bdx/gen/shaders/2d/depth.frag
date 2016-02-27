#pragma optionNV(strict on)

#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D depthTexture;
uniform float far;
uniform float near;

float getDepth(vec4 rgba)
{
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    float z = dot(rgba, bitShifts);
    return z;
}

void main() {

    gl_FragColor = vec4(vec3(getDepth(texture2D(depthTexture, v_texCoords))), 1.0);

}
