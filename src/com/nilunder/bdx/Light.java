package com.nilunder.bdx;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.SpotLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.nilunder.bdx.utils.Color;

import java.util.ArrayList;

public class Light extends GameObject {

	public Type type;
	public BaseLight lightData;
	public ArrayList<Camera> shadowCams = new ArrayList<Camera>();
	public float shadowNear;
	public float shadowFar;
	private float energy;
	private Color color = new Color();
	private float spotSize;
	private float exponent = 1;

	public enum Type {
		POINT,
		SUN,
		SPOT
	}
	
	public void makeLightData(){
		
		if (type.equals(Type.POINT))
			lightData = new PointLight();
		else if (type.equals(Type.SUN)) {
			lightData = new DirectionalLight();
			Camera cam = new Camera();				// This should be an actual camera, I think. We should figure out a way to create a default cam.
			cam.initData(Camera.Type.PERSPECTIVE);
			cam.data = new PerspectiveCamera(120f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			cam.resolution(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());		// Set the resolution for the renderbuffer to render the light's view
			shadowCams.add(cam);
		}
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

			for (Camera cam : shadowCams) {
				com.badlogic.gdx.graphics.Camera camData = cam.data;
				camData.near = shadowNear;
				camData.far = shadowFar;
				camData.position.set(position().x, position().y, position().z);
				Vector3f lookTarget = position();
				lookTarget.plus(axis(2).negated());
				camData.lookAt(lookTarget.x, lookTarget.y, lookTarget.z);		// Needs to change to account for multiple sides for a point light shadow
				camData.update();
			}

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

	public boolean shadowOn(){
		return shadowCams.size() > 0 && shadowCams.get(0).renderingToTexture;
	}

	public void shadowOn(boolean on){
		for (Camera cam : shadowCams)
			cam.renderingToTexture = on;
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

}
