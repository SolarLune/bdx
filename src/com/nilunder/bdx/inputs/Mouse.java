package com.nilunder.bdx.inputs;

import javax.vecmath.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;

import com.bulletphysics.collision.dispatch.*;

import com.nilunder.bdx.*;

public class Mouse {

	public Scene scene;
	
	public boolean btnHit(String btn){
		if (!Gdx.input.justTouched()){
			return false;
		}
		return btnDown(btn);
	}

	public boolean btnDown(String btn){
		if (btn.equals("left")){
			return Gdx.input.isButtonPressed(Buttons.LEFT);
		}else if (btn.equals("right")){
			return Gdx.input.isButtonPressed(Buttons.RIGHT);
		}
		return false;
	}
	
	public Vector4f clipCoords(){
		float mx = (2.0f * Gdx.input.getX()) / Gdx.graphics.getWidth() - 1.0f;
		float my = 1.0f - (2.0f * Gdx.input.getY()) / Gdx.graphics.getHeight();
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
		String type = scene.camera._json.get("camera").get("type").asString();
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

	public boolean clicked(GameObject g){
		return clicked(g, "left");
	} 

	public boolean clicked(GameObject g, String btn){
		if (btnHit(btn)){
			RayHit rh = ray();
			if (rh != null && rh.object == g){
				return true;
			}
		}

		return false;
	} 
	
}
