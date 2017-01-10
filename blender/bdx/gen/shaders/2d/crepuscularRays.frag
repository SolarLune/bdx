#pragma optionNV(strict on)

#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;

const float SAMPLE_SIZE = 0.4;			        // How long the rays are (how large the samples are in the shader)
const float FALLOFF_CONST = 1.0;               	// How much the ray effect falls off in relation to the distance to the light source
const float DECAY = 1.0;                        // How quickly rays weaken
const float MAX_BRIGHTNESS = 2.0;               // How bright the effect can get, maximum

const int NUM_SAMPLES = 128;			        // Number of samples (more = smoother)
const vec4 COLOR = vec4(1.0, 0.75, 0.5, 1.0);	// Ray color; alpha is ignored
const float STRENGTH = 0.5;			            // Strength of each "ray"
const vec2 LIGHT_POSITION = vec2(0.5, 1.0);	    // Position of the crep. rays onscreen (could be uniform, so that they come from something visible)

vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {

    vec2 texCoords = vec2(v_texCoords);

    vec3 add = vec3(0, 0, 0);

    float ssize = (SAMPLE_SIZE / NUM_SAMPLES);

    vec2 delta = (LIGHT_POSITION - texCoords) * ssize;
    float dist = length(texCoords - LIGHT_POSITION);        // Distance from the texture coordinate to the light

    float str = STRENGTH;

    for (int i = 0; i < NUM_SAMPLES; i++) {

        texCoords += delta;

        vec4 s = texture2D(u_texture, texCoords);

        if (s.a <= 0)
            add += COLOR.rgb * (str / dist);

        str *= DECAY;

    }

    float falloff = 1.0 - (dist * FALLOFF_CONST);

    falloff = clamp(falloff, 0.0, 1.0);

    vec3 final = add / float(NUM_SAMPLES) * falloff * str;

    final = rgb2hsv(final);

    final.b = clamp(final.b, 0.0, MAX_BRIGHTNESS);

    final = hsv2rgb(final);

    gl_FragColor.rgb = final;

}
