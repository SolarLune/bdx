package com.bulletphysics.util;

import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;


public class Suppliers {

	public static final Supplier<Vector3f> NEW_VECTOR3F_SUPPLIER = new Supplier<Vector3f>() {
		@Override
		public Vector3f get() {
			return new Vector3f();
		}
	};

	public static final Supplier<Transform> NEW_TRANSFORM_SUPPLIER = new Supplier<Transform>() {
		@Override
		public Transform get() {
			return new Transform();
		}
	};

}
