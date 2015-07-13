/*
 * $RCSfile: VecMathI18N.java,v $
 *
 * Copyright 1998-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

class VecMathI18N {
	private static Map<String, String> exceptionMessages;
	
	static String getString(String key) {
		if (exceptionMessages == null) {
			exceptionMessages = new HashMap<String, String>();
			exceptionMessages.put("Matrix3d0", "Matrix3d setElement");
			exceptionMessages.put("Matrix3d1", "Matrix3d getElement");
			exceptionMessages.put("Matrix3d2", "Matrix3d getRow");
			exceptionMessages.put("Matrix3d4", "Matrix3d getColumn");
			exceptionMessages.put("Matrix3d6", "Matrix3d setRow");
			exceptionMessages.put("Matrix3d9", "Matrix3d setColumn");
			exceptionMessages.put("Matrix3d12", "cannot invert matrix");
			exceptionMessages.put("Matrix3d13", "Logic error: imax < 0");
			exceptionMessages.put("Matrix3f0", "Matrix3f setElement");
			exceptionMessages.put("Matrix3f1", "Matrix3d getRow");
			exceptionMessages.put("Matrix3f3", "Matrix3d getColumn");
			exceptionMessages.put("Matrix3f5", "Matrix3f getElement");
			exceptionMessages.put("Matrix3f6", "Matrix3f setRow");
			exceptionMessages.put("Matrix3f9", "Matrix3f setColumn");
			exceptionMessages.put("Matrix3f12", "cannot invert matrix");
			exceptionMessages.put("Matrix3f13", "Logic error: imax < 0");
			exceptionMessages.put("Matrix4d0", "Matrix4d setElement");
			exceptionMessages.put("Matrix4d1", "Matrix4d getElement");
			exceptionMessages.put("Matrix4d2", "Matrix4d getRow");
			exceptionMessages.put("Matrix4d3", "Matrix4d getColumn");
			exceptionMessages.put("Matrix4d4", "Matrix4d setRow");
			exceptionMessages.put("Matrix4d7", "Matrix4d setColumn");
			exceptionMessages.put("Matrix4d10", "cannot invert matrix");
			exceptionMessages.put("Matrix4d11", "Logic error: imax < 0");
			exceptionMessages.put("Matrix4f0", "Matrix4f setElement");
			exceptionMessages.put("Matrix4f1", "Matrix4f getElement");
			exceptionMessages.put("Matrix4f2", "Matrix4f getRow");
			exceptionMessages.put("Matrix4f4", "Matrix4f getColumn");
			exceptionMessages.put("Matrix4f6", "Matrix4f setRow");
			exceptionMessages.put("Matrix4f9", "Matrix4f setColumn");
			exceptionMessages.put("Matrix4f12", "cannot invert matrix");
			exceptionMessages.put("Matrix4f13", "Logic error: imax < 0");
			exceptionMessages.put("GMatrix0", "GMatrix.mul:array dimension mismatch ");
			exceptionMessages.put("GMatrix1", "GMatrix.mul(GMatrix, GMatrix) dimension mismatch ");
			exceptionMessages.put("GMatrix2", "GMatrix.mul(GVector, GVector): matrix does not have enough rows ");
			exceptionMessages.put("GMatrix3", "GMatrix.mul(GVector, GVector): matrix does not have enough columns ");
			exceptionMessages.put("GMatrix4", "GMatrix.add(GMatrix): row dimension mismatch ");
			exceptionMessages.put("GMatrix5", "GMatrix.add(GMatrix): column dimension mismatch ");
			exceptionMessages.put("GMatrix6", "GMatrix.add(GMatrix, GMatrix): row dimension mismatch ");
			exceptionMessages.put("GMatrix7", "GMatrix.add(GMatrix, GMatrix): column dimension mismatch ");
			exceptionMessages.put("GMatrix8", "GMatrix.add(GMatrix): input matrices dimensions do not match this matrix dimensions");
			exceptionMessages.put("GMatrix9", "GMatrix.sub(GMatrix): row dimension mismatch ");
			exceptionMessages.put("GMatrix10", "GMatrix.sub(GMatrix, GMatrix): row dimension mismatch ");
			exceptionMessages.put("GMatrix11", "GMatrix.sub(GMatrix, GMatrix): column dimension mismatch ");
			exceptionMessages.put("GMatrix12", "GMatrix.sub(GMatrix, GMatrix): input matrix dimensions do not match dimensions for this matrix ");
			exceptionMessages.put("GMatrix13", "GMatrix.negate(GMatrix, GMatrix): input matrix dimensions do not match dimensions for this matrix ");
			exceptionMessages.put("GMatrix14", "GMatrix.mulTransposeBoth matrix dimension mismatch");
			exceptionMessages.put("GMatrix15", "GMatrix.mulTransposeRight matrix dimension mismatch");
			exceptionMessages.put("GMatrix16", "GMatrix.mulTransposeLeft matrix dimension mismatch");
			exceptionMessages.put("GMatrix17", "GMatrix.transpose(GMatrix) mismatch in matrix dimensions");
			exceptionMessages.put("GMatrix18", "GMatrix.SVD: dimension mismatch with V matrix");
			exceptionMessages.put("GMatrix19", "cannot perform LU decomposition on a non square matrix");
			exceptionMessages.put("GMatrix20", "row permutation must be same dimension as matrix");
			exceptionMessages.put("GMatrix21", "cannot invert matrix");
			exceptionMessages.put("GMatrix22", "cannot invert non square matrix");
			exceptionMessages.put("GMatrix24", "Logic error: imax < 0");
			exceptionMessages.put("GMatrix25", "GMatrix.SVD: dimension mismatch with U matrix");
			exceptionMessages.put("GMatrix26", "GMatrix.SVD: dimension mismatch with W matrix");
			exceptionMessages.put("GMatrix27", "LU must have same dimensions as this matrix");
			exceptionMessages.put("GMatrix28", "GMatrix.sub(GMatrix): column dimension mismatch");
			exceptionMessages.put("GVector0", "GVector.normalize( GVector) input vector and this vector lengths not matched");
			exceptionMessages.put("GVector1", "GVector.scale(double,  GVector) input vector and this vector lengths not matched");
			exceptionMessages.put("GVector2", "GVector.scaleAdd(GVector, GVector) input vector dimensions not matched");
			exceptionMessages.put("GVector3", "GVector.scaleAdd(GVector, GVector) input vectors and  this vector dimensions not matched");
			exceptionMessages.put("GVector4", "GVector.add(GVector) input vectors and  this vector dimensions not matched");
			exceptionMessages.put("GVector5", "GVector.add(GVector, GVector) input vector dimensions not matched");
			exceptionMessages.put("GVector6", "GVector.add(GVector, GVector) input vectors and  this vector dimensions not matched");
			exceptionMessages.put("GVector7", "GVector.sub(GVector) input vector and  this vector dimensions not matched");
			exceptionMessages.put("GVector8", "GVector.sub(GVector,  GVector) input vector dimensions not matched");
			exceptionMessages.put("GVector9", "GVector.sub(GMatrix,  GVector) input vectors and this vector dimensions not matched");
			exceptionMessages.put("GVector10", "GVector.mul(GMatrix,  GVector) matrix and vector dimensions not matched");
			exceptionMessages.put("GVector11", "GVector.mul(GMatrix,  GVector) matrix this vector dimensions not matched");
			exceptionMessages.put("GVector12", "GVector.mul(GVector, GMatrix) matrix and vector dimensions not matched");
			exceptionMessages.put("GVector13", "GVector.mul(GVector, GMatrix) matrix this vector dimensions not matched");
			exceptionMessages.put("GVector14", "GVector.dot(GVector) input vector and this vector have different sizes");
			exceptionMessages.put("GVector15", "matrix dimensions are not compatible ");
			exceptionMessages.put("GVector16", "b vector does not match matrix dimension ");
			exceptionMessages.put("GVector17", "GVector.interpolate(GVector, GVector, float) input vectors have different lengths ");
			exceptionMessages.put("GVector18", "GVector.interpolate(GVector, GVector, float) input vectors and this vector have different lengths");
			exceptionMessages.put("GVector19", "GVector.interpolate(GVector, float) input vector and this vector have different lengths");
			exceptionMessages.put("GVector20", "GVector.interpolate(GVector, GVector, double) input vectors have different lengths ");
			exceptionMessages.put("GVector21", "GVector.interpolate(GVector, GVector, double) input vectors and this vector have different lengths");
			exceptionMessages.put("GVector22", "GVector.interpolate(GVector,  double) input vectors and this vector have different lengths");
			exceptionMessages.put("GVector23", "matrix dimensions are not compatible");
			exceptionMessages.put("GVector24", "permutation vector does not match matrix dimension");
			exceptionMessages.put("GVector25", "LUDBackSolve non square matrix");
		}		
		String s = exceptionMessages.get(key);
		if (s == null) {
			System.err.println("VecMathI18N: Error looking up: " + key);
			s = key;
		}
		return s;
	}
}
