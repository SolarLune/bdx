package com.nilunder.bdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.Vector3;

import com.bulletphysics.linearmath.Transform;

import com.nilunder.bdx.gl.Viewport;
import com.nilunder.bdx.gl.RenderBuffer;
import com.nilunder.bdx.utils.ArrayListNamed;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4f;

public class Camera extends GameObject{
	
	public enum Type{
		PERSPECTIVE,
		ORTHOGRAPHIC
	}
	
	private Vector2f resolution;
	
	public Type type;
	public com.badlogic.gdx.graphics.Camera data;
	public boolean renderingToTexture;
	public RenderBuffer renderBuffer;
	public ArrayListNamed<GameObject> ignoreObjects;
	
	{
		resolution = new Vector2f();
		ignoreObjects = new ArrayListNamed<GameObject>();
	}

	public void initData(Type type){
		this.type = type;
		if (type == Type.PERSPECTIVE){
			data = new PerspectiveCamera();
		}else{
			data = new OrthographicCamera();
		}		
	}
	
	public void projection(Matrix4f mat){
		Transform t = new Transform(mat);
		float[] m = new float[16];
		t.getOpenGLMatrix(m);
		data.projection.set(m);
	}
	
	public Matrix4f projection(){
		Matrix4f m = new Matrix4f();
		m.set(data.projection.getValues());
		m.transpose();
		return m;
	}
	
	public void near(float near){
		data.near = near;
	}
	
	public float near(){
		return data.near;
	}
	
	public void far(float far){
		data.far = far;
	}
	
	public float far(){
		return data.far;
  	}
	
	public void width(int w){
		data.viewportWidth = Math.max(w, 1);		// A width of 0 crashes BDX
	}
	
	public int width(){
		return Math.round(data.viewportWidth);
	}
	
	public void height(int h){
		data.viewportHeight = Math.max(h, 1);
	}
	
	public int height(){
		return Math.round(data.viewportHeight);
	}
	
	public Vector2f size(){
		return new Vector2f(data.viewportWidth, data.viewportHeight);
	}
	
	public void size(Vector2f size){
		size(Math.round(size.x), Math.round(size.y));
	}
	
	public void size(int width, int height){
		data.viewportWidth = width;
		data.viewportHeight = height;
	}
	
	public Vector2f resolution(){
		return new Vector2f(resolution);
	}
	
	public void resolution(int width, int height){
		resolution.x = width;
		resolution.y = height;
		updateRenderBuffer();
	}
	
	public void resolution(Vector2f resolution){
		resolution(Math.round(resolution.x), Math.round(resolution.y));
	}
	
	public void fov(float fov){
		((PerspectiveCamera)data).fieldOfView = (float)Math.toDegrees(fov);
	}

	public float fov(){
		Matrix4f p = projection();
		float fov;
		if (type == Type.PERSPECTIVE){
			fov = (float)(Math.atan(1/p.m11)*2);
		}else{
			fov = 2/p.m11;
		}
		return fov;
	}	
	
	public void zoom(float zoom){
		((OrthographicCamera)data).zoom = zoom / width();
	}
	
	public float zoom(){
		return ((OrthographicCamera)data).zoom * width();
	}
	
	public Vector2f screenPosition(Vector3f p){
		Viewport vp = scene.viewport;
		Vector3 out = data.project(new Vector3(p.x, p.y, p.z), vp.x, vp.y, vp.w, vp.h);
		return new Vector2f(Math.round(out.x), Math.round(out.y));
	}
	
	public Vector2f screenPositionNormalized(Vector3f p){
		return screenPosition(p).div(Bdx.display.size());
	}
	
	public Vector3f[] rayData(Vector2f p){
		Viewport vp = scene.viewport;
		Ray pr = data.getPickRay(p.x, Bdx.display.height() - p.y, vp.x, vp.y, vp.w, vp.h);
		return new Vector3f[]{new Vector3f(pr.origin.x, pr.origin.y, pr.origin.z), new Vector3f(pr.direction.x, pr.direction.y, pr.direction.z)};
	}
	
	public Vector3f[] rayDataNormalized(Vector2f p){
		return rayData(p.div(Bdx.display.size()));
	}
	
	public void update(){
		Transform t = new Transform();
		body.getWorldTransform(t);
		data.position.set(t.origin.x, t.origin.y, t.origin.z);
		Vector3f axis = axis("-Z");
		data.direction.set(axis.x, axis.y, axis.z);
		axis = axis("Y");
		data.up.set(axis.x, axis.y, axis.z);
		data.update();
	}
	
	public void initRenderBuffer(){
		renderBuffer = new RenderBuffer(null, Math.round(resolution.x), Math.round(resolution.y));
	}
	
	public void updateRenderBuffer(){
		if (renderBuffer != null)
			renderBuffer.dispose();
		initRenderBuffer();
	}
	
	public TextureRegion texture(){
		return renderBuffer != null ? renderBuffer.region : null;
	}
	
}
