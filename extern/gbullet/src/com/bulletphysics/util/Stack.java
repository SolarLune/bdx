package com.bulletphysics.util;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.bulletphysics.extras.gimpact.BoxCollision.AABB;
import com.bulletphysics.extras.gimpact.BoxCollision.BoxBoxTransformCache;
import com.bulletphysics.extras.gimpact.PrimitiveTriangle;
import com.bulletphysics.extras.gimpact.TriangleContact;
import com.bulletphysics.linearmath.Transform;

public class Stack {

	private static final int TYPE_VECTOR3F = 0;
	private static final int TYPE_VECTOR4F = 1;
	private static final int TYPE_AABB = 2;
	private static final int TYPE_TRANSFORM = 3;
	private static final int TYPE_MATRIX3F = 4;
	private static final int TYPE_QUAT4F = 5;
	private static final int TYPE_BOX_BOX_TRANSFORM_CACHE = 6;
	private static final int TYPE_PRIMITIVE_TRIANGLE = 7;
	private static final int TYPE_TRIANGLE_CONTACT = 8;

	private static ThreadLocal<Stack> stack = new ThreadLocal<Stack>();

	private final int[] stackPositions = new int[9];
	private final int[] types = new int[65536];
	private int sp;

	private final ObjectArrayList<Vector3f> vector3fStack = new ObjectArrayList<Vector3f>();
	private final ObjectArrayList<Vector4f> vector4fStack = new ObjectArrayList<Vector4f>();
	private final ObjectArrayList<AABB> aabbStack = new ObjectArrayList<AABB>();
	private final ObjectArrayList<Transform> transformStack = new ObjectArrayList<Transform>();
	private final ObjectArrayList<Matrix3f> matrix3fStack = new ObjectArrayList<Matrix3f>();
	private final ObjectArrayList<Quat4f> quat4fStack = new ObjectArrayList<Quat4f>();
	private final ObjectArrayList<BoxBoxTransformCache> boxBoxTransformCacheStack = new ObjectArrayList<BoxBoxTransformCache>();
	private final ObjectArrayList<PrimitiveTriangle> primitiveTriangleStack = new ObjectArrayList<PrimitiveTriangle>();
	private final ObjectArrayList<TriangleContact> triangleContactStack = new ObjectArrayList<TriangleContact>();

	public static void libraryCleanCurrentThread() {
		Stack stack = Stack.stack.get();
		if (stack != null) {
			stack.sp = 0;
			for (int i = 0; i < stack.stackPositions.length; i++) {
				stack.stackPositions[i] = 0;
			}
		}
	}

	public Vector3f alloc(Vector3f original) {
		Vector3f v = allocVector3f();
		v.set(original);
		return v;
	}

	public Transform alloc(Transform original) {
		Transform t = allocTransform();
		t.set(original);
		return t;
	}

	public Matrix3f alloc(Matrix3f original) {
		Matrix3f m = allocMatrix3f();
		m.set(original);
		return m;
	}

	public AABB alloc(AABB box) {
		AABB aabb = allocAABB();
		aabb.set(box);
		return aabb;
	}

	public Quat4f alloc(Quat4f rotation) {
		Quat4f q = allocQuat4f();
		q.set(rotation);
		return q;
	}

	public Vector3f allocVector3f() {
		types[sp++] = TYPE_VECTOR3F;
		int pos = stackPositions[TYPE_VECTOR3F]++;
		if (vector3fStack.size() <= pos) {
			vector3fStack.add(new Vector3f());
		}
		return vector3fStack.get(pos);
	}

	public Matrix3f allocMatrix3f() {
		types[sp++] = TYPE_MATRIX3F;
		int pos = stackPositions[TYPE_MATRIX3F]++;
		if (matrix3fStack.size() <= pos) {
			matrix3fStack.add(new Matrix3f());
		}
		return matrix3fStack.get(pos);
	}

	public Quat4f allocQuat4f() {
		types[sp++] = TYPE_QUAT4F;
		int pos = stackPositions[TYPE_QUAT4F]++;
		if (quat4fStack.size() <= pos) {
			quat4fStack.add(new Quat4f());
		}
		return quat4fStack.get(pos);
	}

	public Transform allocTransform() {
		types[sp++] = TYPE_TRANSFORM;
		int pos = stackPositions[TYPE_TRANSFORM]++;
		if (transformStack.size() <= pos) {
			transformStack.add(new Transform());
		}
		return transformStack.get(pos);
	}

	public Vector4f allocVector4f() {
		types[sp++] = TYPE_VECTOR4F;
		int pos = stackPositions[TYPE_VECTOR4F]++;
		if (vector4fStack.size() <= pos) {
			vector4fStack.add(new Vector4f());
		}
		return vector4fStack.get(pos);
	}

	public AABB allocAABB() {
		types[sp++] = TYPE_AABB;
		int pos = stackPositions[TYPE_AABB]++;
		if (aabbStack.size() <= pos) {
			aabbStack.add(new AABB());
		}
		return aabbStack.get(pos);
	}

	public BoxBoxTransformCache allocBoxBoxTransformCache() {
		types[sp++] = TYPE_BOX_BOX_TRANSFORM_CACHE;
		int pos = stackPositions[TYPE_BOX_BOX_TRANSFORM_CACHE]++;
		if (boxBoxTransformCacheStack.size() <= pos) {
			boxBoxTransformCacheStack.add(new BoxBoxTransformCache());
		}
		return boxBoxTransformCacheStack.get(pos);
	}

	public PrimitiveTriangle allocPrimitiveTriangle() {
		types[sp++] = TYPE_PRIMITIVE_TRIANGLE;
		int pos = stackPositions[TYPE_PRIMITIVE_TRIANGLE]++;
		if (primitiveTriangleStack.size() <= pos) {
			primitiveTriangleStack.add(new PrimitiveTriangle());
		}
		return primitiveTriangleStack.get(pos);
	}

	public TriangleContact allocTriangleContact() {
		types[sp++] = TYPE_TRIANGLE_CONTACT;
		int pos = stackPositions[TYPE_TRIANGLE_CONTACT]++;
		if (triangleContactStack.size() <= pos) {
			triangleContactStack.add(new TriangleContact());
		}
		return triangleContactStack.get(pos);
	}

	public static Stack enter() {
		Stack stack = Stack.stack.get();
		if (stack == null) {
			stack = new Stack();
			Stack.stack.set(stack);
		}
		stack.types[stack.sp++] = -1;
		return stack;
	}
	
	public int getSp() {
		return sp;
	}
	
	public void leave() {
		while(true) {
			int type = types[--sp];
			if (type == -1) {
				break;
			}
			stackPositions[type]--;
		}
	}

	public void leave(int savedSp) {
		for (int i = savedSp; i < sp; i++) {
			int type = types[i];
			if (type != -1) {
				stackPositions[type]--;
			}
		}
		this.sp = savedSp - 1;
		assert types[sp] == -1;
	}

}
