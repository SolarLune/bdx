package com.nilunder.bdx.inputs;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.RayHit;
import com.nilunder.bdx.gl.Viewport;

abstract class Pointer{
	
	private int id;
	private Viewport viewport;
	private Vector2f position;
	private Vector3f[] worldCoords;
	
	public Pointer(int id){
		this.id = id;
	}
	
	public int id(){
		return id;
	}
	
	public boolean insideScreenArea(Vector2f min, Vector2f max){
		return min.x <= position.x && max.x >= position.x && min.y <= position.y && max.y >= position.y;
	}
	
	private Viewport viewport(Scene scene){
		Vector2f min;
		Vector2f max;
		Viewport vp;
		for (int i = scene.viewports.size() - 1; i > -1; i--){
			vp = scene.viewports.get(i);
			min = vp.position();
			max = min.plus(vp.size());
			if (insideScreenArea(min, max)){
				return vp;
			}
		}
		return null;
	}
	
	public void update(Scene scene){
		position = new Vector2f(Gdx.input.getX(id), Bdx.display.height() - Gdx.input.getY(id));
		viewport = viewport(scene);
		worldCoords = null;
	}
	
	public Viewport viewport(){
		return viewport;
	}
	
	public Vector2f position(){
		return new Vector2f(position);
	}

	public Vector2f positionNormalized(){
		return position.div(Bdx.display.size());
	}
	
	private boolean hasWorldCoordinates(){
		if (viewport != null){
			if (worldCoords == null){
				worldCoords = viewport.worldCoords(position);
			}
			return true;
		}
		return false;
	}
	
	public Vector3f[] worldCoords(){
		if (hasWorldCoordinates()){
			return new Vector3f[]{new Vector3f(worldCoords[0]), new Vector3f(worldCoords[1])};
		}
		return null;
	}
	
	public RayHit ray(short group, short mask){
		if (hasWorldCoordinates()){
			Vector3f target = new Vector3f(worldCoords[1]);
			target.length(100);
			return viewport.scene().ray(worldCoords[0], target, group, mask);
		}
		return null;
	}
	
	public RayHit ray(){
		return ray((short)~0, (short)~0);
	}
	
	public ArrayList<RayHit> xray(boolean includeAll, short group, short mask){
		if (hasWorldCoordinates()){
			Vector3f target = new Vector3f(worldCoords[1]);
			target.length(100);
			return viewport.scene().xray(worldCoords[0], target, includeAll, group, mask);
		}
		return new ArrayList<RayHit>();
	}
	
	public ArrayList<RayHit> xray(short group, short mask){
		return xray(false, group, mask);
	}
	
	public ArrayList<RayHit> xray(){
		return xray(false, (short)~0, (short)~0);
	}
	
}
