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

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import alice.interfaces.IEnemyDestroyed;
import alice.interfaces.IEnemyEnterLOS;
import alice.interfaces.IUnitCreated;
import alice.interfaces.IUnitDestroyed;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Resource;
import com.springrts.ai.oo.Unit;

/**
 * 
 * Handles and updates all the metal extraction points on the map
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class ExtractionPointMap implements IUnitCreated, IUnitDestroyed, IEnemyEnterLOS, IEnemyDestroyed {

	/**
	 * Constructor, adds all extraction points to the list and the owner to none
	 * 
	 * @param alIce
	 *            The AI-Interface
	 */
	public ExtractionPointMap(AlIce alIce) {
		mAlIce = alIce;
		mAlIce.addEventListener(this);
		mExtractionPoints = new Vector<ExtractionPoint>();
		Resource metal = mAlIce.getResource(Defs.Metal);
		List<AIFloat3> extractionPoints = new LinkedList<AIFloat3>();
		if (metal == null) {
			mAlIce.log(Level.SEVERE, "Could not find specified resource: " + Defs.Metal);
		} else {
			extractionPoints = mAlIce.getMap().getResourceMapSpotsPositions(metal);

			for (AIFloat3 pos : extractionPoints) {
				ExtractionPoint exPoint = new ExtractionPoint(pos, Owner.NONE);
				mExtractionPoints.add(exPoint);
			}
		}
		mAlIce.log(Level.FINE, "Size: " + mExtractionPoints.size());
	}

	/**
	 * Returns the number of extraction points by a specified owner, if owner =
	 * null, then returns the number of all the extraction points
	 * 
	 * @param owner
	 *            The ownership
	 * @return The number of extraction points
	 */
	public int getNumberOfExtractionPoints(Owner owner) {
		int result = 0;
		if (owner == null) {
			result = mExtractionPoints.size();
		} else {
			for (ExtractionPoint exPoint : mExtractionPoints) {
				if (owner == exPoint.mOwner) {
					result++;
				}
			}
		}

		return result;

	}

	/**
	 * A method to get all the free extraction points positions
	 * 
	 * @param owner
	 *            The specified owner the extraction point should belong to
	 * 
	 * @return A LinkedList containing all the free positions
	 */
	public LinkedList<AIFloat3> getExtractionPointPositionsByOwner(Owner owner) {
		LinkedList<AIFloat3> result = new LinkedList<AIFloat3>();

		for (ExtractionPoint exPoint : mExtractionPoints) {
			if (exPoint.mOwner == owner) {
				result.add(exPoint.mPosition);
			}
		}
		return result;
	}

	/**
	 * Returns the position of the closest extraction point by specified owner
	 * 
	 * @param position
	 *            The position of the unit
	 * @param owner
	 *            Ownership of the extraction point
	 * @return The position of the closest extraction point by specified owner
	 */
	public AIFloat3 getClosestExtractorByOwner(AIFloat3 position, Owner owner) {
		ExtractionPoint exPoint = getClosestExtractionPoint(position, owner);
		if (exPoint != null) {
			return exPoint.mPosition;
		} else {
			return null;
		}
	}

	/**
	 * Returns all the extraction points that are close to the specified
	 * position by the radius.
	 * 
	 * @param position
	 *            The position to check
	 * @param owner
	 *            The ownership to look for, null if you don't care about the
	 *            ownership and just want the closest extraction point
	 * @param radius
	 *            the radius to search for close extraction points
	 * @return All the extraction points that are close to the specified
	 *         position by the radius.
	 */
	public LinkedList<AIFloat3> getCloseExtractionPoints(AIFloat3 position, Owner owner, float radius) {
		LinkedList<AIFloat3> closeExtractionPoints = new LinkedList<AIFloat3>();

		if (position != null) {
			if (owner == null) {
				for (ExtractionPoint exPoint : mExtractionPoints) {
					float distance = SpringHelper.getDist(position.x, position.z, exPoint.mPosition.x, exPoint.mPosition.z);
					if (distance <= radius) {
						closeExtractionPoints.add(exPoint.mPosition);
					}
				}
			} else {
				for (ExtractionPoint exPoint : mExtractionPoints) {
					if (owner == exPoint.mOwner) {
						float distance = SpringHelper.getDist(position.x, position.z, exPoint.mPosition.x, exPoint.mPosition.z);
						if (distance <= radius) {
							closeExtractionPoints.add(exPoint.mPosition);
						}
					}
				}
			}
		} else {
			mAlIce.log(Level.SEVERE, "Position is null");
		}

		return closeExtractionPoints;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitCreated#unitCreated(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitCreated(Unit unit, Unit builder) {
		if (unit.getDef().getName().equals(Defs.MetalExtractor.unitName)) {
			ExtractionPoint exPoint = getClosestExtractionPoint(unit.getPos(), null);
			if (exPoint != null) {
				exPoint.mOwner = Owner.SELF;
			} else {
				mAlIce.log(Level.WARNING, "Extraction point is null!");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IUnitDestroyed#unitDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitDestroyed(Unit unit, Unit attacker) {
		if (unit.getDef().getName().equals(Defs.MetalExtractor.unitName)) {
			ExtractionPoint exPoint = getClosestExtractionPoint(unit.getPos(), null);
			if (exPoint != null) {
				exPoint.mOwner = Owner.NONE;
			} else {
				mAlIce.log(Level.WARNING, "Extraction point is null!");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEnterLOS#enemyEnterLOS(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyEnterLOS(Unit enemy) {
		if (enemy.getDef().getName().equals(Defs.MetalExtractor.unitName)) {
			ExtractionPoint exPoint = getClosestExtractionPoint(enemy.getPos(), null);
			if (exPoint != null) {
				exPoint.mOwner = Owner.ENEMY;
			} else {
				mAlIce.log(Level.WARNING, "Extraction point is null!");
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyDestroyed#enemyDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyDestroyed(Unit enemy, Unit attacker) {

		if (enemy.getDef().getName().equals(Defs.MetalExtractor.unitName)) {
			ExtractionPoint exPoint = getClosestExtractionPoint(enemy.getPos(), null);
			if (exPoint != null) {
				exPoint.mOwner = Owner.NONE;
			} else {
				mAlIce.log(Level.WARNING, "Extraction point is null!");
			}
		}
	}

	/**
	 * Get the closest extractionPoint
	 * 
	 * @param position
	 *            The position to check
	 * @param owner
	 *            The ownership to lock for, null if you don't care about the
	 *            ownership and just want the closest extration point
	 * @return The extractionPoint in the mExtractionPoints vector that is the
	 *         closest to the position
	 */
	private ExtractionPoint getClosestExtractionPoint(AIFloat3 position, Owner owner) {
		float min = Float.MAX_VALUE;
		float distance = Float.MAX_VALUE;
		ExtractionPoint result = null;

		if (position != null) {

			if (owner == null) {
				for (ExtractionPoint exPoint : mExtractionPoints) {
					distance = SpringHelper.getDist(position.x, position.z, exPoint.mPosition.x, exPoint.mPosition.z);
					if (min > distance) {
						min = distance;
						result = exPoint;
					}
				}
			} else {
				for (ExtractionPoint exPoint : mExtractionPoints) {
					if (owner == exPoint.mOwner) {
						distance = SpringHelper.getDist(position.x, position.z, exPoint.mPosition.x, exPoint.mPosition.z);
						if (min > distance) {
							min = distance;
							result = exPoint;
						}
					}
				}
			}
		} else {
			mAlIce.log(Level.SEVERE, "Position is null");
		}
		return result;
	}

	/**
	 * The AI-Interface
	 */
	private AlIce mAlIce;
	/**
	 * Vector of all extraction points and their currently known owner status
	 */
	private Vector<ExtractionPoint> mExtractionPoints;

	/**
	 * 
	 * Enumeration of ownership
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public enum Owner {
		/**
		 * We currently own the extraction point
		 */
		SELF,
		/**
		 * The enemy or someone else than our self own the extraction point
		 */
		ENEMY,
		/**
		 * No one owns the extraction point, it's free
		 */
		NONE
	}

	/**
	 * Extraction point class
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	private class ExtractionPoint {
		/**
		 * @param position
		 *            Position of the extraction point
		 * @param owner
		 *            The current owner of the extraction point
		 */
		public ExtractionPoint(AIFloat3 position, Owner owner) {
			mPosition = position;
			mOwner = owner;
		}

		/**
		 * The position of the extraction point
		 */
		private AIFloat3 mPosition;
		/**
		 * Status of who currently owns the extraction point
		 */
		private Owner mOwner;
	}
}
