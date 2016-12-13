package com.nilunder.bdx;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.SpotLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.nilunder.bdx.utils.Color;

public class Light extends GameObject {

	public Type type;
	public BaseLight lightData;
	private float energy;
	private Color color = new Color();
	private float spotSize;
	private float exponent = 1;
	private boolean on;

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
		else if (type.equals(Type.SPOT))
			lightData = new SpotLight();

		updateLight();
	}

	public void color(Color color){
		this.color.set(color);
	}
	
	public Color color(){
		return color;
	}
		
	public void energy(float energy) {
		this.energy = energy;
	}
	
	public float energy(){
		return energy;
	}

	public void spotSize(float angle){
		spotSize = angle;
	}

	public float spotSize(){
		return spotSize;
	}

	public void exponent(float exponentFactor){
		exponent = exponentFactor;
	}

	public float exponent(){
		return exponent;
	}

	public void updateLight(){
		if (lightData != null) {
			if (type.equals(Type.POINT)) {
				PointLight p = (PointLight)lightData;
				p.set(color.r, color.g, color.b, position().x, position().y, position().z, energy);
			}
			else if (type.equals(Type.SUN)) {
				DirectionalLight d = (DirectionalLight)lightData;
				Vector3f dir = axis(2).negated();
				d.set(color.r, color.g, color.b, dir.x, dir.y, dir.z);
			}
			else if (type.equals(Type.SPOT)) {
				SpotLight s = (SpotLight) lightData;
				Vector3f down = axis(2).negated();
				s.set(color.r, color.g, color.b, position().x, position().y, position().z, down.x, down.y, down.z, energy, spotSize, exponent);
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

	public void on(boolean on){

		this.on = on;

		if (type == Type.POINT) {
			PointLightsAttribute la = (PointLightsAttribute) scene.environment.get(PointLightsAttribute.Type);
			PointLight pl = (PointLight) lightData;

			if (on && !la.lights.contains(pl, true))
				la.lights.add(pl);
			else if (!on && la.lights.contains(pl, true))
				la.lights.removeValue(pl, true);

		} else if (type == Type.SUN) {
			DirectionalLightsAttribute la = (DirectionalLightsAttribute) scene.environment.get(DirectionalLightsAttribute.Type);
			DirectionalLight dl = (DirectionalLight) lightData;

			if (on && !la.lights.contains(dl, true))
				la.lights.add(dl);
			else if (!on && la.lights.contains(dl, true))
				la.lights.removeValue(dl, true);

		} else if (type == Type.SPOT) {
			SpotLightsAttribute la = (SpotLightsAttribute) scene.environment.get(SpotLightsAttribute.Type);
			SpotLight sl = (SpotLight) lightData;

			if (on && !la.lights.contains(sl, true))
				la.lights.add(sl);
			else if (!on && la.lights.contains(sl, true))
				la.lights.removeValue(sl, true);

		}

	}

	public boolean on(){
		return on;
	}

}
