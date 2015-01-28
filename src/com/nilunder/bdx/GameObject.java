package com.nilunder.bdx;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
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
	public ArrayListNamed<GameObject> touchingObjects;
	public ArrayListNamed<GameObject> touchingObjectsLast;
	public ArrayList<PersistentManifold> contactManifolds;
	public ModelInstance modelInstance;
	public RigidBody body;
	
	public HashMap<String, JsonValue> props;
	
	public ArrayListNamed<GameObject> children;
	
	public ArrayList<Component> components;
	
	public Scene scene;

	private GameObject parent;
	private Matrix4f localTransform;
	private Vector3f localScale;
	private boolean visible;
	private boolean valid;
	private Model uniqueModel;
	
	
	public GameObject() {
		touchingObjects = new ArrayListNamed<GameObject>();
		touchingObjectsLast = new ArrayListNamed<GameObject>();
		contactManifolds = new ArrayList<PersistentManifold>();
		components = new ArrayList<Component>();
		children = new ArrayListNamed<GameObject>();
		valid = true;
	}


	public String name(){
		return name;
	}
	
	public void init(){
	}
	
	public void main(){
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

	public void move(Vector3f delta){
		position(position().plus(delta));
	}

	public void move(float x, float y, float z){
		Vector3f delta = new Vector3f(x, y, z);
		move(delta);
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
	
	public boolean touching(){
		return !touchingObjects.isEmpty();
	}
	
	public boolean touching(String name){
		return touchingObjects.get(name) != null;
	}
	
	public boolean hit(){
		for (GameObject g: touchingObjects){
			if (!touchingObjectsLast.contains(g)){
				return true;
			}
		}
		return false;
	}
	
	public boolean hit(String name){
		return touchingObjects.get(name) != null && 
			touchingObjectsLast.get(name) == null;
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

	public boolean visible(){
		return visible;
	}

	public void visible(boolean visible){

		for (GameObject g : children){
			g.visible(visible);
		}
		visibleNoChildren(visible);
	}

	public void visibleNoChildren(boolean visible){
		this.visible = visible;
	}


	public void end(){
		for (GameObject g : new ArrayList<GameObject>(children)){
			g.end();
		}
		endNoChildren();
	}
	
	public void endNoChildren(){
		parent(null);
		valid = false;
		if (uniqueModel != null)
			uniqueModel.dispose();
		scene.remove(this);
	}

	public boolean valid(){
		return valid;
	}
	
	public void scale(float x, float y, float z, boolean updateLocal){
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
		Vector3f ps = scale();
		Matrix4f pt = transform();
		Matrix4f ms = new Matrix4f();
		Matrix3f rs = new Matrix3f();
		Vector4f es = new Vector4f();

		for (GameObject c : children){
			ms.setIdentity();
			ms.m00 = ps.x; ms.m11 = ps.y; ms.m22 = ps.z;
			pt.mul(ms);
			ms.mul(pt, c.localTransform);
			ms.getColumn(0, es); ps.x = es.length();
			ms.getColumn(1, es); ps.y = es.length();
			ms.getColumn(2, es); ps.z = es.length();
			c.scale(ps.mul(c.localScale), false);
			c.transform().getRotationScale(rs);
			ms.setRotationScale(rs);
			c.transform(ms, false);
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

	public Vector3f dimensions(){
		Vector3f min = new Vector3f(0, 0, 0);
		Vector3f max = new Vector3f(0, 0, 0);
		body.getAabb(min, max);
		return max.minus(min);
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

	public Vector4f color(){

		ColorAttribute ca = (ColorAttribute) modelInstance.materials.get(0).get(ColorAttribute.Diffuse);

		return new Vector4f(ca.color.r, ca.color.g, ca.color.b, ca.color.a);

	}

	public void color(float r, float g, float b, float a){

		for (Material mat : modelInstance.materials){

			ColorAttribute ca = (ColorAttribute) mat.get(ColorAttribute.Diffuse);

			ca.color.set(r, g, b, a);

		}
	}

	public void color(float r, float g, float b){
		color(r, g, b, 1);
	}

	public void color(Vector4f color){
		color(color.x, color.y, color.z, color.w);
	}

	public int[] blendMode(){

		BlendingAttribute ba = (BlendingAttribute) modelInstance.materials.first().get(BlendingAttribute.Type);

		int[] a = {ba.sourceFunction, ba.destFunction};

		return a;

	}

	public void blendMode(int src, int dest){

		for (Material mat : modelInstance.materials){

			BlendingAttribute ba = (BlendingAttribute) mat.get(BlendingAttribute.Type);

			if (ba == null){
				ba = new BlendingAttribute();
				mat.set(ba);
			}

			ba.sourceFunction = src;
			ba.destFunction = dest;

		}

	}

	public void useUniqueMesh(){
		String modelName = modelInstance.model.meshParts.get(0).id;
		JsonValue modelData = scene.json.get("models").get(modelName);
		uniqueModel = scene.createModel(modelData);
		ModelInstance mi = new ModelInstance(uniqueModel);
		mi.transform.set(modelInstance.transform);
		modelInstance = mi;
	}

}
