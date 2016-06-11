package com.nilunder.bdx.gl;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.Vector3;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.Camera;
import com.nilunder.bdx.utils.Named;
import com.nilunder.bdx.utils.Color;

public class Viewport implements Named{
	
	public enum Type{
		LETTERBOX,
		EXTEND,
		SCALE,
		SCREEN
	}
	
	private Vector2f positionNormalized, sizeNormalized;
	private com.badlogic.gdx.utils.viewport.Viewport data;
	private Camera camera;
	private String name;
	private Type type;
	
	private ArrayList<ArrayList<Object>> drawCommands;
	static private ShapeRenderer shapeRenderer;
	
	public Viewport(String name, Camera camera, Type type){
		this.name = name;
		this.camera = camera;
		positionNormalized = new Vector2f();
		sizeNormalized = new Vector2f(1, 1);
		type(type);
		update();
		
		shapeRenderer = new ShapeRenderer();
		drawCommands = new ArrayList<ArrayList<Object>>();
	}
	
	@Override
	public String name(){
		return name;
	}
	
	public void name(String name){
		this.name = name;
	}
	
	public Type type(){
		return type;
	}
	
	public void type(Type type){
		this.type = type;
		if (type == Type.SCREEN){
			data = new ScreenViewport(camera.data);
		}else{
			Vector2f resolution = camera.resolution();
			if (type == Type.LETTERBOX){
				data = new FitViewport(resolution.x, resolution.y, camera.data);
			}else if (type == Type.EXTEND){
				data = new ExtendViewport(resolution.x, resolution.y, camera.data);
			}else if (type == Type.SCALE){
				data = new StretchViewport(resolution.x, resolution.y, camera.data);
			}
		}
	}
	
	public Scene scene(){
		return camera.scene;
	}
	
	public Camera camera(){
		return camera;
	}
	
	public void camera(Camera camera){
		data.setCamera(camera.data);
		this.camera = camera;
	}
	
	public Vector2f resolution(){
		return camera.resolution();
	}
	
	public void resolution(Vector2f resolution){
		camera.resolution(resolution);
	}
	
	public void resolution(float width, float height){
		camera.resolution(width, height);
	}
	
	public Vector2f sizeNormalized(){
		return sizeNormalized;
	}
	
	public void sizeNormalized(Vector2f sizeNormalized){
		this.sizeNormalized = sizeNormalized;
	}
	
	public void sizeNormalized(float width, float height){
		sizeNormalized(new Vector2f(width, height));
	}
	
	public Vector2f size(){
		return new Vector2f(data.getScreenWidth(), data.getScreenHeight());
	}
	
	public void size(Vector2f size){
		sizeNormalized = size.div(Bdx.display.size());
	}
	
	public void size(float width, float height){
		size(new Vector2f(width, height));
	}
	
	public Vector2f positionNormalized(){
		return positionNormalized;
	}
	
	public void positionNormalized(Vector2f positionNormalized){
		this.positionNormalized = positionNormalized;
	}
	
	public void positionNormalized(float x, float y){
		positionNormalized(new Vector2f(x, y));
	}
	
	public Vector2f position(){
		return new Vector2f(data.getScreenX(), data.getScreenY());
	}
	
	public void position(Vector2f p){
		positionNormalized = p.div(Bdx.display.size());
	}
		
	public void position(float x, float y){
		position(new Vector2f(x, y));
	}
	
	public Matrix3f orientation(){
		return camera.orientation();
	}
	
	public Vector2f position(Vector3f p){
		Vector3 out = data.project(new Vector3(p.x, p.y, p.z));
		return new Vector2f(Math.round(out.x), Math.round(Bdx.display.height() - out.y));
	}
	
	public Vector2f positionNormalized(Vector3f p){
		return position(p).div(Bdx.display.size());
	}
	
	public Vector3f[] worldCoords(Vector2f p){
		Ray pr = camera.data.getPickRay(p.x, Bdx.display.height() - p.y, data.getScreenX(), data.getScreenY(), data.getScreenWidth(), data.getScreenHeight());
		Vector3f position = new Vector3f(pr.origin.x, pr.origin.y, pr.origin.z);
		Vector3f direction = new Vector3f(pr.direction.x, pr.direction.y, pr.direction.z);
		return new Vector3f[]{position, direction};
	}
	
	public Vector3f[] worldCoordsNormalized(Vector2f p){
		return worldCoords(p.div(Bdx.display.size()));
	}
	
	public void apply(){
		camera.update();
		data.apply();
	}
	
	public void update(float displayWidth, float displayHeight){
		Vector2f resolution = resolution();
		data.setWorldSize(resolution.x, resolution.y);
		int x = Math.round(positionNormalized.x * displayWidth);
		int y = Math.round(positionNormalized.y * displayHeight);
		int w;
		int h;
		if (type == Type.SCREEN){
			w = Math.round(sizeNormalized.x * resolution.x);
			h = Math.round(sizeNormalized.y * resolution.y);
			((ScreenViewport)data).setUnitsPerPixel(1 / sizeNormalized.x);
		}else{
			w = Math.round(sizeNormalized.x * displayWidth);
			h = Math.round(sizeNormalized.y * displayHeight);
			if (type == Type.LETTERBOX){
				float sRatio = sizeNormalized.x / sizeNormalized.y;
				float rRatio = resolution.x / resolution.y;
				if (sRatio < rRatio){
					int h2 = Math.round(w / rRatio);
					y += (h - h2) / 2;
					h = h2;
				}else if (sRatio > rRatio){
					int w2 = Math.round(h * rRatio);
					x += (w - w2) / 2;
					w = w2;
				}
			}
		}
		camera.update();
		data.setScreenSize(w, h);
		data.update(w, h);
		data.setScreenX(x);
		data.setScreenY(y);
		data.apply();
	}
	
	public void update(){
		update(Bdx.display.width(), Bdx.display.height());
	}
	
	public void drawLine(Vector3f start, Vector3f end, Color color){
		ArrayList<Object> commands = new ArrayList<Object>();
		commands.add("drawLine");
		commands.add(color);
		commands.add(start);
		commands.add(end);
		drawCommands.add(commands);
	}

	public void drawPoint(Vector3f point, Color color){
		ArrayList<Object> commands = new ArrayList<Object>();
		commands.add("drawPoint");
		commands.add(color);
		commands.add(point);
		drawCommands.add(commands);
	}
	
	public void drawBox(Vector3f point, Vector3f dimensions, Color color){
		ArrayList<Object> commands = new ArrayList<Object>();
		commands.add("drawBox");
		commands.add(color);
		commands.add(point.minus(new Vector3f(dimensions.x * 0.5f, dimensions.y * 0.5f, -dimensions.z * 0.5f)));
		commands.add(dimensions);
		drawCommands.add(commands);
	}
	
	public void executeDrawCommands(){
		String func;
		Color color;
		Vector3f start;
		Vector3f other;
		
		for (ArrayList<Object> commands : drawCommands){
			func = (String) commands.get(0);
			color = (Color) commands.get(1);
			start = (Vector3f) commands.get(2);
			
			shapeRenderer.setProjectionMatrix(camera.data.combined);
			shapeRenderer.setColor(color);
			
			if (func.equals("drawLine")){
				other = (Vector3f) commands.get(3);
				shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
				shapeRenderer.line(start.x, start.y, start.z, other.x, other.y, other.z);
				shapeRenderer.end();
			}else if (func.equals("drawPoint")){
				shapeRenderer.begin(ShapeRenderer.ShapeType.Point);
				shapeRenderer.point(start.x, start.y, start.z);
				shapeRenderer.end();
			}else{ // Draw Box
				other = (Vector3f) commands.get(3);
				shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
				shapeRenderer.box(start.x, start.y, start.z, other.x, other.y, other.z);
				shapeRenderer.end();
			}
		}
		
		drawCommands.clear();
	}
	
	public String toString(){
		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());
	}

}
