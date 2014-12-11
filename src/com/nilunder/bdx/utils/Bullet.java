package com.nilunder.bdx.utils;

import java.nio.ByteBuffer;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.JsonValue;
import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.ScalarType;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.nilunder.bdx.GameObject;

public class Bullet {
	
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
		m.vertexStride = (Float.SIZE/8) * 5;
		
		return m;
	}
	
	public static RigidBody makeBody(Mesh mesh, float[] glTransform, JsonValue physics){
		CollisionShape shape;
		
		String bounds = physics.get("bounds").asString();
		
		if (bounds.equals("TRIANGLE_MESH")){
			TriangleIndexVertexArray mi = new TriangleIndexVertexArray();
			mi.addIndexedMesh(Bullet.makeMesh(mesh), ScalarType.SHORT);
			shape = new BvhTriangleMeshShape(mi, false);
			
		}else if (bounds.equals("SPHERE")){
			float radius = mesh.calculateRadius(0f, 0f, 0f);
			shape = new SphereShape(radius);
			
		}else{ // BOX
			BoundingBox bbox = mesh.calculateBoundingBox();
			Vector3 d = bbox.getDimensions().scl(0.5f);
			Vector3f dim = new Vector3f(d.x, d.y, d.z);
			shape = new BoxShape(dim);
		}
		
		float mass = physics.get("mass").asFloat();
		String body_type = physics.get("body").asString();
		
		Vector3f inertia = new Vector3f();
		shape.calculateLocalInertia(mass, inertia);
		
		Transform startTransform = new Transform();
		startTransform.setFromOpenGLMatrix(glTransform);
		MotionState motionState = new DefaultMotionState(startTransform);
		
		RigidBodyConstructionInfo ci = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
		
		RigidBody body = new RigidBody(ci);
		
		if (body_type.equals("STATIC")){
			body.setCollisionFlags(CollisionFlags.KINEMATIC_OBJECT);
			//body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
		}else if (body_type.equals("DYNAMIC")){
			body.setAngularFactor(0f);
		}
		
		if (body_type.equals("SENSOR") || physics.get("ghost").asBoolean()){
			body.setCollisionFlags(CollisionFlags.NO_CONTACT_RESPONSE);
		}
		
		body.setRestitution(physics.get("restitution").asFloat());
		body.setFriction(physics.get("friction").asFloat());

		return body;
	}

	public static RigidBody cloneBody(RigidBody body){
		GameObject gobj = (GameObject)body.getUserPointer();
		float mass = gobj._json.get("physics").get("mass").asFloat();
		
		Vector3f inertia = new Vector3f();
		body.getCollisionShape().calculateLocalInertia(mass, inertia);
		
		RigidBody b = new RigidBody(mass, new DefaultMotionState(new Transform(gobj.transform())),
								body.getCollisionShape(),
								inertia);
		
		b.setCollisionFlags(gobj.body.getCollisionFlags());
		b.setAngularFactor(gobj.body.getAngularFactor());
		b.setRestitution(gobj.body.getRestitution());
		b.setFriction(gobj.body.getFriction());
		
		return b;
	}
	
	public static void normalizeBasis(Matrix3f m){
		Vector3f v = new Vector3f();
		for (int i = 0; i < 3; ++i){
			m.getColumn(i, v);
			v.normalize();
			m.setColumn(i, v);
		}
	}
	
	public static float vecAngle(Vector3f a, Vector3f b){
		return (float)Math.acos(a.dot(b) / (a.length() * b.length()));
	}
	
	public static Matrix3f rotMatrix(Vector3f axis, float angle){
		AxisAngle4f aa = new AxisAngle4f(axis, angle);
		Matrix3f m = new Matrix3f();
		m.set(aa);
		return m;
	}
}
