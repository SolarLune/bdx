package com.nilunder.bdx.gl;

import com.badlogic.gdx.graphics.glutils.HdpiUtils;

import javax.vecmath.Vector2f;
import javax.vecmath.Matrix3f;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.Camera;

public class Viewport{
	
	public enum Type{
		LETTERBOX,
		EXTEND,
		SCALE,
		SCREEN
	}
	
	private Type type;
	private Vector2f positionNormalized, sizeNormalized;
	
	public int x, y, w, h;
	public Scene scene;
	
	public Viewport(Scene scene, Type type){
		this.scene = scene;
		this.type = type;
		this.positionNormalized = new Vector2f();
		this.sizeNormalized = new Vector2f(1, 1);
		update();
	}
	
	public Type type(){
		return type;
	}
	
	public void type(Type type){
		this.type = type;
		update();
	}
	
	public Matrix3f orientation(){
		return scene.camera.orientation();
	}
	
	public Vector2f resolution(){
		return scene.camera.resolution();
	}
	
	public void resolution(Vector2f resolution){
		resolution(Math.round(resolution.x), Math.round(resolution.y));
	}
	
	public void resolution(int width, int height){
		scene.camera.resolution(width, height);
		update();
	}
	
	public Vector2f sizeNormalized(){
		return new Vector2f(sizeNormalized);
	}
	
	public void sizeNormalized(Vector2f sizeNormalized){
		this.sizeNormalized = sizeNormalized;
		update();
	}
	
	public void sizeNormalized(float width, float height){
		sizeNormalized(new Vector2f(width, height));
	}
	
	public Vector2f size(){
		return sizeNormalized.mul(Bdx.display.size());
	}
		
	public void size(Vector2f size){
		sizeNormalized(size.div(Bdx.display.size()));
	}
	
	public void size(int width, int height){
		size(new Vector2f(width, height));
	}
	
	public Vector2f positionNormalized(){
		return new Vector2f(positionNormalized);
	}
	
	public void positionNormalized(Vector2f positionNormalized){
		this.positionNormalized = positionNormalized;
		update();
	}
		
	public void positionNormalized(float x, float y){
		positionNormalized(new Vector2f(x, y));
	}
	
	public Vector2f position(){
		return positionNormalized.mul(Bdx.display.size());
	}
	
	public void position(Vector2f position){
		positionNormalized(position.div(Bdx.display.size()));
	}
	
	public void position(float x, float y){
		position(new Vector2f(x, y));
	}
	
	public void apply(){
		scene.camera.update();
		HdpiUtils.glViewport(x, y, w, h);
	}
	
	public void update(int displayWidth, int displayHeight){
		Vector2f resolution = resolution();
		x = Math.round(positionNormalized.x * displayWidth);
		y = Math.round(positionNormalized.y * displayHeight);
		if (type == Type.SCREEN){
			w = Math.round(sizeNormalized.x * resolution.x);
			h = Math.round(sizeNormalized.y * resolution.y);
			scene.camera.size(resolution);
		}else{
			w = Math.round(sizeNormalized.x * displayWidth);
			h = Math.round(sizeNormalized.y * displayHeight);
			if (type == Type.SCALE){
				scene.camera.size(resolution);
			}else{
				float dRatio = (float) w / h;
				if (type == Type.LETTERBOX){
					float rRatio = resolution.x / resolution.y;
					if (dRatio < rRatio){
						int h2 = Math.round(w / rRatio);
						y += (h - h2) / 2;
						h = h2;
					}else if (dRatio > rRatio){
						int w2 = Math.round(h * rRatio);
						x += (w - w2) / 2;
						w = w2;
					}
					if (scene.camera.type == Camera.Type.PERSPECTIVE){
						scene.camera.size(w, h);
					}
				}else if (type == Type.EXTEND){
					scene.camera.size(Math.round(resolution.y * dRatio), (int) resolution.y);
				}
			}
		}
		apply();
	}
	
	public void update(){
		update(Bdx.display.width(), Bdx.display.height());
	}
	
}
