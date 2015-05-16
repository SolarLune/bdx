package com.nilunder.bdx.utils;

import java.nio.*;

import javax.vecmath.*;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.glutils.*;

import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.*;
import com.bulletphysics.linearmath.*;
import com.bulletphysics.util.*;

import com.nilunder.bdx.*;

public class Bullet {

public static class DebugDrawer extends IDebugDraw{

	private static ShapeRenderer shapeRenderer;
	private static Vector3 from;
	private static Vector3 to;

	private boolean canDraw;
	private boolean debug;

	public DebugDrawer(boolean debug){
		if (shapeRenderer == null){
			shapeRenderer = new ShapeRenderer();
			from = new Vector3();
			to = new Vector3();
		}
		this.debug = debug;
	}

	public void drawLine(Vector3f from, Vector3f to, Vector3f color){
		// It appears that this method will be called outside world.debugDrawWorld(),
		// to draw things that don't appear to be overly relevant.
		// So, instead of buffering vectors to draw at the right time (between shaperRenderer.begin() and end()),
		// I simply bail:
		if (canDraw){
			shapeRenderer.setColor(color.x, color.y, color.z, 1f);
			this.from.x = from.x; this.to.x = to.x;
			this.from.y = from.y; this.to.y = to.y;
			this.from.z = from.z; this.to.z = to.z;
			shapeRenderer.line(this.from, this.to);
		}
	}

	public int getDebugMode(){
		if (debug)
			return DebugDrawModes.DRAW_AABB | DebugDrawModes.DRAW_WIREFRAME;
		else
			return DebugDrawModes.NO_DEBUG;
	}

	public void setDebugMode(int mode){}
	public void draw3dText(Vector3f v, String s){}
	public void reportErrorWarning(String s){}
	public void drawContactPoint(Vector3f a, Vector3f b, float f, int i, Vector3f c){}

	public void drawWorld(DynamicsWorld world, Camera camera){
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		canDraw = true;

		world.debugDrawWorld();
		
		shapeRenderer.end();
		canDraw = false;
	}
}
	
	public static IndexedMesh makeMesh(Mesh mesh){
		ByteBuffer indices = ByteBuffer.allocate(mesh.getIndicesBuffer().capacity() * (Short.SIZE/8));
		indices.asShortBuffer().put(mesh.getIndicesBuffer());
		mesh.getIndicesBuffer().rewind();
		
		ByteBuffer verts = ByteBuffer.allocate(mesh.getVerticesBuffer().capacity() * (Float.SIZE/8));
		verts.asFloatBuffer().put(mesh.getVerticesBuffer());
		mesh.getVerticesBuffer().rewind();
		
		IndexedMesh m = new IndexedMesh();
		
		m.numTriangles = mesh.getNumIndices()/3;
		m.triangleIndexBase = indices;
		m.triangleIndexStride = (Short.SIZE/8) * 3;
		m.numVertices = mesh.getNumVertices();
		m.vertexBase = verts;
		m.vertexStride = (Float.SIZE/8) * Bdx.VERT_STRIDE;
		
		return m;
	}

	public static CollisionShape makeShape(Mesh mesh, String bounds, boolean compound){

		CollisionShape shape;
	
		if (bounds.equals("TRIANGLE_MESH")){
			TriangleIndexVertexArray mi = new TriangleIndexVertexArray();
			mi.addIndexedMesh(Bullet.makeMesh(mesh), ScalarType.SHORT);
			shape = new BvhTriangleMeshShape(mi, false);
			
		}else if (bounds.equals("SPHERE")){
			Vector3 bbox = mesh.calculateBoundingBox().getDimensions(new Vector3());
			float radius = Math.max(Math.max(bbox.x, bbox.y), bbox.z) / 2;
			shape = new SphereShape(radius);
		}else if (bounds.equals("CAPSULE")){
			Vector3 dim = mesh.calculateBoundingBox().getDimensions(new Vector3());
			float radius = Math.max(dim.x, dim.y) / 2;
			float height = dim.z - (2*radius);
			shape = new CapsuleShapeZ(radius, height);			
		}else if (bounds.equals("CONVEX_HULL")){
			float[] verts = new float[mesh.getNumVertices() * mesh.getVertexSize()];
			mesh.getVertices(verts);
			ObjectArrayList<Vector3f> vertList = new ObjectArrayList<Vector3f>();
			for (int i = 0; i < mesh.getNumVertices() * Bdx.VERT_STRIDE; i += Bdx.VERT_STRIDE) {
				vertList.add(new Vector3f(verts[i], verts[i + 1], verts[i + 2]));
			}
			shape = new ConvexHullShape(vertList);
		}else{ // BOX
			BoundingBox bbox = mesh.calculateBoundingBox();
			Vector3 d = bbox.getDimensions(new Vector3()).scl(0.5f);
			Vector3f dim = new Vector3f(d.x, d.y, d.z);
			shape = new BoxShape(dim);
		}

		if (compound) {
			CompoundShape compShape = new CompoundShape();
			Transform trans = new Transform();
			trans.setIdentity();
			compShape.addChildShape(trans, shape);
			return compShape;
		}
		return shape;

	}
	
	public static RigidBody makeBody(Mesh mesh, float[] glTransform, JsonValue physics){
		String boundsType = physics.get("bounds_type").asString();
		
		CollisionShape shape = makeShape(mesh, boundsType, physics.get("compound").asBoolean());
		
		float mass = physics.get("mass").asFloat();
		String bodyType = physics.get("body_type").asString();
		
		Vector3f inertia = new Vector3f();
		shape.calculateLocalInertia(mass, inertia);
		
		Transform startTransform = new Transform();
		startTransform.setFromOpenGLMatrix(glTransform);
		MotionState motionState = new DefaultMotionState(startTransform);
		
		RigidBodyConstructionInfo ci = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
		
		RigidBody body = new RigidBody(ci);
		
		if (bodyType.equals("STATIC")){
			body.setCollisionFlags(CollisionFlags.KINEMATIC_OBJECT);
			//body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
		}else if (bodyType.equals("DYNAMIC")){
			body.setAngularFactor(0f);
		}
		
		if (bodyType.equals("SENSOR") || physics.get("ghost").asBoolean()){
			body.setCollisionFlags(CollisionFlags.NO_CONTACT_RESPONSE);
		}
		
		body.setRestitution(physics.get("restitution").asFloat());
		body.setFriction(physics.get("friction").asFloat());

		return body;
	}

	public static RigidBody cloneBody(RigidBody body){
		GameObject gobj = (GameObject)body.getUserPointer();
		JsonValue physics = gobj.json.get("physics");
		float mass = gobj.mass();
		
		Vector3f inertia = new Vector3f();
		body.getCollisionShape().calculateLocalInertia(mass, inertia);
		
		CollisionShape shape;

		if (gobj.modelInstance != null){
			shape = makeShape(gobj.modelInstance.model.meshes.first(), physics.get("bounds_type").asString(), physics.get("compound").asBoolean());
		}else{
			shape = new BoxShape(new Vector3f(0.25f, 0.25f, 0.25f));
		}
		
		RigidBody b = new RigidBody(mass, new DefaultMotionState(new Transform(gobj.transform())),
								shape,
								inertia);
		
		b.setCollisionFlags(gobj.body.getCollisionFlags());
		b.setAngularFactor(gobj.body.getAngularFactor());
		b.setRestitution(gobj.body.getRestitution());
		b.setFriction(gobj.body.getFriction());
		
		return b;
	}
}
