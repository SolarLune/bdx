package com.nilunder.bdx;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
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
import com.nilunder.bdx.utils.*;

public class GameObject implements Named{
	public JsonValue json;
	
	public String name;
	public ArrayListGameObject touchingObjects;
	public ArrayListGameObject touchingObjectsLast;
	public ArrayList<PersistentManifold> contactManifolds;
	public ModelInstance modelInstance;
	public RigidBody body;
	public String currBodyType;
	public String currBoundsType;
	public Vector3f origin;
	public Vector3f dimensionsNoScale;
	
	public HashMap<String, JsonValue> props;
	
	public ArrayListGameObject children;
	
	public ArrayListNamed<Component> components;
	
	public ArrayList<GameObject> joinedMeshObjects;
	
	public Scene scene;
	
	private GameObject parent;
	private Matrix4f localTransform;
	private Vector3f localScale;
	private boolean visible;
	private boolean valid;
	private Model uniqueModel;
	public ArrayListMaterials materials;

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
		
	}

	public class ArrayListMaterials extends ArrayListNamed<Material> {

		public Material set(int index, Material material) {
			modelInstance.nodes.get(0).parts.get(index).material = material;
			return super.set(index, material);
		}

		public Material set(int index, String matName) {
			return set(index, scene.materials.get(matName));
		}

		public void set(Material material){
			for (int i = 0; i < size(); i++) {
				set(i, material);
			}
		}

		public void set(String matName){
			set(scene.materials.get(matName));
		}

		public void color(Color color){
			for (Material mat : this)
				mat.color(color);
		}

		public void tint(Color color){
			for (Material mat : this)
				mat.tint(color);
		}

		public void blendMode(int src, int dest){
			for (Material mat : this)
				mat.blendMode(src, dest);
		}

		public void shadeless(boolean shadelessness){
			for (Material mat : this)
				mat.shadeless(shadelessness);
		}

	}

	public GameObject() {
		joinedMeshObjects = new ArrayList<GameObject>();
		touchingObjects = new ArrayListGameObject();
		touchingObjectsLast = new ArrayListGameObject();
		contactManifolds = new ArrayList<PersistentManifold>();
		components = new ArrayListNamed<Component>();
		children = new ArrayListGameObject();
		materials = new ArrayListMaterials();
		valid = true;
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
			updateLocalScale();

			if (compound){
				CompoundShape compShape = parent.compoundShape();
				if (compShape != null){
					scene.world.removeRigidBody(body);
					compShape.addChildShape(new Transform(localTransform), body.getCollisionShape());
				}
			}else{
				dynamics(false);
			}

		}else if (currBodyType.equals("STATIC") || currBodyType.equals("SENSOR")){
			if (compound && compShapeOld != null)
				scene.world.addRigidBody(body);

		}else{
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
	
	public Vector3f position(){
		Transform t = new Transform();
		body.getWorldTransform(t);
		
		return new Vector3f(t.origin);
	}
	
	public void position(Vector3f vec){
		activate();
		
		Matrix4f t = transform();
		t.setTranslation(vec);
		
		transform(t);
		
	}
	
	public void position(float x, float y, float z){
		position(new Vector3f(x, y, z));
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
		activate();
		
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
				g.activate();
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
		Vector3f ps = parent.scale();
		ms.m00 = ps.x; ms.m11 = ps.y; ms.m22 = ps.z;
		localTransform.mul(ms);
		localTransform.invert();
		localTransform.mul(transform());
	}

	public void applyForce(Vector3f vec){
		activate();
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
	
	public void applyTorque(Vector3f vec){
		activate();
		body.applyTorque(vec);
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
		for (GameObject g: touchingObjects){
			if (!touchingObjectsLast.contains(g)){
				return true;
			}
		}
		return false;
	}
	
	public boolean hit(String name){
		for (GameObject g : touchingObjects){
			if (g.name().equals(name) && !touchingObjectsLast.contains(g))
				return true;
		}
		return false;
	}
	
	public boolean hitProperty(String propName){
		for (GameObject g : touchingObjects){
			if (g.props.containsKey(propName) && !touchingObjectsLast.contains(g))
				return true;
		}
		return false;
	}
	
	public boolean hitComponent(String compName){
		for (GameObject g : touchingObjects){
			if (g.components.get(compName) != null && !touchingObjectsLast.contains(g))
				return true;
		}
		return false;
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
		for (GameObject g : new ArrayList<GameObject>(children)){
			g.end();
		}
		endNoChildren();
	}
	
	public void endNoChildren(){
		if (!valid)
			return;
		onEnd();
		parent(null);
		valid = false;
		if (uniqueModel != null)
			uniqueModel.dispose();
		scene.remove(this);
		for (GameObject g : touchingObjects)
			g.activate();
	}

	public boolean valid(){
		return valid;
	}
	
	public void scale(float x, float y, float z, boolean updateLocal){
		activate();
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

	public String modelName(){
		return modelInstance.model.meshParts.first().id;
	}
	
	public void useUniqueModel(){
		JsonValue modelData = scene.json.get("models").get(modelName());
		uniqueModel = scene.createModel(modelData);
		ModelInstance mi = new ModelInstance(uniqueModel);
		mi.transform.set(modelInstance.transform);
		modelInstance = mi;
		for (int i = 0; i < modelInstance.nodes.get(0).parts.size; i++){
			modelInstance.nodes.get(0).parts.get(i).material = materials.get(i);
		}
	}

	public void replaceModel(String modelName, boolean updateVisual, boolean updatePhysics){
		if (modelName.equals(modelName()))
			return;
		
		Model model = null;
		JsonValue mOrigin = null;
		JsonValue mDimNoScale = null;
		for (Scene sce : Bdx.scenes){
			if (sce.models.containsKey(modelName)){
				model = sce.models.get(modelName);
				mOrigin = sce.json.get("origins").get(modelName);
				mDimNoScale = sce.json.get("dimensions").get(modelName);
				break;
			}
		}
		if (model == null){
			throw new RuntimeException("No model found with name: '" + modelName + "'");
		}
		origin = mOrigin == null ? new Vector3f() : new Vector3f(mOrigin.asFloatArray());
		dimensionsNoScale = mDimNoScale == null ? new Vector3f(1, 1, 1) : new Vector3f(mDimNoScale.asFloatArray());
		Matrix4 trans = modelInstance.transform;

		if (updateVisual){
			ModelInstance mi = new ModelInstance(model);
			mi.transform.set(trans);
			modelInstance = mi;
		}
		
		if (updatePhysics){
			GameObject compParent = parent != null && parent.body.getCollisionShape().isCompound() ? parent : null;
			boolean isCompChild = compParent != null && !(currBodyType.equals("NO_COLLISION") || currBodyType.equals("SENSOR"));
			if (isCompChild){
				parent(null);
			}
			
			Matrix4f transform = transform();
			Vector3f scale = scale();
			
			CollisionShape shape = body.getCollisionShape();
			body.setCollisionShape(Bullet.makeShape(model.meshes.first(), currBoundsType, shape.getMargin(), shape.isCompound()));
			
			if (currBoundsType.equals("CONVEX_HULL")){
				Transform startTransform = new Transform();
				body.getMotionState().getWorldTransform(startTransform);
				Matrix4f originMatrix = new Matrix4f();
				originMatrix.set(origin);
				Transform centerOfMassTransform = new Transform();
				centerOfMassTransform.set(originMatrix);
				centerOfMassTransform.mul(startTransform);
				body.setCenterOfMassTransform(centerOfMassTransform);
			}
			
			transform(transform);
			scale(scale);

			if (body.isInWorld()){
				scene.world.updateSingleAabb(body);
			}else{ // update Aabb hack for when not in world
				scene.world.addRigidBody(body);
				scene.world.updateSingleAabb(body);
				scene.world.removeRigidBody(body);
			}

			if (isCompChild){
				parent(compParent);
			}
		}
	}

	public void replaceModel(String modelName){
		replaceModel(modelName, true, false);
	}
	
	public void updateJoinedMesh(boolean endJoinedMeshObjects){
		
		// Set invisible and remove body if not joining anything
		
		if (joinedMeshObjects.isEmpty()){
			scene.world.removeRigidBody(body);
			visible = false;
			return;
		}
		
		// Join the transformed vertex arrays
		
		int VERT_STRIDE = Bdx.VERT_STRIDE;
		
		ArrayList<float[]> tvaList = new ArrayList<float[]>();
		int tvaListJoinedLength = 0;
		GameObject g = null;
		
		for (int k=0; k < joinedMeshObjects.size(); k++){
			g = joinedMeshObjects.get(k);
			
			Vector3f p = g.position();
			Matrix3f o = g.orientation();
			Vector3f s = g.scale();
			
			Mesh m = g.modelInstance.model.meshes.first();
			float[] va = new float[m.getNumVertices() * VERT_STRIDE];
			m.getVertices(0, va.length, va);
			
			float[] tva = new float[va.length];
			int len = va.length / VERT_STRIDE;
			int j = 0;
			
			for (int i=0; i < len; i++){
				Vector3f vP = new Vector3f(va[j], va[j+1], va[j+2]);
				Vector3f nP = new Vector3f(va[j+3], va[j+4], va[j+5]);
				Vector3f vPT = o.mult(vP.mul(s)).plus(p);
				Vector3f nPT = o.mult(vP.plus(nP)).minus(o.mult(vP));
				tva[j] = vPT.x;
				tva[j+1] = vPT.y;
				tva[j+2] = vPT.z;
				tva[j+3] = nPT.x;
				tva[j+4] = nPT.y;
				tva[j+5] = nPT.z;
				tva[j+6] = va[j+6];
				tva[j+7] = va[j+7];
				j += VERT_STRIDE;
			}
			
			tvaListJoinedLength += tva.length;
			tvaList.add(tva);
		}
		
		float[] tvaListJoined = new float[tvaListJoinedLength];
		int j = 0;
		for (float[] tva : tvaList){
			int len = tva.length;
			for (int i=0; i < len; i++){
				tvaListJoined[i + j] = tva[i];
			}
			j += len;
		}
		
		// Build to replace the model
		
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshPartBuilder mpb = builder.part(name, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates, g.materials.get(0));
		mpb.vertex(tvaListJoined);
		int numIndices = tvaListJoinedLength / VERT_STRIDE;
		if (numIndices > 32767){
			throw new RuntimeException("Maximum number of vertices exceeded. Join not more than 10922 triangles."); 
		}
		for (short i=0; i < numIndices; i++){
			mpb.index(i);
		}
		uniqueModel = builder.end();
		modelInstance = new ModelInstance(uniqueModel);
		
		// Update visual
		
		visible = json.get("visible").asBoolean();
		Mesh mesh = uniqueModel.meshes.first();
		BoundingBox bbox = mesh.calculateBoundingBox();
		Vector3 dimensions = new Vector3();
		Vector3 center = new Vector3();
		bbox.getDimensions(dimensions);
		bbox.getCenter(center);
		dimensionsNoScale = new Vector3f(dimensions.x, dimensions.y, dimensions.z);
		origin = new Vector3f(center.x, center.y, center.z);
		position(new Vector3f());
		
		// Update physics
		
		scene.world.removeRigidBody(body);
		body.destroy();
		float[] trans = new float[]{1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
		body = Bullet.makeBody(mesh, trans, origin, currBodyType, currBoundsType, json.get("physics"));
		body.setUserPointer(this);
		scene.world.addRigidBody(body);
		scene.world.updateSingleAabb(body);
		if (currBodyType.equals("NO_COLLISION")){
			scene.world.removeRigidBody(body);
		}
		
		// Update joined mesh objects
		
		if (endJoinedMeshObjects){
			for (GameObject jmo : joinedMeshObjects){
				jmo.end();
			}
			joinedMeshObjects.clear();
		}
	}
	
	public void updateJoinedMesh(){
		updateJoinedMesh(false);
	}
	
	public String toString(){

		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());

	}

	public void dynamics(boolean restore){
		if (currBodyType.equals("DYNAMIC") || currBodyType.equals("RIGID_BODY")){
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
	
	public String bodyType(){
		return currBodyType;
	}
	
	public void bodyType(String s){
		int flags = body.getCollisionFlags();
		if (body.isInWorld())
			scene.world.removeRigidBody(body);
		if (s.equals("NO_COLLISION")){
			for (GameObject g : touchingObjects)
				g.activate();
			flags &= ~CollisionFlags.KINEMATIC_OBJECT;
		}else{
			if (s.equals("STATIC")){
				flags |= CollisionFlags.KINEMATIC_OBJECT;
			}else if (s.equals("SENSOR")){
				flags |= CollisionFlags.KINEMATIC_OBJECT;
				flags |= CollisionFlags.NO_CONTACT_RESPONSE;
			}else{
				// NO_COLLISION -> DYNAMIC or RIGID_BODY hack
				if (currBodyType.equals("NO_COLLISION")){
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
				if (s.equals("DYNAMIC")){
					body.setAngularVelocity(new Vector3f());
					body.setAngularFactor(0);
				}else if (s.equals("RIGID_BODY")){
					body.setAngularFactor(1);
				}else{
					throw new RuntimeException(s + " is no valid bodyType name.");
				}
			}
			scene.world.addRigidBody(body);
			activate();
		}
		body.setCollisionFlags(flags);
		currBodyType = s;
	}
	
	public String boundsType(){
		return currBoundsType;
	}

	public void boundsType(String s){
		Mesh mesh = modelInstance.model.meshes.first();
		CollisionShape shape = body.getCollisionShape();
		shape = Bullet.makeShape(mesh, s, shape.getMargin(), shape.isCompound());
		body.setCollisionShape(shape);
		currBoundsType = s;
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

	public boolean insideFrustum(){
		Vector3f min = new Vector3f();
		Vector3f max = new Vector3f();
		body.getAabb(min, max);
		Vector3f dimHalved = max.minus(min).mul(0.5f);
		Vector3f center;

		if (origin.length() == 0 || currBoundsType.equals("CONVEX_HULL") || currBoundsType.equals("TRIANGLE_MESH"))
			center = min.plus(dimHalved);
		else
			center = min.plus(dimHalved).plus(orientation().mult(origin).mul(scale()));
		
		return scene.camera.data.frustum.boundsInFrustum(center.x, center.y, center.z, dimHalved.x, dimHalved.y, dimHalved.z);
	}

}
