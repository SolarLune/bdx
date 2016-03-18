package com.nilunder.bdx.gl;

import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Camera;

import javax.vecmath.Vector2f;

public class Viewport {

	public FitViewport viewportData;
	private Camera camera;
	private Vector2f size;
	private Vector2f position;

	public Viewport() {
		viewportData = new FitViewport(Bdx.display.size().x, Bdx.display.size().y);
		size = new Vector2f(1, 1);
		position = new Vector2f(0, 0);
	}

	public Viewport(Camera camera){
		this();
		camera(camera);
	}

	public void camera(Camera cam) {
		camera = cam;
		viewportData.setCamera(camera.data);
	}

	public Camera camera(){
		return camera;
	}

	public Vector2f position(){
		return position;
	}

	public void position(float x, float y) {
		this.position.set(position);
		viewportData.setScreenPosition( (int)(viewportData.getScreenWidth() * x), (int)(viewportData.getScreenHeight() * y) );
	}

	public void position(Vector2f pos) {
		position(pos.x, pos.y);
	}

	public Vector2f size(){
		return size;
	}

	public void size(float x, float y){
		this.size.set(size);
		viewportData.setScreenSize( (int)(viewportData.getScreenWidth() * x), (int)(viewportData.getScreenHeight() * y) );
	}

	public void size(Vector2f size){
		size(size.x, size.y);
	}

}
