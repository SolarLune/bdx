package com.nilunder.bdx.inputs;

import java.util.ArrayList;

import javax.vecmath.*;

import com.badlogic.gdx.Gdx;
import com.nilunder.bdx.*;

abstract class Pointer {

	public Scene scene;
	public int id;

	public Pointer(int id){
		this.id = id;
	}
	
	public Vector2f position(){
		return new Vector2f(Gdx.input.getX(id), Gdx.input.getY(id));
	}

	public Vector2f positionNormalized(){
		Vector2f c = Bdx.display.center();
		float x = (float)Gdx.input.getX(id) / (c.x * 2);
		float y = 1 - (float)Gdx.input.getY(id) / (c.y * 2);
		return new Vector2f(x, y);
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
	
	public RayHit ray(short group, short mask){
		Vector3f v = rayDirection();
		v.length(100);

		return scene.ray(raySource(), v, group, mask);
	}

	public RayHit ray(){
		return ray((short)~0, (short)~0);
	}
	
	public ArrayList<RayHit> xray(boolean includeAll, short group, short mask) {
	
		Vector3f v = rayDirection();
		v.length(100);

		return scene.xray(raySource(), v, includeAll, group, mask);
		
	}
	
	public ArrayList<RayHit> xray(){
		return xray(false, (short)~0, (short)~0);
	}
	
}
