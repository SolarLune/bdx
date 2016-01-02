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

public class Light extends GameObject {

	public String type;
	private float energy;
	private Vector4f color;
	public BaseLight lightData;
	
	public void makeLightData(){
		
		if (type.equals("POINT"))
			lightData = new PointLight();
		else if (type.equals("SUN"))
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
			if (type.equals("POINT")) {
				PointLight p = (PointLight)lightData;
				p.set(color.x, color.y, color.z, position().x, position().y, position().z, energy * 10);
			}
			else if (type.equals("SUN")) {
				DirectionalLight d = (DirectionalLight)lightData;
				Vector3f dir = axis(2).negated();
				d.set(color.x, color.y, color.z, dir.x, dir.y, dir.z);
			}
		}
	}

	@Override
	public void endNoChildren(){

		if (type.equals("POINT"))
			((PointLightsAttribute) scene.environment.get(PointLightsAttribute.Type)).lights.removeValue((PointLight) lightData, true);		// Remove the light from the environment
		if (type.equals("SUN"))
			((DirectionalLightsAttribute) scene.environment.get(DirectionalLightsAttribute.Type)).lights.removeValue((DirectionalLight) lightData, true);
		if (type.equals("SPOT"))
			((SpotLightsAttribute) scene.environment.get(SpotLightsAttribute.Type)).lights.removeValue((SpotLight) lightData, true);

		super.endNoChildren();
	}
	
	@Override
	public void transform(Matrix4f mat, boolean updateLocal) {
				
		super.transform(mat, updateLocal);
		
		updateLight();
	}
	
}
