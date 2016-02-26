package com.nilunder.bdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

import com.bulletphysics.linearmath.Transform;

import javax.vecmath.*;

public class Camera extends GameObject{

	PerspectiveCamera data;
	public Type type;

	public enum Type {
		PERSPECTIVE,
		ORTHOGRAPHIC
	}

	public Camera(){
		data = new PerspectiveCamera();
	}

	public Matrix4f projection(){
		Matrix4f m = new Matrix4f();
		m.set(data.projection.getValues());
		m.transpose();
		return m;
	}

	public void projection(Matrix4f mat){
		Transform t = new Transform(mat);
		float[] m = new float[16];
		t.getOpenGLMatrix(m);
		m[11] = mat.m32;
		m[15] = mat.m33;
		data.projection.set(m);
	}

	public float fov(){
		Matrix4f p = projection();
		float fov;
		if (type == Type.ORTHOGRAPHIC){
			fov = 2/p.m11;
		}else{
			fov = (float)(Math.atan(1/p.m11) * 2);
		}
		return fov;
	}

	public void fov(float fov){
		fov = fov < 0 ? 0 : fov;
		Matrix4f p = projection();
		float w = Gdx.app.getGraphics().getWidth();
		float h = Gdx.app.getGraphics().getHeight();

		if (type == Type.ORTHOGRAPHIC){
			p.m00 = 2/fov/(w/h);
			p.m11 = 2/fov;
		}else{
			float pi = 3.141519f;
			fov = fov > pi ? pi : fov;
			float atan = 1/(float)Math.tan(fov / 2);
			p.m00 = atan / (w/h);
			p.m11 = atan;
		}
		projection(p);
	}

	public float near(){
		Matrix4f proj = projection();
		if (type == Type.PERSPECTIVE)
			return (proj.m23 / (proj.m22 - 1));
		else
			return (1 + proj.m23) / proj.m22;
	}

	public float far(){
		Matrix4f proj = projection();
		if (type == Type.PERSPECTIVE)
			return (proj.m23 / (proj.m22 + 1));
		else
			return -(1 - proj.m23) / proj.m22;
	}

	public Vector2f screenPosition(Vector3f worldPosition){

		Vector3 out = data.project(new Vector3(worldPosition.x, worldPosition.y, worldPosition.z));

		return new Vector2f(out.x / Gdx.app.getGraphics().getWidth(), out.y / Gdx.app.getGraphics().getHeight());

	}

}
