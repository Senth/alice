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

import java.util.Random;

import alice.AlIce;
import alice.SpringHelper;
import alice.TaskUnit;

import com.springrts.ai.AIFloat3;

/**
 * 
 * A task that scouts a number of random positions on the map
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 */
public class TScoutRandom extends TScout {

	/**
	 * @param alIce
	 *            The AI-interface
	 * @param taskUnit
	 *            The scout unit
	 */
	public TScoutRandom(AlIce alIce, TaskUnit taskUnit) {
		super(alIce, taskUnit);
		mRand = new Random();
		mRandomPos = new AIFloat3(-1, 0, -1);
		mcRandomPos = 0;
		mMoveToDestination = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.TScout#nextScoutPosition()
	 */
	@Override
	protected TMoveCloseTo nextScoutPosition() {
		if (mcRandomPos < 20) {
			mRandomPos.x = mRand.nextFloat() * SpringHelper.mapToUnitPos(mAlIce.getMap().getWidth());
			mRandomPos.z = mRand.nextFloat() * SpringHelper.mapToUnitPos(mAlIce.getMap().getHeight());

			// Get the elevation (y) on the randomized position
			mRandomPos.y = mAlIce.getMap().getElevationAt(mRandomPos.x, mRandomPos.z);
			mMoveToDestination = new TMoveCloseTo(mAlIce, mScout, mRandomPos, SpringHelper.getRealRadius(mScout.getUnit()
					.getDef().getLosRadius()));
			mcRandomPos++;
			return mMoveToDestination;
		} else {
			return null;
		}
	}

	/**
	 * The random position we want to scout
	 */
	private AIFloat3 mRandomPos;

	/**
	 * The MoveCloseTo task
	 */
	private TMoveCloseTo mMoveToDestination;

	/**
	 * The scout unit
	 */
	private TaskUnit mScout;

	/**
	 * Random generator
	 */
	private Random mRand;

	/**
	 * Counter for how many times we have randomized
	 */
	private int mcRandomPos;
}
