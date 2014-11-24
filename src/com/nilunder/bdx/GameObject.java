package com.nilunder.bdx;

import java.util.ArrayList;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonValue;

import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;

import com.nilunder.bdx.utils.*;

public class GameObject implements Named{
	public JsonValue _json;
	
	public String name;
	public boolean visible;
	public ArrayListNamed<GameObject> hitObjects;
	public ArrayListNamed<GameObject> hitObjectsLast;
	public ArrayList<PersistentManifold> contactManifolds;
	public ModelInstance modelInstance;
	public RigidBody body;
	
	public ArrayListNamed<GameObject> children;
	
	public ArrayList<Component> components;
	
	public Scene scene;

	private GameObject parent;
	private Matrix4f localTransform;
	private Vector3f localScale;
	
	
	public GameObject() {
		hitObjects = new ArrayListNamed<GameObject>();
		hitObjectsLast = new ArrayListNamed<GameObject>();
		contactManifolds = new ArrayList<PersistentManifold>();
		components = new ArrayList<Component>();
		children = new ArrayListNamed<GameObject>();
	}

	public String name(){
		return name;
	}
	
	public void init(){
	}
	
	public void main(){
		for (Component c : components){
			if (c.state != null)
				c.state.main();
		}
	}
	
	public GameObject parent(){
		return parent;
	}
	
	public void parent(GameObject p){
		if (parent != null){
			parent.children.remove(this);
		}
		
		parent = p;

		if (parent != null){
			
			parent.children.add(this);
			
			updateLocalTransform();
			updateLocalScale();

		}
		
	}
	
	public Vector3f position(){
		Transform t = new Transform();
		body.getWorldTransform(t);
		
		return new Vector3f(t.origin);
	}
	
	public void position(Vector3f vec){
		body.activate();
		
		Matrix4f t = transform();
		t.setTranslation(vec);
		
		transform(t);
		
	}
	
	public void position(float x, float y, float z){
		position(new Vector3f(x, y, z));
	}
	
	public Matrix3f orientation(){
		Matrix4f t = transform();
		Matrix3f ori = new Matrix3f();
		t.getRotationScale(ori);
		return ori;
	}
	
	public void orientation(Matrix3f ori){
		Matrix4f t = transform();
		t.setRotation(ori);
		transform(t);
	}
	
	public void rotate(float x, float y, float z){
		Matrix3f ori = orientation();
		
		Matrix3f rot = new Matrix3f();
		MatrixUtil.setEulerZYX(rot, x, y, z);

		rot.mul(ori);
		
		orientation(rot);
	}
	
	public void rotate(Vector3f rot){
		rotate(rot.x, rot.y, rot.z);
	}
	
	public void rotateLocal(float x, float y, float z){
		Matrix3f ori = orientation();
		
		Matrix3f rot = new Matrix3f();
		MatrixUtil.setEulerZYX(rot, x, y, z);

		ori.mul(rot);
		
		orientation(ori);
	}
	
	public Matrix4f transform(){
		Transform t = new Transform();
		body.getWorldTransform(t);

		Bullet.normalizeBasis(t.basis);
		
		Matrix4f m = new Matrix4f();
		t.getMatrix(m);
		
		return m;
	}
	
	public void transform(Matrix4f mat){
		transform(mat, true);
	}
	
	public void updateChildTransforms(){
		for (GameObject c : children){
			Matrix4f ct = transform();
			ct.mul(c.localTransform);
			c.transform(ct, false);
		}
	}

	public void transform(Matrix4f mat, boolean updateLocal){
		body.activate();
		
		Transform t = new Transform();
		t.set(mat);
		
		body.setWorldTransform(t);
		body.getMotionState().setWorldTransform(t); // required for static objects
		
		updateChildTransforms();

		if (parent != null && updateLocal){
			updateLocalTransform();
		}
	}


	
	private void updateLocalTransform(){
		localTransform = parent.transform();
		localTransform.invert();
		localTransform.mul(transform());
	}

	public void applyForce(Vector3f vec){
		body.activate();
		body.applyCentralForce(vec);
	}
	
	public void applyForce(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		applyForce(v);
	}
	
	public void velocity(Vector3f vec){
		body.activate();
		body.setLinearVelocity(vec);
	}
	
	public void velocity(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		velocity(v);
	}
	
	public Vector3f velocity(){
		Vector3f v = new Vector3f();
		body.getLinearVelocity(v);
		return v;
	}
	
	public void angularVelocity(Vector3f vec){
		body.activate();
		body.setAngularVelocity(vec);
	}
	
	public void angularVelocity(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		angularVelocity(v);
	}
	
	public boolean touching(String name){
		return hitObjects.get(name) != null;
	}
	
	public boolean hit(String name){
		return hitObjects.get(name) != null && 
			hitObjectsLast.get(name) == null;
	}
	
	public float reactionForce(){
		float force = 0;
		int totalContacts = 0;

		for (PersistentManifold m : contactManifolds){

			int numContacts = m.getNumContacts();
			totalContacts += numContacts;

			for (int i = 0; i < numContacts; ++i){
				ManifoldPoint p = m.getContactPoint(i);
				force += p.appliedImpulse;
			}

		}

		return totalContacts != 0 ? force / totalContacts : 0;
	}
	
	public void end(){
		for (GameObject g : children){
			g.end();
		}
		scene.remove(this);
	}
	
	public void endNoChildren(){
		scene.remove(this);
	}
	
	public void scale(float x, float y, float z, boolean updateLocal){
		if (modelInstance == null) return;

		// Set unit scale
		Matrix4 t = modelInstance.transform;
		Matrix4 mat_scale = new Matrix4();
		Vector3 s = new Vector3();
		t.getScale(s);
		mat_scale.scl(1/s.x, 1/s.y, 1/s.z);
		t.mul(mat_scale);

		// Set target scale
		mat_scale.idt(); mat_scale.scl(x, y, z);
		t.mul(mat_scale);

		// Relevant bullet body update
		CollisionShape cs = body.getCollisionShape();
		cs.setLocalScaling(new Vector3f(x, y, z));
		if (body.isInWorld() && body.isStaticOrKinematicObject())
			scene.world.updateSingleAabb(body);

		// Child propagation
		for (GameObject c : children){
			c.scale(scale().mul(c.localScale), false);
		}

		if (parent != null && updateLocal){
			updateLocalScale();
		}
	}

	private void updateLocalScale(){
		localScale = scale().div(parent.scale());
	}

	public void scale(Vector3f s, boolean updateLocal){
		scale(s.x, s.y, s.z, updateLocal);
	}

	public void scale(float x, float y, float z){
		scale(x, y, z, true);
	}

	public void scale(Vector3f s){
		scale(s.x, s.y, s.z);
	}

	public void scale(float s){
		scale(s, s, s);
	}

	public Vector3f scale(){
		Vector3f s = new Vector3f();
		CollisionShape cs = body.getCollisionShape();
		cs.getLocalScaling(s);
		return s;
	}
	
	public Vector3f axis(int axis){
		Vector3f v = new Vector3f();
		orientation().getColumn(axis, v);
		return v;
	}
	
	public void alignAxisToVec(int axis, Vector3f vec){
		Vector3f alignAxis = axis(axis);
		Vector3f rotAxis = new Vector3f();
		rotAxis.cross(alignAxis, vec);
		Matrix3f rotMatrix = Bullet.rotMatrix(rotAxis, Bullet.vecAngle(alignAxis, vec));
		Matrix3f ori = orientation();
		rotMatrix.mul(ori);
		orientation(rotMatrix);
	}
}
