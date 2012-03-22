/**
 * @file
 * @version 0.3
 * Copyright © Kool Banana
 *
 * @section LICENSE
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details at
 * http://www.gnu.org/copyleft/gpl.html
 */

package alice;

import java.util.List;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.UnitDef;

/**
 * Various help methods used for Spring methods.
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class SpringHelper {

	/**
	 * Returns the distance of the positions on the map
	 * 
	 * @param mapX1
	 *            X-pos of the first position
	 * @param mapY1
	 *            Y-pos of the first position
	 * @param mapX2
	 *            X-pos of the second position
	 * @param mapY2
	 *            Y-pos of the second position
	 * @return The distance of the two positions on the map
	 */
	public static float getDist(float mapX1, float mapY1, float mapX2, float mapY2) {
		// Correct version. CPU intensive
		/*
		 * float Asquare = (mapX1 - mapX2) * (mapX1 - mapX2); float Bsquare =
		 * (mapY1 - mapY2) * (mapY1 - mapY2); return Math.sqrt((double)Asquare +
		 * (double)Bsquare);
		 */

		// Optimized version
		float lX = mapX1 - mapX2;
		if (lX < 0) {
			lX *= -1;
		}
		float lY = mapY1 - mapY2;
		if (lY < 0) {
			lY *= -1;
		}

		float max = lX;
		float min = lY;
		if (lY > max) {
			max = lY;
			min = lX;
		}

		int index = (int) (min / max * (float) 100);
		float appD = (max * preCalc[index]);
		return appD;
	}

	/**
	 * Returns the distance between the positions on the map. Does not use the
	 * y-value
	 * 
	 * @param positionOne
	 *            the first position
	 * @param positionTwo
	 *            the second position
	 * @return the distance between the two positions on the map
	 */
	public static float getDist(AIFloat3 positionOne, AIFloat3 positionTwo) {
		return getDist(positionOne.x, positionOne.z, positionTwo.x, positionTwo.z);
	}

	/**
	 * The number of seconds per frame.
	 */
	public static final double SECONDS_PER_FRAME = 1.0 / 30.0;

	/**
	 * The number of minutes per frame.
	 */
	public static final double MINUTES_PER_FRAME = SECONDS_PER_FRAME / 60.0;

	/**
	 * Returns the UnitDef of the buildDef if we can build it, else null.
	 * 
	 * @param unitDef
	 *            The builder
	 * @param buildDef
	 *            The definition of the building/unit that should be created
	 * @return UnitDef of the buildDef if we can build it, else null.
	 */
	public static UnitDef canBuild(UnitDef unitDef, String buildDef) {

		// Check if the builder can build the specified unit
		if (unitDef != null) {
			List<UnitDef> buildOptions = unitDef.getBuildOptions();
			for (UnitDef def : buildOptions) {
				if (buildDef.equals(def.getName())) {
					return def;
				}
			}
		}
		return null;

	}

	/**
	 * Convert unit coordinate to map coordinate (1 -> 1/8)
	 * 
	 * @param unitPos
	 *            the unit position
	 * @return the map position of the unit position
	 */
	public static int unitToMapPos(float unitPos) {
		return (int) (unitPos / 8);
	}

	/**
	 * Convert map coordinate to unit coordinate (1 -> 8)
	 * 
	 * @param mapPos
	 *            the map position of the unit
	 * @return the unit's position
	 */
	public static float mapToUnitPos(int mapPos) {
		return mapPos * 8 + 4;
	}

	/**
	 * Convert map coordinate to maptile coordinate (1 -> 1/8)
	 * 
	 * @param mapPos
	 *            the map position
	 * @return the tile position of the map position
	 */
	public static int mapToTilePos(int mapPos) {
		return (int) (mapPos / 8);
	}

	/**
	 * Convert maptile coordinate to map coordinate (1 -> 8)
	 * 
	 * @param tilePos
	 *            the tile position
	 * @return the map position of the map
	 */
	public static int tileToMapPos(int tilePos) {
		return tilePos * 8 + 4;
	}

	/**
	 * Convert unit coordinate to map tile coordinate (1 -> 1/8 * 1/8)
	 * 
	 * @param unitPos
	 *            the unit position
	 * @return the unit's position on the tile
	 */
	public static int unitToTilePos(float unitPos) {
		return mapToTilePos(unitToMapPos(unitPos));
	}

	/**
	 * Convert map tile coordinate to unit coordinate (1 -> 8 * 8)
	 * 
	 * @param tilePos
	 *            the tile position of the unit
	 * @return the unit position
	 */
	public static float tileToUnitPos(int tilePos) {
		return tileToMapPos((int) mapToUnitPos(tilePos));
	}

	/**
	 * Returns the real radius of the unit
	 * 
	 * @param radius
	 *            The LosRadius of a unit
	 * @return The real radius of a unit
	 */
	public static float getRealRadius(float radius) {
		// Y-tile = 8
		// X-tile = 8
		// 8 * 8 = 64 and the radius of a unit need to be multiplied with
		// this...
		return radius * 64;
	}

	/**
	 * Returns a new AIFloat3 containing the difference between the two vectors
	 * 
	 * @param left
	 *            the left side of the difference, i.e. the one we substract
	 *            from.
	 * @param right
	 *            the right side of the difference, i.e. the one we substract
	 *            with.
	 * @return new vector with the difference between left and right.
	 */
	public static AIFloat3 getDiff(AIFloat3 left, AIFloat3 right) {
		AIFloat3 diffVec = new AIFloat3(left.x, left.y, left.z);
		diffVec.x -= right.x;
		diffVec.y -= right.y;
		diffVec.z -= right.z;
		return diffVec;
	}

	/**
	 * Returns a new AIFloat3 containing the sum of the two vectors
	 * 
	 * @param left
	 *            the left side of the addition
	 * @param right
	 *            the right side of the addition
	 * @return new vector with the sum of the two vectors
	 */
	public static AIFloat3 getSum(AIFloat3 left, AIFloat3 right) {
		AIFloat3 sumVec = new AIFloat3(left.x, left.y, left.z);
		sumVec.x += right.x;
		sumVec.y += right.y;
		sumVec.z += right.z;
		return sumVec;
	}

	/**
	 * Returns a new AIFloat3 containing the product of the vector and value
	 * 
	 * @param vec
	 *            the vector
	 * @param value
	 *            the value to multiply with the vector
	 * @return new vector with the product of the vector and value
	 */
	public static AIFloat3 getProduct(AIFloat3 vec, float value) {
		AIFloat3 productVec = new AIFloat3(vec.x, vec.y, vec.z);
		productVec.x *= value;
		productVec.y *= value;
		productVec.z *= value;
		return productVec;
	}

	/**
	 * Normalizes an AIFloat3 vector.
	 * 
	 * @pre vec isn't a 0-vector and vec isn't null
	 * @param vec
	 *            the vector to normalize
	 * @param useY
	 *            if we should use y or not. If we don't use y it will be set to
	 *            0
	 */
	public static void normalizeVec(AIFloat3 vec, boolean useY) {
		if (!useY) {
			vec.y = 0;
		}
		float length = (float) Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);
		vec.x /= length;
		vec.y /= length;
		vec.z /= length;
	}

	/**
	 * Pre-calculations
	 */
	private static float[] preCalc = { 1.00f, 1.00f, 1.00f, 1.001f, 1.001f, 1.001f, 1.002f, 1.003f, 1.004f, 1.004f, 1.005f,
			1.006f, 1.008f, 1.009f, 1.01f, 1.012f, 1.013f, 1.015f, 1.017f, 1.019f, 1.021f, 1.023f, 1.025f, 1.027f, 1.029f,
			1.032f, 1.034f, 1.037f, 1.04f, 1.042f, 1.045f, 1.048f, 1.051f, 1.054f, 1.058f, 1.061f, 1.064f, 1.068f, 1.071f,
			1.075f, 1.079f, 1.082f, 1.086f, 1.09f, 1.094f, 1.098f, 1.103f, 1.107f, 1.111f, 1.116f, 1.12f, 1.125f, 1.129f, 1.134f,
			1.139f, 1.143f, 1.148f, 1.153f, 1.158f, 1.163f, 1.169f, 1.174f, 1.179f, 1.184f, 1.19f, 1.195f, 1.201f, 1.206f,
			1.212f, 1.218f, 1.223f, 1.229f, 1.235f, 1.241f, 1.247f, 1.253f, 1.259f, 1.265f, 1.271f, 1.277f, 1.284f, 1.29f,
			1.296f, 1.303f, 1.309f, 1.316f, 1.322f, 1.329f, 1.335f, 1.342f, 1.349f, 1.355f, 1.362f, 1.369f, 1.376f, 1.383f,
			1.39f, 1.397f, 1.404f, 1.411f, 1.414f };

}
