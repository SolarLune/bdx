#ifdef GL_ES 
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#ifdef normalFlag
varying vec3 v_normal;
#endif //normalFlag

#if defined(colorFlag)
varying vec4 v_color;
#endif

#ifdef blendedFlag
varying float v_opacity;
#ifdef alphaTestFlag
varying float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#ifdef diffuseTextureFlag
varying MED vec2 v_diffuseUV;
#endif

#ifdef specularTextureFlag
varying MED vec2 v_specularUV;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#ifdef specularFlag
varying vec3 v_lightSpecular;
#endif //specularFlag

#ifdef shadowMapFlag
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
varying vec3 v_shadowMapUv;
#define separateAmbientFlag

float getShadowness(vec2 offset)
{
	const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
	return step(v_shadowMapUv.z, dot(texture2D(u_shadowTexture, v_shadowMapUv.xy + offset), bitShifts));//+(1.0/255.0));
}

float getShadow() 
{
	return (//getShadowness(vec2(0,0)) + 
			getShadowness(vec2(u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(u_shadowPCFOffset, -u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, -u_shadowPCFOffset))) * 0.25;
}
#endif //shadowMapFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#if defined(numPointLights) && (numPointLights > 0)
struct PointLight
{
	HIGH vec3 color;
	HIGH vec3 position;
};
uniform PointLight u_pointLights[numPointLights];
#endif // numPointLights

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
struct DirectionalLight
{
	HIGH vec3 color;
	HIGH vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif // numDirectionalLights

#if defined(numSpotLights) && (numSpotLights > 0)
struct SpotLight
{
	vec3 color;
	vec3 position;
	vec3 direction;
	float cutoffAngle;
	float exponent;
};
uniform SpotLight u_spotLights[numSpotLights];
#endif

#endif //lightingFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
varying float v_fog;
uniform vec2 u_fogRange;
uniform vec2 u_camRange;

vec4 mix_fog(vec4 fragColour) {
  float perspective_far = u_camRange.y;
  //float start = 20.0;
  //float depth = 34.0;
  float start = u_fogRange.x;
  float depth = u_fogRange.y;
  float fog_coord = ((gl_FragCoord.z / gl_FragCoord.w) - start) / perspective_far;
  float fog = fog_coord * (perspective_far / depth);
  return mix(u_fogColor, fragColour, clamp(1.0 - fog, 0., 1.) );
}

#endif // fogFlag

varying vec3 v_position;
varying vec3 view_vec;

uniform HIGH float u_shininess;
uniform int u_shadeless;
uniform vec4 u_tintColor;
uniform vec4 u_emitColor;

vec3 applyLighting(vec3 inColor){

	vec3 diffuseColor = vec3(u_emitColor.rgb + v_lightDiffuse.rgb);
	vec3 specColor = vec3(0, 0, 0);

	vec3 normal = normalize(v_normal);

	for (int i = 0; i < numPointLights; i++) {

		vec3 lP = u_pointLights[i].position;
		float distance = length(lP - v_position);
		vec3 lightVector = normalize(lP - v_position);
		float diffuse = max(dot(normal, lightVector), 0.0);
		float attenuation = 1.0 / (1.0 + (0.25 * distance * distance));  // Light falloff
		diffuse *= attenuation;
		diffuseColor += (u_pointLights[i].color * diffuse);

		#if defined(specularColorFlag)
		vec3 reflectVector = normalize(-reflect(lightVector, normal));
		vec3 H = normalize(lightVector + view_vec);
		float spec = pow(max(dot(normal, H), 0.0), u_shininess);
		if (diffuse <= 0.0)
			spec = 0.0;
		specColor += u_specularColor.rgb * u_pointLights[i].color * spec * attenuation;
		#endif
	}

	for (int i = 0; i < numDirectionalLights; i++){

		vec3 lightVector = -u_dirLights[i].direction;
		float diffuse = max(dot(normal, lightVector), 0.0);
		diffuseColor += (u_dirLights[i].color * diffuse);

		#if defined(specularColorFlag)
		vec3 reflectVector = normalize(-reflect(lightVector, normal));
		vec3 H = normalize(lightVector + view_vec);
		float spec = pow(max(dot(normal, H), 0.0), u_shininess);
		if (diffuse <= 0.0)
			spec = 0.0;
		specColor += u_specularColor.rgb * u_dirLights[i].color * spec;
		#endif

	}

	for (int i = 0; i < numSpotLights; i++) {
		vec3 lP = u_spotLights[i].position;
		float distance = length(lP - v_position);
		vec3 lightVector = normalize(lP - v_position);
		float diffuse = max(dot(normal, lightVector), 0.0);

		float spotEffect = max(dot(normalize(u_spotLights[i].direction), -lightVector), 0.0);

		if (acos(spotEffect) < u_spotLights[i].cutoffAngle / 2.0) {

			spotEffect = pow(spotEffect, u_spotLights[i].exponent);
			float attenuation = spotEffect / (1.0 * (0.25 * distance));
			diffuse *= attenuation;
			diffuseColor += (u_spotLights[i].color * diffuse);
			#if defined(specularColorFlag)
			vec3 reflectVector = normalize(-reflect(lightVector, normal));
			vec3 H = normalize(lightVector + view_vec);
			float spec = pow(max(dot(normal, H), 0.0), u_shininess);
			specColor += u_specularColor.rgb * u_spotLights[i].color * spec * attenuation;
			#endif
		}

	}

	inColor *= diffuseColor;

	inColor += specColor;

	return inColor;
}

void main() {
	#if defined(normalFlag) 
		vec3 normal = v_normal;
	#endif // normalFlag
		
	#if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor * v_color;
	#elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor;
	#elif defined(diffuseTextureFlag) && defined(colorFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * v_color;
	#elif defined(diffuseTextureFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV);
	#elif defined(diffuseColorFlag) && defined(colorFlag)
		vec4 diffuse = u_diffuseColor * v_color;
	#elif defined(diffuseColorFlag)
		vec4 diffuse = u_diffuseColor;
	#elif defined(colorFlag)
		vec4 diffuse = v_color;
	#else
		vec4 diffuse = vec4(1.0);
	#endif

	diffuse.rgb += u_tintColor.rgb;

	if (u_shadeless == 1)
		gl_FragColor.rgb = diffuse.rgb;
	else
		gl_FragColor.rgb = applyLighting(diffuse.rgb);

	#ifdef fogFlag
		gl_FragColor = mix_fog(gl_FragColor);
	#endif // end fogFlag

	#ifdef blendedFlag
		gl_FragColor.a = diffuse.a * v_opacity;
		#ifdef alphaTestFlag
			if (gl_FragColor.a <= v_alphaTest)
				discard;
		#endif
	#else
		gl_FragColor.a = 1.0;
	#endif

}
