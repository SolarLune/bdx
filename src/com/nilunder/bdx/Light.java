package com.nilunder.bdx;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.SpotLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;

public class Light extends GameObject {

	public Type type;
	private float energy;
	private Vector4f color;
	public BaseLight lightData;

	public enum Type {
		POINT,
		SUN,
		SPOT
	}
	
	public void makeLightData(){
		
		if (type.equals(Type.POINT))
			lightData = new PointLight();
		else if (type.equals(Type.SUN))
			lightData = new DirectionalLight();	
		
		updateLight();
	}
			
	public void color(float r, float g, float b, float a){
		this.color = new Vector4f(r,g,b,a);
	}
	
	public Vector4f color(){
		return color;
	}
		
	public void energy(float energy) {
		this.energy = energy;
	}
	
	public float energy(){
		return energy;
	}
	
	public void updateLight(){
		if (lightData != null) {
			if (type.equals(Type.POINT)) {
				PointLight p = (PointLight)lightData;
				p.set(color.x, color.y, color.z, position().x, position().y, position().z, energy);
			}
			else if (type.equals(Type.SUN)) {
				DirectionalLight d = (DirectionalLight)lightData;
				Vector3f dir = axis(2).negated();
				d.set(color.x, color.y, color.z, dir.x, dir.y, dir.z);
			}
		}
	}

	@Override
	public void endNoChildren(){

		if (type.equals(Type.POINT))
			((PointLightsAttribute) scene.environment.get(PointLightsAttribute.Type)).lights.removeValue((PointLight) lightData, true);		// Remove the light from the environment
		if (type.equals(Type.SUN))
			((DirectionalLightsAttribute) scene.environment.get(DirectionalLightsAttribute.Type)).lights.removeValue((DirectionalLight) lightData, true);
		if (type.equals(Type.SPOT))
			((SpotLightsAttribute) scene.environment.get(SpotLightsAttribute.Type)).lights.removeValue((SpotLight) lightData, true);

		super.endNoChildren();
	}
	
	@Override
	public void transform(Matrix4f mat, boolean updateLocal) {
				
		super.transform(mat, updateLocal);
		
		updateLight();
	}

	public static void setMaxCount(Type lightType, int count){
		DefaultShader.Config config = Bdx.shaderProvider.config;

		if (lightType.equals(Type.POINT))
			config.numPointLights = count;
		else if (lightType.equals(Type.SUN))
			config.numDirectionalLights = count;
		else
			config.numSpotLights = count;

		Bdx.shaderProvider.deleteShaders();			// Get rid of the old shaders, as they need to be recreated for the new light count.
	}

	public static int getMaxCount(Type lightType){
		DefaultShader.Config config = Bdx.shaderProvider.config;
		if (lightType.equals(Type.POINT))
			return config.numPointLights;
		else if (lightType.equals(Type.SUN))
			return config.numDirectionalLights;
		else
			return config.numSpotLights;

	}
	
}
