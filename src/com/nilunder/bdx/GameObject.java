package com.nilunder.bdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.collision.BoundingBox;
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

import com.nilunder.bdx.gl.Material;
import com.nilunder.bdx.gl.Mesh;
import com.nilunder.bdx.utils.*;

public class GameObject implements Named{
	public JsonValue json;
	
	public String name;
	public ArrayListGameObject touchingObjects;
	public ArrayListGameObject touchingObjectsLast;
	public ArrayList<PersistentManifold> contactManifolds;
	public ModelInstance modelInstance;
	public RigidBody body;
	public BodyType currBodyType;
	public BoundsType currBoundsType;
	public Matrix4f transform;
	public Vector3f flipState;
	public Vector3f origin;
	public Vector3f dimensionsNoScale;
	
	public HashMap<String, JsonValue> props;
	public boolean frustumCulling;
	
	public ArrayListGameObject children;
	
	public ArrayListNamed<Component> components;
	
	public Scene scene;
	
	private GameObject parent;
	private Matrix4f localTransform;
	private boolean visible;
	private boolean valid;
	public boolean initialized;
	public float logicFrequency;
	public float logicCounter;
	private Mesh mesh;
	private static java.util.Random logicCounterRandom;
	
	public enum BodyType {
		NO_COLLISION,
		STATIC,
		SENSOR,
		DYNAMIC,
		RIGID_BODY
	}
	
	public enum BoundsType {
		TRIANGLE_MESH,
		CONVEX_HULL,
		SPHERE,
		BOX,
		CYLINDER,
		CAPSULE,
		CONE
	}

	public class ArrayListGameObject extends ArrayListNamed<GameObject> {

		public GameObject getByProperty(String propName){
			for (GameObject t : this) {
				if (t.props.containsKey(propName)) {
					return t;
				}
			}
			return null;
			
		}
		
		public GameObject getByComponent(String compName){
			for (GameObject t : this) {
				if (t.components.get(compName) != null) {
					return t;
				}
			}
			return null;
		}

		public ArrayListGameObject getObjectsByProperty(String propName) {
			ArrayListGameObject ret = new ArrayListGameObject();
			for (GameObject t : this) {
				if (t.props.containsKey(propName))
					ret.add(t);
			}
			return ret;
		}

		public ArrayListGameObject getObjectsByComponent(String compName) {
			ArrayListGameObject ret = new ArrayListGameObject();
			for (GameObject t : this) {
				if (t.components.get(compName) != null)
					ret.add(t);
			}
			return ret;
		}
		
	}

	public GameObject() {
		touchingObjects = new ArrayListGameObject();
		touchingObjectsLast = new ArrayListGameObject();
		contactManifolds = new ArrayList<PersistentManifold>();
		components = new ArrayListNamed<Component>();
		children = new ArrayListGameObject();
		transform = new Matrix4f();
		localTransform = Matrix4f.identity();
		flipState = new Vector3f();
		valid = true;
		logicFrequency = Bdx.TICK_RATE;
		if (logicCounterRandom == null)
			logicCounterRandom = new java.util.Random();
		logicCounter = 1 + logicCounterRandom.nextFloat();
		frustumCulling = true;
	}

	public String name(){
		return name;
	}
	
	public void init(){}
	
	public void main(){}

	public void onEnd(){}
	
	public GameObject parent(){
		return parent;
	}
	
	public void parent(GameObject p){
		parent(p, true);
	}

	public void parent(GameObject p, boolean compound){
		CompoundShape compShapeOld = null;

		if (parent != null){
			parent.children.remove(this);
			localTransform.setIdentity();

			if (compound){
				compShapeOld = parent.compoundShape();
				if (compShapeOld != null){
					scene.world.removeRigidBody(parent.body);
					compShapeOld.removeChildShape(body.getCollisionShape());
					scene.world.addRigidBody(parent.body);
				}
			}

		}else if (p == null){
			return;
		}

		parent = p;

		if (parent != null){

			parent.children.add(this);

			updateLocalTransform();

			if (compound){
				CompoundShape compShape = parent.compoundShape();
				if (compShape != null){
					scene.world.removeRigidBody(body);
					compShape.addChildShape(new Transform(localTransform), body.getCollisionShape());
				}
			}else{
				dynamics(false);
			}

		}else if (currBodyType == BodyType.STATIC || currBodyType == BodyType.SENSOR){
			if (compound && compShapeOld != null)
				scene.world.addRigidBody(body);

		}else if (valid()) {
			dynamics(true);
		}
	}

	public ArrayListGameObject childrenRecursive(){
		ArrayListGameObject childList = new ArrayListGameObject();
		for (GameObject child : children) {
			childList.add(child);
			childList.addAll(child.childrenRecursive());
		}
		return childList;
	}
	
	private CompoundShape compoundShape(){
		if (body.getCollisionShape() instanceof CompoundShape)
			return (CompoundShape) body.getCollisionShape();
		return null;
	}
	
	public Matrix4f transform(){
		return new Matrix4f(transform);
	}
	
	private void updateLocalTransform(){
		localTransform = parent.transform();
		localTransform.invert();
		localTransform.mul(transform);
	}
	
	public void updateChildTransforms(){
		for (GameObject g : children){
			g.transform(transform.mult(g.localTransform), false);
		}
	}
	
	private void updateTransform(boolean updatePosOri, boolean updateSca, boolean updateLocal){
		
		// get scale and unscaled transform
		
		Vector3f sca = new Vector3f();
		Matrix4f transNoSca = new Matrix4f();
		transform.get(sca, transNoSca);
		
		// apply unscaled transform to body and motion state
		
		if (updatePosOri){
			Transform t = new Transform(transNoSca);
			body.setWorldTransform(t);
			body.getMotionState().setWorldTransform(t);
		}
		
		// apply scale to collision shape
		
		if (updateSca){
			body.getCollisionShape().setLocalScaling(sca);
		}
		
		if (body.isInWorld()){
			
			// STATIC or SENSOR -> update Aabb and activate bodies of touching DYNAMIC objects
				
			if (body.isKinematicObject()){
				scene.world.updateSingleAabb(body);
				for (GameObject g : touchingObjects){
					g.activate();
				}
				
			// DYNAMIC or RIGID_BODY -> activate body
				
			}else{
				body.activate();
			}
		}
		
		// update transforms of children
		
		updateChildTransforms();
		
		// update local transform
		
		if (updateLocal && parent != null){
			updateLocalTransform();
		}
		
	}
	
	public void transform(Matrix4f m, boolean updateLocal){
		transform.set(m);
		updateTransform(true, true, updateLocal);
	}
	
	public void transform(Matrix4f m){
		transform(m, true);
	}
	
	public Vector3f position(){
		return transform.position();
	}
	
	public void position(Vector3f v){
		transform.position(v);
		updateTransform(true, false, true);
	}
	
	public void position(float x, float y, float z){
		position(new Vector3f(x, y, z));
	}
	
	public Vector3f scale(){
		return transform.scale();
	}
	
	public void flipState(Vector3f v){
		flipState.x = ((float) Math.abs(v.x)) / v.x;
		flipState.y = ((float) Math.abs(v.y)) / v.y;
		flipState.z = ((float) Math.abs(v.z)) / v.z;
	}
	
	public void flipX(){
		flipState.x *= -1;
	}
	
	public void flipY(){
		flipState.y *= -1;
	}
	
	public void flipZ(){
		flipState.z *= -1;
	}
	
	public void scale(Vector3f v){
		transform.scale(v);
		flipState(v);
		updateTransform(false, true, true);
	}
	
	public void scale(float x, float y, float z){
		scale(new Vector3f(x, y, z));
	}
	
	public void scale(float s){
		scale(s, s, s);
	}
	
	public void move(Vector3f delta){
		position(position().plus(delta));
	}

	public void move(float x, float y, float z){
		move(new Vector3f(x, y, z));
	}
	
	public void moveLocal(Vector3f delta){
		move(orientation().mult(delta));
	}

	public void moveLocal(float x, float y, float z){
		moveLocal(new Vector3f(x, y, z));
	}
	
	public Matrix3f orientation(){
		return transform.orientation();
	}
	
	public void orientation(Matrix3f ori){
		transform.orientation(ori);
		updateTransform(true, false, true);
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
	
	public void applyForce(Vector3f vec){
		activate();
		body.applyCentralForce(vec.mul(1f / Bdx.physicsSpeed));
	}

	public void applyForce(Vector3f force, Vector3f relPos) {
		activate();
		body.applyForce(force.mul(1f / Bdx.physicsSpeed), relPos);
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

	public void applyForceLocal(Vector3f force, Vector3f relPos) {
		applyForce(orientation().mult(force), relPos);
	}

	public void applyTorque(Vector3f vec){
		activate();
		body.applyTorque(vec.mul(1f / Bdx.physicsSpeed));
	}
	
	public void applyTorque(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		applyTorque(v);
	}
	
	public void applyTorqueLocal(Vector3f vec){
		applyTorque(orientation().mult(vec));
	}
	
	public void applyTorqueLocal(float x, float y, float z){
		Vector3f v = new Vector3f(x, y, z);
		applyTorqueLocal(v);
	}
	
	public void velocity(Vector3f vec){
		activate();
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
		activate();
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
	
	public boolean touchingProperty(String propName){
		return touchingObjects.getByProperty(propName) != null;
	}
	
	public boolean touchingComponent(String compName){
		return touchingObjects.getByComponent(compName) != null;
	}
	
	public boolean hit(){
		return hitObjects().size() > 0;
	}
	
	public boolean hit(String name){
		if (hitObjects().get(name) != null)
			return true;
		return false;
	}
	
	public boolean hitProperty(String propName){
		if (hitObjects().getByProperty(propName) != null)
			return true;
		return false;
	}
	
	public boolean hitComponent(String compName){
		if (hitObjects().getByComponent(compName) != null)
			return true;
		return false;
	}

	public ArrayListGameObject hitObjects(){
		ArrayListGameObject g = new ArrayListGameObject();
		g.addAll(touchingObjects);
		g.removeAll(touchingObjectsLast);
		return g;
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

	public void collisionGroup(short group)	{
		short mask = body.getBroadphaseProxy().collisionFilterMask;

		scene.world.removeRigidBody(body);
		scene.world.addRigidBody(body, group, mask);
	}

	public short collisionGroup()
	{
		return body.getBroadphaseProxy().collisionFilterGroup;
	}

	public void collisionMask(short mask) {
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
		endNoChildren();
		for (GameObject g : new ArrayList<GameObject>(children))
			g.end();
	}
	
	public void endNoChildren(){
		if (!valid)
			return;
		onEnd();
		valid = false;

		for (Component c : components)
			c.onGameObjectEnd();

		scene.remove(this);
		for (GameObject g : touchingObjects)
			g.activate();

		if (modelInstance != null)
			mesh.instances.remove(modelInstance);

	}

	public boolean valid(){
		return valid;
	}
	
	public Vector3f dimensions(){
		return dimensionsNoScale.mul(scale());
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

	public Mesh mesh(){
		return mesh;
	}

	public void mesh(String meshName){

		Mesh m = scene.meshes.get(meshName);

		if (m == null)
			throw new RuntimeException("No model found with name '" + meshName + "' in an active scene.");

		mesh(m);

	}

	public void mesh(Mesh mesh) {

		String meshName = mesh.name();

		if (mesh == this.mesh)              // You're already set to the current mesh
			return;

		if (!mesh.valid())
			throw new RuntimeException("ERROR! Attempting to set mesh of GameObject \"" + name + "\" to invalid Mesh \"" + mesh.name() + "\"!");

		JsonValue mOrigin = null;
		JsonValue mDimNoScale = null;

		ArrayList<Scene> sceneList = new ArrayList<Scene>(Bdx.scenes);
		
		if (sceneList.indexOf(scene) >= 0)
			Collections.swap(sceneList, sceneList.indexOf(scene), 0);
		else
			sceneList.add(0, scene);

		for (Scene sce : sceneList) {
			if (sce.meshes.containsKey(meshName)){
				mOrigin = sce.json.get("origins").get(meshName);
				mDimNoScale = sce.json.get("dimensions").get(meshName);
				break;
			}
		}

		origin = mOrigin == null ? new Vector3f() : new Vector3f(mOrigin.asFloatArray());
		dimensionsNoScale = mDimNoScale == null ? new Vector3f(1, 1, 1) : new Vector3f(mDimNoScale.asFloatArray());

		Matrix4 trans;
		if (modelInstance != null) {
			trans = modelInstance.transform;
			this.mesh.instances.remove(modelInstance);
		}
		else
			trans = new Matrix4();

		this.mesh = mesh;

		modelInstance = mesh.getInstance();
		modelInstance.transform.set(trans);

	}

	public void updateBody(Mesh mesh){

		// store and unparent compound children
		
		GameObject compParent = parent != null && parent.body.getCollisionShape().isCompound() ? parent : null;
		boolean isCompChild = compParent != null && !(currBodyType == BodyType.NO_COLLISION || currBodyType == BodyType.SENSOR);
		if (isCompChild){
			parent(null);
		}

		// get scale and unscaled transform
		
		Vector3f sca = new Vector3f();
		Matrix4f t = new Matrix4f();
		transform.get(sca, t);
		Transform transNoSca = new Transform(t);
		
		// update collision shape

		CollisionShape shape = body.getCollisionShape();
		float margin = shape.getMargin();
		boolean isCompound = shape.isCompound();
		shape = Bullet.makeShape(mesh.model.meshes.first(), currBoundsType, margin, isCompound);
		shape.setLocalScaling(sca);
		body.setCollisionShape(shape);
		
		// update center of mass transform
		
		Matrix4f o = new Matrix4f();
		o.set(origin);
		t.mul(o);
		body.setCenterOfMassTransform(new Transform(t));
		
		// update world transform
		
		body.setWorldTransform(transNoSca);
		
		// update Aabb
		
		if (body.isInWorld()){
			scene.world.updateSingleAabb(body);
		}
		
		// restore compound children

		if (isCompChild){
			parent(compParent);
		}

	}

	public void updateBody(String mesh) {

		ArrayList<Scene> sceneList = new ArrayList<Scene>(Bdx.scenes);
		if (sceneList.indexOf(scene) >= 0)
			Collections.swap(sceneList, sceneList.indexOf(scene), 0);
		else
			sceneList.add(0, scene);

		for (Scene s : sceneList) {
			Mesh m = s.meshes.get(mesh);
			if (m != null) {
				updateBody(m);
				return;
			}
		}

	}

	public void updateBody(){
		updateBody(mesh);
	}
	
	public HashMap<Mesh, ArrayList<Matrix4f>> join(ArrayList<GameObject> objects, boolean endObjects){
		
		// collect scaled transforms per mesh
		
		HashMap<Mesh, ArrayList<Matrix4f>> map = new HashMap<Mesh, ArrayList<Matrix4f>>();
		Mesh m;
		for (GameObject g : objects){
			m = g.mesh();
			if (!g.valid() || !m.valid())
				continue;
			ArrayList<Matrix4f> l;
			if (map.containsKey(m)){
				l = map.get(m);
			}else{
				l = new ArrayList<Matrix4f>();
				map.put(m, l);
			}
			l.add(g.transform());
			if (endObjects)
				g.endNoChildren();
		}
		
		// join
		
		join(map);
		
		return map;
	}
	
	public HashMap<Mesh, ArrayList<Matrix4f>> join(ArrayList<GameObject> objects){
		return join(objects, true);
	}
	
	public void join(HashMap<Mesh, ArrayList<Matrix4f>> map){
		
		// Collect transformed vertex arrays for each material & calculate number of indices
		
		int VERT_STRIDE = Bdx.VERT_STRIDE;
		
		HashMap<Material, ArrayList<float[]>> tvaMap = new HashMap<Material, ArrayList<float[]>>();
		HashMap<Material, Integer> lenMap = new HashMap<Material, Integer>();
		
		Mesh m;
		Node node;
		Material mat;
		MeshPart meshPart;
		float[] va, tva;
		int numIndices, numVertices, offset, j, len;
		
		Matrix4f transTemp = Matrix4f.identity();
		Vector3f vecTemp = new Vector3f();
		Matrix4f trans = new Matrix4f();
		Matrix4f vertTrans = new Matrix4f();
		Matrix4f normTrans = new Matrix4f();
		Matrix4f transZeroPos = new Matrix4f();
		
		Matrix4f transInv = transform();
		transInv.invert();
		Vector3f zeroPos = new Vector3f();
		
		for (Map.Entry<Mesh, ArrayList<Matrix4f>> e : map.entrySet()){
			m = e.getKey();
			node = m.model.nodes.get(0);
			for (Matrix4f t : e.getValue()){
				
				trans.set(transInv);
				trans.mul(t);
				transZeroPos.set(trans);
				transZeroPos.position(zeroPos);
				
				for (NodePart nodePart : node.parts){
					meshPart = nodePart.meshPart;
					numIndices = meshPart.size;
					numVertices = numIndices * VERT_STRIDE;
					offset = meshPart.offset * VERT_STRIDE;
					va = meshPart.mesh.getVertices(offset, numVertices, new float[numVertices]);
					tva = new float[numVertices];
					j = 0;
					
					for (int i = 0; i < numIndices; i++){
						vertTrans.set(trans);
						vecTemp.set(va[j], va[j+1], va[j+2]);
						transTemp.position(vecTemp);
						vertTrans.mul(transTemp);
						
						normTrans.set(transZeroPos);
						vecTemp.set(va[j+3], va[j+4], va[j+5]);
						transTemp.position(vecTemp);
						normTrans.mul(transTemp);
						
						tva[j] = vertTrans.m03;
						tva[j+1] = vertTrans.m13;
						tva[j+2] = vertTrans.m23;
						tva[j+3] = normTrans.m03;
						tva[j+4] = normTrans.m13;
						tva[j+5] = normTrans.m23;
						tva[j+6] = va[j+6];
						tva[j+7] = va[j+7];
						j += VERT_STRIDE;
					}
					
					mat = m.materials.get(nodePart.material.id);
					ArrayList<float[]> l;
					if (tvaMap.containsKey(mat)){
						l = tvaMap.get(mat);
						len = lenMap.get(mat);
					}else{
						l = new ArrayList<float[]>();
						tvaMap.put(mat, l);
						len = 0;
					}
					l.add(tva);
					lenMap.put(mat, len + tva.length);
				}
			}
		}
		
		// Build a unique model out of meshparts for each material
		
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		
		short idx = 0;
		MeshPartBuilder mpb;
		
		for (Map.Entry<Material, ArrayList<float[]>> e : tvaMap.entrySet()){
			mat = e.getKey();
			len = lenMap.get(mat);
			tva = new float[len];
			j = 0;
			
			for (float[] verts : e.getValue()){
				numVertices = verts.length;
				for (int i = 0; i < numVertices; i++){
					tva[i + j] = verts[i];
				}
				j += numVertices;
			}
			
			mpb = builder.part(mat.name(), GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat);
			mpb.vertex(tva);
			
			try{
				for (short i = 0; i < len / VERT_STRIDE; i++){
					mpb.index(idx);
					idx += 1;
				}
			}catch (Error error){
				throw new RuntimeException("MODEL ERROR: Models with more than 32767 vertices are not supported. Decrease the number of objects to join.");
			}
		}
		
		Model finishedModel = builder.end();
		
		// Update mesh
		
		mesh(new Mesh(finishedModel, scene));
		
		// Update dimensionsNoScale and origin
		
		com.badlogic.gdx.graphics.Mesh mesh = finishedModel.meshes.first();
		BoundingBox bbox = mesh.calculateBoundingBox();
		Vector3 dimensions = bbox.getDimensions(new Vector3());
		Vector3 center = bbox.getCenter(new Vector3());
		dimensionsNoScale = new Vector3f(dimensions.x, dimensions.y, dimensions.z);
		origin = new Vector3f(center.x, center.y, center.z);
		
		// Update body
		
		updateBody();
		
		// Set visbility to initial value if empty
		
		if (json.get("mesh_name").asString() == null){
			visible = json.get("visible").asBoolean();
		}
	}
	
	public String toString(){

		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());

	}

	public void dynamics(boolean restore){
		if (currBodyType == BodyType.DYNAMIC || currBodyType == BodyType.RIGID_BODY){
			if (restore){
				bodyType(currBodyType);
			}else{ // suspend
				body.setCollisionFlags(body.getCollisionFlags() | CollisionFlags.KINEMATIC_OBJECT);
			}
		}
	}

	public boolean dynamics(){
		return body.isInWorld() && !body.isKinematicObject();
	}
	
	public float mass(){
		return 1 / body.getInvMass();
	}
	
	public void mass(float mass){
		if (mass == 0){
			throw new RuntimeException("no zero value allowed: use 'dynamics(false)' instead");
		}
		Vector3f inertia = new Vector3f();
		body.getCollisionShape().calculateLocalInertia(mass, inertia);
		body.setMassProps(mass, inertia);
	}
	
	public BodyType bodyType(){
		return currBodyType;
	}
	
	public void bodyType(BodyType bodyType){
		int flags = body.getCollisionFlags();
		if (body.isInWorld())
			scene.world.removeRigidBody(body);
		if (bodyType == BodyType.NO_COLLISION){
			for (GameObject g : touchingObjects)
				g.activate();
			flags &= ~CollisionFlags.KINEMATIC_OBJECT;
		}else{
			if (bodyType == BodyType.STATIC){
				flags |= CollisionFlags.KINEMATIC_OBJECT;
			}else if (bodyType == BodyType.SENSOR){
				flags |= CollisionFlags.KINEMATIC_OBJECT;
				flags |= CollisionFlags.NO_CONTACT_RESPONSE;
			}else{
				// NO_COLLISION -> DYNAMIC or RIGID_BODY hack
				if (currBodyType == BodyType.NO_COLLISION){
					body.clearForces();
					body.setLinearVelocity(new Vector3f());
				}
				// kinematic initialization hack
				if (mass() == Float.POSITIVE_INFINITY){
					mass(1); // Blender default
					flags &= ~CollisionFlags.KINEMATIC_OBJECT;
					body.setCollisionFlags(flags);
				}
				flags &= ~CollisionFlags.KINEMATIC_OBJECT;
				if (bodyType == BodyType.DYNAMIC){
					body.setAngularVelocity(new Vector3f());
					body.setAngularFactor(0);
				}else if (bodyType == BodyType.RIGID_BODY){
					body.setAngularFactor(1);
				}
			}
			scene.world.addRigidBody(body);
			activate();
		}
		body.setCollisionFlags(flags);
		currBodyType = bodyType;
	}
	
	public BoundsType boundsType(){
		return currBoundsType;
	}

	public void boundsType(BoundsType boundsType){
		com.badlogic.gdx.graphics.Mesh mesh = modelInstance.model.meshes.first();
		CollisionShape shape = body.getCollisionShape();
		shape = Bullet.makeShape(mesh, boundsType, shape.getMargin(), shape.isCompound());
		body.setCollisionShape(shape);
		currBoundsType = boundsType;
	}
	
	public float collisionMargin(){
		return body.getCollisionShape().getMargin();
	}
	
	public void collisionMargin(float m){
		body.getCollisionShape().setMargin(m);
	}
	
	public void activate(){
		if (dynamics())
			body.activate();
	}

	public void deactivate(){
		body.forceActivationState(2);
	}

	public boolean insideFrustum(Vector3f customHalfDim){
		Vector3f min = new Vector3f();
		Vector3f max = new Vector3f();
		body.getAabb(min, max);
		Vector3f dimHalved = max.minus(min).mul(0.5f);
		Vector3f center;

		if (origin.length() == 0 || currBoundsType == BoundsType.CONVEX_HULL || currBoundsType == BoundsType.TRIANGLE_MESH)
			center = min.plus(dimHalved);
		else
			center = min.plus(dimHalved).plus(orientation().mult(origin).mul(scale()));

		if (customHalfDim == null)
			customHalfDim = dimHalved;

		return scene.camera.data.frustum.boundsInFrustum(center.x, center.y, center.z, customHalfDim.x, customHalfDim.y, customHalfDim.z);
	}

	public boolean insideFrustum(){
		return insideFrustum(null);
	}

	public Vector3f vecTo(Vector3f vector){
		return vector.minus(position());
	}

	public Vector3f vecTo(GameObject other){
		return vecTo(other.position());
	}

	public PersistentManifold getManifoldForCollision(GameObject other){

		for (PersistentManifold contact : contactManifolds) {

			RigidBody rb = (RigidBody) contact.getBody0();

			if (rb.getUserPointer() == this)
				rb = (RigidBody) contact.getBody1();

			if (rb.getUserPointer() == other)
				return contact;

		}

		return null;

	}

	public boolean aabbContains(float x, float y, float z, float[][] aabb) {
		Vector3f min = new Vector3f();
		Vector3f max = new Vector3f();
		if (aabb == null) {
			body.getAabb(min, max);
			min.plus(position());
			max.plus(position());
		} else {
			min.set(aabb[0]);
			max.set(aabb[7]);
		}
		return (x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z);
	}

	public boolean aabbContains(float[] point, float[][] aabb) {
		return aabbContains(point[0], point[1], point[2], aabb);
	}

	public boolean aabbContains(Vector3f point, float[][] aabb) {
		return aabbContains(point.x, point.y, point.z, aabb);
	}

	public boolean aabbContains(float[][] otherAABBPoints, float[][] thisAABBPoints) {

		Vector3f vec = new Vector3f();
		float[][] aabb = thisAABBPoints;
		if (aabb == null)
			aabb = getAABBPoints();

		for (float[] p : otherAABBPoints) {
			vec.set(p);
			if (!aabbContains(vec, aabb))
				return false;
		}

		return true;

	}

	public boolean aabbContains(GameObject other) {
		return aabbContains(other.getAABBPoints(), null);
	}

	public boolean aabbContainsAny(float[][] otherAABBPoints, float[][] thisAABBPoints) {

		Vector3f vec = new Vector3f();
		float[][] aabb = thisAABBPoints;
		if (aabb == null)
			aabb = getAABBPoints();

		for (float[] p : otherAABBPoints) {
			vec.set(p);
			if (aabbContains(vec, aabb))
				return true;
		}
		return false;

	}

	public boolean aabbContainsAny(GameObject other) {
		return aabbContainsAny(other.getAABBPoints(), null);
	}

	public float[][] getAABBPoints(float margin) {

		float points[][] = new float[8][3];

		Vector3f min = new Vector3f();
		Vector3f max = new Vector3f();
		body.getAabb(min, max);

		min.sub(margin, margin, margin);
		max.add(margin, margin, margin);

		min.plus(position());
		max.plus(position());

		points[0] = new float[]{min.x, min.y, min.z};
		points[1] = new float[]{max.x, min.y, min.z};
		points[2] = new float[]{min.x, max.y, min.z};
		points[3] = new float[]{min.x, min.y, max.z};
		points[4] = new float[]{max.x, max.y, min.z};
		points[5] = new float[]{min.x, max.y, max.z};
		points[6] = new float[]{max.x, min.y, max.z};
		points[7] = new float[]{max.x, max.y, max.z};

		return points;
	}

	public float[][] getAABBPoints(){
		return getAABBPoints(0);
	}

}
