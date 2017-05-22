/*
 * $RCSfile: Vector2f.java,v $
 *
 * Copyright 1997-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 * $Revision: 1.5 $
 * $Date: 2008/02/28 20:18:51 $
 * $State: Exp $
 */

package javax.vecmath;

import java.lang.Math;

/**
 * A 2-element vector that is represented by single-precision floating point x,y
 * coordinates.
 * 
 */
public class Vector2f extends Tuple2f implements java.io.Serializable {

	// Combatible with 1.1
	static final long serialVersionUID = -2168194326883512320L;

	/**
	 * Constructs and initializes a Vector2f from the specified xy coordinates.
	 * 
	 * @param x
	 *          the x coordinate
	 * @param y
	 *          the y coordinate
	 */
	public Vector2f(float x, float y) {
		super(x, y);
	}

	/**
	 * Constructs and initializes a Vector2f from the specified array.
	 * 
	 * @param v
	 *          the array of length 2 containing xy in order
	 */
	public Vector2f(float[] v) {
		super(v);
	}

	/**
	 * Constructs and initializes a Vector2f from the specified Vector2f.
	 * 
	 * @param v1
	 *          the Vector2f containing the initialization x y data
	 */
	public Vector2f(Vector2f v1) {
		super(v1);
	}

	/**
	 * Constructs and initializes a Vector2f from the specified Vector2d.
	 * 
	 * @param v1
	 *          the Vector2d containing the initialization x y data
	 */
	public Vector2f(Vector2d v1) {
		super(v1);
	}

	/**
	 * Constructs and initializes a Vector2f from the specified Tuple2f.
	 * 
	 * @param t1
	 *          the Tuple2f containing the initialization x y data
	 */
	public Vector2f(Tuple2f t1) {
		super(t1);
	}

	/**
	 * Constructs and initializes a Vector2f from the specified Tuple2d.
	 * 
	 * @param t1
	 *          the Tuple2d containing the initialization x y data
	 */
	public Vector2f(Tuple2d t1) {
		super(t1);
	}

	/**
	 * Constructs and initializes a Vector2f to (0,0).
	 */
	public Vector2f() {
		super();
	}

	/**
	 * Computes the dot product of the this vector and vector v1.
	 * 
	 * @param v1
	 *          the other vector
	 */
	public final float dot(Vector2f v1) {
		return (this.x * v1.x + this.y * v1.y);
	}

	/**
	 * Returns the length of this vector.
	 * 
	 * @return the length of this vector
	 */
	public final float length() {
		return (float) Math.sqrt(this.x * this.x + this.y * this.y);
	}

	/**
	 * Returns the squared length of this vector.
	 * 
	 * @return the squared length of this vector
	 */
	public final float lengthSquared() {
		return (this.x * this.x + this.y * this.y);
	}

	/**
	 * Sets the value of this vector to the normalization of vector v1.
	 * 
	 * @param v1
	 *          the un-normalized vector
	 */
	public final void normalize(Vector2f v1) {
		float norm;

		norm = (float) (1.0 / Math.sqrt(v1.x * v1.x + v1.y * v1.y));
		this.x = v1.x * norm;
		this.y = v1.y * norm;
	}

	/**
	 * Normalizes this vector in place.
	 */
	public final void normalize() {
		float norm;

		norm = (float) (1.0 / Math.sqrt(this.x * this.x + this.y * this.y));
		this.x *= norm;
		this.y *= norm;
	}

	public final Vector2f normalized(){
		Vector2f a = new Vector2f(this);
		a.normalize();
		return a;
	}

	/**
	 * Returns the angle in radians between this vector and the vector parameter;
	 * the return value is constrained to the range [0,PI].
	 * 
	 * @param v1
	 *          the other vector
	 * @return the angle in radians in the range [0,PI]
	 */
	public final float angle(Vector2f v1) {
		double vDot = this.dot(v1) / (this.length() * v1.length());
		if (vDot < -1.0)
			vDot = -1.0;
		if (vDot > 1.0)
			vDot = 1.0;
		return ((float) (Math.acos(vDot)));
	}

   /**
    * BDX conveniences:
    */

   public final float angle(){
	   return angle(new Vector2f(1, 0));
   }

	public final float angleSigned(Vector2f refVec){
		Vector2f rot = new Vector2f(-refVec.y, refVec.x);
		float d = dot(rot);
		if (d < 0)
			return -angle(refVec);
		return angle(refVec);
	}

	public final float angleSigned(){
		return angleSigned(new Vector2f(1, 0));
	}


	public final Vector2f rotated(float radians) {

	   Vector3f to3D = new Vector3f(x, y, 0);
	   to3D.set(Matrix3f.rotation(new Vector3f(0, 0, 1), radians).mult(to3D));
	   return new Vector2f(to3D.x, to3D.y);

   }

	public final void rotate(float radians){
		this.set(rotated(radians));
	}


	public final Vector2f plus(Vector2f b){
	   Vector2f a = new Vector2f(this);
	   a.add(b);
	   return a;
   }

	public final Vector2f plus(float x, float y) {
		Vector2f a = new Vector2f(this);
		a.add(x, y);
		return a;
	}

   public final Vector2f minus(Vector2f b){
	   Vector2f a = new Vector2f(this);
	   a.sub(b);
	   return a;
   }

   public final Vector2f minus(float x, float y) {
   		Vector2f a = new Vector2f(this);
   		a.sub(x, y);
   		return a;
   }

   public final void length(float n){
	   if (length() == 0)
		   return;
	   normalize();
	   scale(n);
   }

   public final Vector2f negated(){
	   Vector2f v = new Vector2f(this);
	   v.negate();
	   return v;
   }

   public final Vector2f mul(float n){
	   Vector2f v = new Vector2f(this);
	   v.x *= n;
	   v.y *= n;
	   return v;
   }
   
   public final Vector2f mul(Vector2f b){
	   Vector2f a = new Vector2f(this);
	   a.x *= b.x;
	   a.y *= b.y;
	   return a;
   }

   public final Vector2f div(Vector2f b){
	   Vector2f a = new Vector2f(this);
	   a.x /= b.x;
	   a.y /= b.y;
	   return a;
   }
   
   public final Vector2f reflected(Vector2f normal){
	   return normal.mul(-2 * this.dot(normal)).plus(this);  
   }

}
