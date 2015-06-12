package com.nilunder.bdx;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;

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
		updateLight();
	}
	
	public Vector4f color(){
		return color;
	}
		
	public void energy(float energy) {
		this.energy = energy;
		updateLight();
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
		super.endNoChildren();
		scene.environment.remove(lightData);
	}
	
	@Override
	public void transform(Matrix4f mat, boolean updateLocal) {
				
		super.transform(mat, updateLocal);
		
		updateLight();
	}
	
}
