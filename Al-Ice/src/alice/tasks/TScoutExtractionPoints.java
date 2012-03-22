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

package alice.tasks;

import java.util.LinkedList;

import alice.AlIce;
import alice.SpringHelper;
import alice.TaskUnit;
import alice.ExtractionPointMap.Owner;

import com.springrts.ai.AIFloat3;

/**
 * 
 * Scouts all the extraction points that are free
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 */
public class TScoutExtractionPoints extends TScout {

	/**
	 * Constructor, sets
	 * 
	 * @param alIce
	 *            The AI-Interface
	 * @param taskUnit
	 *            The scout unit
	 */
	public TScoutExtractionPoints(AlIce alIce, TaskUnit taskUnit) {
		super(alIce, taskUnit);
		mPositions = mAlIce.getExtractionPointMap().getExtractionPointPositionsByOwner(Owner.NONE);
		mPositions.addAll(mAlIce.getExtractionPointMap().getExtractionPointPositionsByOwner(Owner.ENEMY));
		mMoveToDestination = null;
		mScout = taskUnit;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.TScout#nextScoutPosition()
	 */
	@Override
	protected TMoveCloseTo nextScoutPosition() {
		if (mPositions.isEmpty()) {
			return null;
		} else {

			float min = Float.MAX_VALUE;
			float distance = Float.MAX_VALUE;
			AIFloat3 result = new AIFloat3(0, 0, 0);

			for (AIFloat3 exPoint : mPositions) {
				distance = SpringHelper.getDist(mScout.getUnitPos().x, mScout.getUnitPos().z, exPoint.x, exPoint.z);
				if (min > distance) {
					min = distance;
					result = exPoint;
				}
			}
			mPositions.remove(result);
			mMoveToDestination = new TMoveCloseTo(mAlIce, mScout, result, SpringHelper.getRealRadius(mScout.getUnit().getDef()
					.getLosRadius()));

			return mMoveToDestination;
		}
	}

	/**
	 * The MoveCloseTo task
	 */
	private TMoveCloseTo mMoveToDestination;
	/**
	 * A list of the position to scout
	 */
	private LinkedList<AIFloat3> mPositions;

}
