package com.nilunder.bdx.utils;

import java.nio.*;

import javax.vecmath.*;

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
import com.nilunder.bdx.gl.Mesh;
import com.nilunder.bdx.GameObject.BodyType;
import com.nilunder.bdx.GameObject.BoundsType;

public class Bullet {

public static class DebugDrawer extends IDebugDraw{

	private static ShapeRenderer shapeRenderer;
	private static Vector3 from;
	private static Vector3 to;

	private boolean canDraw;
	public boolean debug;

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
		com.badlogic.gdx.graphics.Mesh m = mesh.model.meshes.first();
		ByteBuffer indices = ByteBuffer.allocate(m.getIndicesBuffer().capacity() * (Short.SIZE/8));
		indices.asShortBuffer().put(m.getIndicesBuffer());
		m.getIndicesBuffer().rewind();
		
		ByteBuffer verts = ByteBuffer.allocate(m.getVerticesBuffer().capacity() * (Float.SIZE/8));
		verts.asFloatBuffer().put(m.getVerticesBuffer());
		m.getVerticesBuffer().rewind();
		
		IndexedMesh im = new IndexedMesh();
		
		im.numTriangles = m.getNumIndices()/3;
		im.triangleIndexBase = indices;
		im.triangleIndexStride = (Short.SIZE/8) * 3;
		im.numVertices = m.getNumVertices();
		im.vertexBase = verts;
		im.vertexStride = (Float.SIZE/8) * Bdx.VERT_STRIDE;
		
		return im;
	}

	public static CollisionShape makeShape(Mesh mesh, BoundsType bounds, float margin, boolean compound){

		CollisionShape shape;
	
		if (bounds == BoundsType.TRIANGLE_MESH){
			TriangleIndexVertexArray mi = new TriangleIndexVertexArray();
			mi.addIndexedMesh(Bullet.makeMesh(mesh), ScalarType.SHORT);
			shape = new BvhTriangleMeshShape(mi, false);
		}else if (bounds == BoundsType.CONVEX_HULL){
			float[] verts = mesh.vertices();
			ObjectArrayList<Vector3f> vertList = new ObjectArrayList<Vector3f>();
			for (int i = 0; i < mesh.vertexArrayLength(); i += Bdx.VERT_STRIDE){
				vertList.add(new Vector3f(verts[i], verts[i + 1], verts[i + 2]));
			}
			shape = new ConvexHullShape(vertList);
			margin *= 0.5f;
		}else{
			com.badlogic.gdx.graphics.Mesh m = mesh.model.meshes.first();
			Vector3 d = m.calculateBoundingBox().getDimensions(new Vector3()).scl(0.5f);
			if (bounds == BoundsType.SPHERE){
				float radius = Math.max(Math.max(d.x, d.y), d.z);
				shape = new SphereShape(radius);
			}else if (bounds == BoundsType.BOX){
				shape = new BoxShape(new Vector3f(d.x, d.y, d.z));
			}else if (bounds == BoundsType.CYLINDER){
				shape = new CylinderShapeZ(new Vector3f(d.x, d.y, d.z));
			}else if (bounds == BoundsType.CAPSULE){
				float radius = Math.max(d.x, d.y);
				float height = (d.z - radius) * 2;
				shape = new CapsuleShapeZ(radius, height);
			}else{ //"CONE"
				float radius = Math.max(d.x, d.y);
				float height = d.z * 2;
				shape = new ConeShapeZ(radius, height);
				margin *= 0.5f;
			}
		}
		shape.setMargin(margin);

		if (compound) {
			CompoundShape compShape = new CompoundShape();
			compShape.setMargin(0);
			Transform trans = new Transform();
			trans.setIdentity();
			compShape.addChildShape(trans, shape);
			return compShape;
		}
		return shape;

	}
	
	public static RigidBody makeBody(Mesh mesh, Matrix4f trans, Vector3f origin, BodyType bodyType, BoundsType boundsType, JsonValue physics){
		
		// get scale and unscaled transform
		
		Vector3f sca = new Vector3f();
		Matrix4f t = new Matrix4f();
		trans.get(sca, t);
		Transform transform = new Transform(t);
		
		// collect physics properties
		
		float margin = physics.get("margin").asFloat();
		boolean isCompound = physics.get("compound").asBoolean();
		float mass = physics.get("mass").asFloat();
		
		// create new scaled shape and get inertia
		
		CollisionShape shape = makeShape(mesh, boundsType, margin, isCompound);
		shape.setLocalScaling(sca);
		Vector3f inertia = new Vector3f();
		shape.calculateLocalInertia(mass, inertia);

		// create new motion state

		Matrix4f originMatrix = new Matrix4f();
		originMatrix.set(origin);
		t.mul(originMatrix);
		Transform centerOfMassTransform = new Transform(t);
		MotionState motionState = new DefaultMotionState(transform, centerOfMassTransform);
		
		// create new body
		
		RigidBodyConstructionInfo ci = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
		RigidBody body = new RigidBody(ci);
		
		// set transform
		
		body.setWorldTransform(transform);
		
		// set collision flags
		
		int flags = 0;
		if (bodyType == BodyType.SENSOR){
			flags = CollisionFlags.KINEMATIC_OBJECT | CollisionFlags.NO_CONTACT_RESPONSE;
		}else{
			if (bodyType == BodyType.STATIC){
				flags = CollisionFlags.KINEMATIC_OBJECT;
				//body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
			}else if (bodyType == BodyType.DYNAMIC){
				body.setAngularFactor(0);
			}
			if (physics.get("ghost").asBoolean())
				flags |= CollisionFlags.NO_CONTACT_RESPONSE;
		}
		body.setCollisionFlags(flags);
		
		// set restitution and friction
		
		body.setRestitution(physics.get("restitution").asFloat());
		body.setFriction(physics.get("friction").asFloat());

		return body;
	}

	public static RigidBody cloneBody(RigidBody body){
		
		// get gobj and shape
		
		GameObject gobj = (GameObject) body.getUserPointer();
		CollisionShape shape = body.getCollisionShape();
		
		// collect physics properties
		
		Vector3f origin = gobj.origin;
		float margin = shape.getMargin();
		boolean isCompound = shape.isCompound();
		BoundsType boundsType = gobj.boundsType();
		float mass = gobj.mass();
		Vector3f inertia = new Vector3f();
		shape.calculateLocalInertia(mass, inertia);
		
		// create new shape
		
		if (gobj.modelInstance != null){
			shape = makeShape(gobj.mesh(), boundsType, margin, isCompound);
		}else{
			shape = new BoxShape(new Vector3f(0.25f, 0.25f, 0.25f));
		}
		
		// create new motion state
		
		Transform startTransform = body.getMotionState().getWorldTransform(new Transform());
		Transform centerOfMassTransform = body.getCenterOfMassTransform(new Transform());
		MotionState motionState = new DefaultMotionState(startTransform, centerOfMassTransform);
		
		// create new body
		
		RigidBody b = new RigidBody(mass, motionState, shape, inertia);
		
		b.setCollisionFlags(gobj.body.getCollisionFlags());
		b.setAngularFactor(gobj.body.getAngularFactor());
		b.setRestitution(gobj.body.getRestitution());
		b.setFriction(gobj.body.getFriction());
		
		return b;
	}
}
