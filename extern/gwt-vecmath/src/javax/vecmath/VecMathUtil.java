/*
 * $RCSfile: VecMathUtil.java,v $
 *
 * Copyright 2004-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

/**
 * Utility vecmath class used when computing the hash code for vecmath objects
 * containing float or double values. This fixes Issue 36.
 */
class VecMathUtil {
	/**
	 * Returns the representation of the specified floating-point value according
	 * to the IEEE 754 floating-point "single format" bit layout, after first
	 * mapping -0.0 to 0.0. This method is identical to
	 * Float.floatToIntBits(float) except that an integer value of 0 is returned
	 * for a floating-point value of -0.0f. This is done for the purpose of
	 * computing a hash code that satisfies the contract of hashCode() and
	 * equals(). The equals() method in each vecmath class does a pair-wise "=="
	 * test on each floating-point field in the class (e.g., x, y, and z for a
	 * Tuple3f). Since 0.0f&nbsp;==&nbsp;-0.0f returns true, we must also return
	 * the same hash code for two objects, one of which has a field with a value
	 * of -0.0f and the other of which has a cooresponding field with a value of
	 * 0.0f.
	 * 
	 * @param f
	 *          an input floating-point number
	 * @return the integer bits representing that floating-point number, after
	 *         first mapping -0.0f to 0.0f
	 */
	static long floatToIntBits(float f) {
		// Check for +0 or -0
		if (f == 0.0f) {
			return 0;
		} else {
			return doubleToLongBitsImpl(f);
		}
	}

	/**
	 * Returns the representation of the specified floating-point value according
	 * to the IEEE 754 floating-point "double format" bit layout, after first
	 * mapping -0.0 to 0.0. This method is identical to
	 * Double.doubleToLongBits(double) except that an integer value of 0L is
	 * returned for a floating-point value of -0.0. This is done for the purpose
	 * of computing a hash code that satisfies the contract of hashCode() and
	 * equals(). The equals() method in each vecmath class does a pair-wise "=="
	 * test on each floating-point field in the class (e.g., x, y, and z for a
	 * Tuple3d). Since 0.0&nbsp;==&nbsp;-0.0 returns true, we must also return the
	 * same hash code for two objects, one of which has a field with a value of
	 * -0.0 and the other of which has a cooresponding field with a value of 0.0.
	 * 
	 * @param d
	 *          an input double precision floating-point number
	 * @return the integer bits representing that floating-point number, after
	 *         first mapping -0.0f to 0.0f
	 */
	static long doubleToLongBits(double d) {
		// Check for +0 or -0
		if (d == 0.0) {
			return 0L;
		} else {
			return doubleToLongBitsImpl(d);
		}
	}
	
	/**
	 * Implementation of doubleToLongBits as GWT does not provide that.
	 * Reference: http://markmail.org/message/mqp52x6lukels2sd
	 * 
	 * @param v
	 * @return
	 */
	private static long doubleToLongBitsImpl(double v) {
		if (Double.isNaN(v)) {
			// IEEE754, NaN exponent bits all 1s, and mantissa is non-zero
			return 0x0FFFl << 51;
		}

		long sign = (v < 0 ? 0x1l << 63 : 0);
		long exponent = 0;

		double absV = Math.abs(v);
		// IEEE754 infinite numbers, exponent all 1s, mantissa is 0
		if (Double.isInfinite(v)) {
			exponent = 0x07FFl << 52;
		} else {
			if (absV == 0.0) {
				// IEEE754, exponent is 0, mantissa is zero
				// we don't handle negative zero at the moment, it is treated as
				// positive zero
				exponent = 0l;
			} else {
				// get an approximation to the exponent
				int guess = (int) Math.floor(Math.log(absV) / Math.log(2));
				// force it to -1023, 1023 interval (<= -1023 = denorm/zero)
				guess = Math.max(-1023, Math.min(guess, 1023));

				// divide away exponent guess
				double exp = Math.pow(2, guess);
				absV = absV / exp;

				// while the number is still bigger than a normalized number
				// increment exponent guess
				while (absV > 2.0) {
					guess++;
					absV /= 2.0;
				}
				// if the number is smaller than a normalized number
				// decrement exponent
				while (absV < 1 && guess > 1024) {
					guess--;
					absV *= 2;
				}
				exponent = (guess + 1023l) << 52;
			}
		}
		// if denorm
		if (exponent <= 0) {
			absV /= 2;
		}

		// the input value has now been stripped of its exponent
		// and is in the range [0,2), we strip off the leading decimal
		// and use the remainer as a percentage of the significand value (2^52)
		long mantissa = (long) ((absV % 1) * Math.pow(2, 52));
		return sign | exponent | (mantissa & 0xfffffffffffffl);
	}

	/**
	 * Do not construct an instance of this class.
	 */
	private VecMathUtil() {
	}
}
