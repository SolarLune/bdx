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
import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;

import com.nilunder.bdx.utils.*;

public class GameObject implements Named{
	public JsonValue json;
	
	public String name;
	public ArrayListNamed<GameObject> touchingObjects;
	public ArrayListNamed<GameObject> touchingObjectsLast;
	public ArrayList<PersistentManifold> contactManifolds;
	public ModelInstance modelInstance;
	public RigidBody body;
	
	public HashMap<String, JsonValue> props;
	
	public ArrayListNamed<GameObject> children;
	
	public ArrayListNamed<Component> components;
	
	public Scene scene;

	private GameObject parent;
	private Matrix4f localTransform;
	private Vector3f localScale;
	private boolean visible;
	private boolean valid;
	private Model uniqueModel;
	private float lastNonZeroMass;

	
	public GameObject() {
		touchingObjects = new ArrayListNamed<GameObject>();
		touchingObjectsLast = new ArrayListNamed<GameObject>();
		contactManifolds = new ArrayList<PersistentManifold>();
		components = new ArrayListNamed<Component>();
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

	public void onEnd(){
	}
	
	public GameObject parent(){
		return parent;
	}
	
	public void parent(GameObject p){
		if (parent != null){
			parent.children.remove(this);

			if (parent.compoundShape() != null)
				parent.compoundShape().removeChildShape(body.getCollisionShape());

		}
		
		parent = p;

		if (parent != null){
			
			parent.children.add(this);
			
			updateLocalTransform();
			updateLocalScale();

			if (parent.compoundShape() != null)
				parent.compoundShape().addChildShape(new Transform(localTransform), body.getCollisionShape());

			dynamic(false);
		}else if (!json.get("physics").get("body_type").asString().equals("NO_COLLISION")){
			dynamic(true);
		}
		
	}

	private CompoundShape compoundShape(){
		if (body.getCollisionShape() instanceof CompoundShape)
			return (CompoundShape) body.getCollisionShape();
		return null;
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

		Vector3f v = new Vector3f();
		for (int i = 0; i < 3; ++i){
		    t.basis.getColumn(i, v);
		    v.normalize();
		    t.basis.setColumn(i, v);
		}
		
		Matrix4f m = new Matrix4f();
		t.getMatrix(m);
		
		return m;
	}
	
	public void transform(Matrix4f mat){
		transform(mat, true);
	}
	
	public void updateChildTransforms(){
		Matrix4f pt = transform();
		Matrix4f ct = new Matrix4f();
		Matrix4f ms = new Matrix4f(); ms.setIdentity();
		Vector3f ps = scale();
		ms.m00 = ps.x; ms.m11 = ps.y; ms.m22 = ps.z;
		pt.mul(ms);

		for (GameObject c : children){
			ct.mul(pt, c.localTransform);
			c.transform(ct, false);
		}

	}

	public void transform(Matrix4f mat, boolean updateLocal){
		body.activate(true);
		
		Transform t = new Transform();
		t.set(mat);
		
		Vector3f v = new Vector3f();
		for (int i = 0; i < 3; ++i){
		    t.basis.getColumn(i, v);
		    v.normalize();
		    t.basis.setColumn(i, v);
		}

		body.setWorldTransform(t);

		// required for static objects:
		body.getMotionState().setWorldTransform(t); 
		if (body.isInWorld() && body.isStaticOrKinematicObject()){
			scene.world.updateSingleAabb(body);
			for (GameObject g : touchingObjects)
				g.body.activate();
		}
		//
		
		updateChildTransforms();

		if (parent != null && updateLocal){
			updateLocalTransform();
		}
	}

	private void updateLocalTransform(){
		localTransform = parent.transform();
		Matrix4f ms = new Matrix4f(); ms.setIdentity();
		Vector3f ps = scale();
		ms.m00 = ps.x; ms.m11 = ps.y; ms.m22 = ps.z;
		localTransform.mul(ms);
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
	
	public void applyForceLocal(Vector3f vec){
		applyForce(orientation().mult(vec));
	}
	
	public void applyForceLocal(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		applyForceLocal(v);
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
	
	public void velocityLocal(Vector3f vec){
		velocity(orientation().mult(vec));
	}
	
	public void velocityLocal(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		velocityLocal(v);
	}
	
	public Vector3f velocityLocal(){
		Vector3f v = new Vector3f();
		body.getLinearVelocity(v);
		Matrix3f invOri = orientation();
		invOri.invert();
		return invOri.mult(v);
	}
	
	public void angularVelocity(Vector3f vec){
		body.activate();
		body.setAngularVelocity(vec);
	}
	
	public void angularVelocity(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		angularVelocity(v);
	}
	
	public Vector3f angularVelocity(){
		Vector3f v = new Vector3f();
		body.getAngularVelocity(v);
		return v;
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

	public void collisionGroup(short group)
	{
		short mask = body.getBroadphaseProxy().collisionFilterMask;

		scene.world.removeRigidBody(body);
		scene.world.addRigidBody(body, group, mask);
	}

	public short collisionGroup()
	{
		return body.getBroadphaseProxy().collisionFilterGroup;
	}

	public void collisionMask(short mask)
	{
		short group = body.getBroadphaseProxy().collisionFilterGroup;

		scene.world.removeRigidBody(body);
		scene.world.addRigidBody(body, group, mask);
	}

	public short collisionMask()
	{
		return body.getBroadphaseProxy().collisionFilterMask;
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
	
	public boolean ghost(){
		int noContact = body.getCollisionFlags() & CollisionFlags.NO_CONTACT_RESPONSE;
		return noContact != 0 ? true : false;
	}

	public void ghost(boolean ghost){
		for (GameObject g : children){
			g.ghost(ghost);
		}
		ghostNoChildren(ghost);
	}

	public void ghostNoChildren(boolean ghost){
		int flags = body.getCollisionFlags();
		int noContact = CollisionFlags.NO_CONTACT_RESPONSE;
		
		if (ghost)
			flags |= noContact;
		else
			flags &= ~noContact;
			
		body.setCollisionFlags(flags);
	}

	public void end(){
		for (GameObject g : new ArrayList<GameObject>(children)){
			g.end();
		}
		endNoChildren();
	}
	
	public void endNoChildren(){
		onEnd();
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
		body.activate(true);
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
		Matrix4f ct = new Matrix4f();
		Matrix4f ms = new Matrix4f(); ms.setIdentity();
		ms.m00 = ps.x; ms.m11 = ps.y; ms.m22 = ps.z;
		pt.mul(ms);

		for (GameObject c : children){
			c.scale(scale().mul(c.localScale), false);
			ct.mul(pt, c.localTransform);
			c.transform(ct, false);
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
	
	public Vector3f axis(String axisName){
		int axis = "XYZ".indexOf(axisName.charAt(axisName.length() - 1)); 
		Vector3f v = new Vector3f();
		orientation().getColumn(axis, v);
		if (axisName.charAt(0) == '-')
			v.negate();
		return v;
	}

	public Vector3f axis(int axis){
		return axis(String.valueOf("XYZ".charAt(axis)));
	}
	
	public void alignAxisToVec(String axisName, Vector3f vec){
		Vector3f alignAxis = axis(axisName);
		Vector3f rotAxis = new Vector3f();
		rotAxis.cross(alignAxis, vec);
		if (rotAxis.length() == 0)
			rotAxis = axis(("XYZ".indexOf(axisName) + 1) % 3);
		Matrix3f rotMatrix = Matrix3f.rotation(rotAxis, alignAxis.angle(vec));
		Matrix3f ori = orientation();
		rotMatrix.mul(ori);
		orientation(rotMatrix);
	}

	public void alignAxisToVec(int axis, Vector3f vec){
		alignAxisToVec(String.valueOf("XYZ".charAt(axis)), vec);
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

	public void useUniqueModel(){
		String modelName = modelInstance.model.meshParts.get(0).id;
		JsonValue modelData = scene.json.get("models").get(modelName);
		uniqueModel = scene.createModel(modelData);
		ModelInstance mi = new ModelInstance(uniqueModel);
		mi.transform.set(modelInstance.transform);
		modelInstance = mi;
	}

	public void replaceModel(String modelName, boolean updateVisual, boolean updatePhysics){
		if (modelName.equals(modelInstance.model.meshParts.get(0).id))
			return;
		Model model = null;
		for (Scene sce : Bdx.scenes){
			if (sce.models.containsKey(modelName)){
				model = sce.models.get(modelName);
				break;
			}
		}
		if (model == null) throw new RuntimeException("No model found with name: '" + modelName + "'");
		Matrix4 trans = modelInstance.transform;
		if (updateVisual){
			ModelInstance mi = new ModelInstance(model);
			mi.transform.set(trans);
			modelInstance = mi;
		}
		if (updatePhysics && body.isInWorld()){
			Vector3f vel = velocity();
			Vector3f angVel = angularVelocity();
			scene.world.removeRigidBody(body);
			body.destroy();
			JsonValue physics = json.get("physics");
			body = Bullet.makeBody(model.meshes.first(), trans.getValues(), physics);
			body.setUserPointer(this);
			mass(lastNonZeroMass);
			body.updateInertiaTensor();
			if (dynamic()){
				velocity(vel);
				angularVelocity(angVel);
			}
			scene.world.addRigidBody(body);
			scene.world.updateSingleAabb(body);
		}
	}

	public void replaceModel(String modelName){
		replaceModel(modelName, true, false);
	}

	public String toString(){

		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());

	}

	public void dynamic(boolean dynamic){
		if (dynamic){
			scene.world.removeRigidBody(body);
			mass(lastNonZeroMass);
			scene.world.addRigidBody(body);
			body.activate();
		}else{
			lastNonZeroMass = mass();
			mass(0);
		}
	}

	public boolean dynamic(){
		return !body.isStaticOrKinematicObject();
	}
	
	public float mass(){
		return 1 / body.getInvMass();
	}
	
	public void mass(float mass){
		Vector3f inertia = new Vector3f();
		body.getCollisionShape().calculateLocalInertia(mass, inertia);
		body.setMassProps(mass, inertia);
	}

}
