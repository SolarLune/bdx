package com.nilunder.bdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;

import com.bulletphysics.linearmath.Transform;

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
	
	public void initData(Type type){
		this.type = type;
		if (type == Type.PERSPECTIVE){
			data = new PerspectiveCamera();
		}else{
			data = new OrthographicCamera();
		}
		ignoreObjects = new ArrayListNamed<GameObject>();
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
	
	public float width(){
		return data.viewportWidth;
	}
	
	public float height(){
		return data.viewportHeight;
	}
	
	public Vector2f resolution(){
		return resolution;
	}
	
	public void resolution(Vector2f resolution){
		this.resolution = resolution;
		data.viewportWidth = resolution.x;
		data.viewportHeight = resolution.y;
	}
	
	public void resolution(float width, float height){
		resolution(new Vector2f(width, height));
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
	
	public void update(){
		Transform t = new Transform();
		body.getWorldTransform(t);
		data.position.set(t.origin.x, t.origin.y, t.origin.z);
		Vector3f axis = axis("-Z");
		data.direction.set(axis.x, axis.y, axis.z);
		axis = axis("Y");
		data.up.set(axis.x, axis.y, axis.z);
	}
	
	public void updateRenderBuffer(){
		if (renderBuffer == null || ((int) resolution.x != renderBuffer.getWidth() || (int) resolution.y != renderBuffer.getHeight())){
			if (renderBuffer != null)
				renderBuffer.dispose();
			renderBuffer = new RenderBuffer(null, (int) resolution.x, (int) resolution.y);
		}
	}
	
	public TextureRegion texture(){
		return renderBuffer != null ? renderBuffer.region : null;
	}
	
}
