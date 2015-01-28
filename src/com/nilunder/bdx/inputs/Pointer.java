package com.nilunder.bdx.inputs;

import javax.vecmath.*;

import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.*;

abstract class Pointer {

	public Scene scene;
	public int id;

	public Pointer(int id){
		this.id = id;
	}
	
	public Vector4f clipCoords(){
		float mx = (2.0f * Gdx.input.getX(id)) / Gdx.graphics.getWidth() - 1.0f;
		float my = 1.0f - (2.0f * Gdx.input.getY(id)) / Gdx.graphics.getHeight();
		return new Vector4f(mx, my, -1.0f, 1.0f);
	}

	public Vector3f raySource(){
		Vector4f v = clipCoords();
		
		// Transform to view space:
		Matrix4f invProj = scene.camera.projection();
		invProj.invert();
		invProj.transform(v);
		v.z = -1;
		v.w = 0;
		
		// To world space:
		Matrix4f ct = scene.camera.transform();
		ct.transform(v);
		
		Vector3f wv = new Vector3f(v.x, v.y, v.z);
		wv.add(scene.camera.position());
		
		return wv;
	}

	public Vector3f rayDirection(){
		String type = scene.camera.json.get("camera").get("type").asString();
		if (type.equals("ORTHO")){
			return scene.camera.axis(2).negated();
		}

		Vector3f dir = raySource().minus(scene.camera.position());
		return dir;
	}
	
	public RayHit ray(){

		Vector3f v = rayDirection();
		v.length(100);

		return scene.ray(raySource(), v);
	}
}
