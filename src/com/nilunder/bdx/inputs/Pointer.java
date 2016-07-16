package com.nilunder.bdx.inputs;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.RayHit;

abstract class Pointer{
	
	private Vector2f position;
	private Vector3f[] rayData;
	private Scene scene;
	
	public int id;
	
	public Pointer(int id){
		this.id = id;
	}
	
	public void init(Scene scene){
		this.scene = scene;
		position = new Vector2f(Gdx.input.getX(id), Bdx.display.height() - Gdx.input.getY(id));
		rayData = null;
	}
	
	private void updateRayData(){
		if (rayData == null){
			rayData = scene.camera.rayData(position);
		}
	}
	
	public Vector4f clipCoords(){
		return new Vector4f(2 * position.x / Bdx.display.width() - 1, 2 * position.y / Bdx.display.height() - 1, -1, 1);
	}
	
	public Vector3f raySource(){
		updateRayData();
		return new Vector3f(rayData[0]);
	}
	
	public Vector3f rayDirection(){
		updateRayData();
		return new Vector3f(rayData[1]);
	}
	
	public Vector2f position(){
		return new Vector2f(position);
	}
	
	public Vector2f positionNormalized(){
		return position.div(Bdx.display.size());
	}
	
	public RayHit ray(short group, short mask){
		updateRayData();
		Vector3f target = new Vector3f(rayData[1]);
		target.length(100);
		return scene.ray(rayData[0], target, group, mask);
	}
	
	public RayHit ray(){
		return ray((short)~0, (short)~0);
	}
	
	public ArrayList<RayHit> xray(boolean includeAll, short group, short mask){
		updateRayData();
		Vector3f target = new Vector3f(rayData[1]);
		target.length(100);
		return scene.xray(rayData[0], target, includeAll, group, mask);
	}
	
	public ArrayList<RayHit> xray(short group, short mask) {
		return xray(false, group, mask);
	}
	
	public ArrayList<RayHit> xray(){
		return xray(false, (short)~0, (short)~0);
	}
	
}
