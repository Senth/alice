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
import alice.Defs;
import alice.SpringHelper;
import alice.TaskUnit;

import com.springrts.ai.AIFloat3;

/**
 * 
 * A task that systematically scouts the entire map
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 */
public class TScoutRoaming extends TScout {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            The AI-Interface
	 * @param taskUnit
	 *            The scout unit
	 */
	public TScoutRoaming(AlIce alIce, TaskUnit taskUnit) {
		super(alIce, taskUnit);

		mMoveToDestination = null;
		mPositions = new LinkedList<AIFloat3>();

		GenerateRoamingPositions();
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
			mMoveToDestination = new TMoveCloseTo(mAlIce, mScout, mPositions.getFirst(), SpringHelper.getRealRadius(mScout
					.getUnit().getDef().getLosRadius()));
			mPositions.remove(mPositions.getFirst());
			return mMoveToDestination;
		}
	}

	/**
	 * Method used for filling the mPositions list
	 */
	private void GenerateRoamingPositions() {
		// Generate and save roaming positions in the mPositions list

		LinkedList<AIFloat3> tempTop = new LinkedList<AIFloat3>();
		LinkedList<AIFloat3> tempBot = new LinkedList<AIFloat3>();
		int nrOfWidthPos = (int) (SpringHelper.mapToUnitPos(mAlIce.getMap().getWidth()) / Defs.ROAMING_DISTANCE_WIDTH);
		int nrOfHeightPos = (int) (SpringHelper.mapToUnitPos(mAlIce.getMap().getHeight()) / Defs.ROAMING_DISTANCE_HEIGHT);
		AIFloat3 current = new AIFloat3(0, 0, 0);

		// Set the upper positions of the map
		int counter = 1;
		for (int i = 0; i < nrOfWidthPos; i++) {
			current = new AIFloat3((SpringHelper.mapToUnitPos(mAlIce.getMap().getWidth()) / nrOfWidthPos) * counter - 50, 0, 100);
			tempTop.add(current);
			counter++;
		}
		// Set the right positions of the map
		counter = 1;
		for (int i = 0; i < nrOfHeightPos; i++) {
			current = new AIFloat3((SpringHelper.mapToUnitPos(mAlIce.getMap().getWidth()) - 100), 0, ((SpringHelper
					.mapToUnitPos(mAlIce.getMap().getHeight() / nrOfHeightPos)) * counter));
			tempTop.add(current);
			counter++;
		}
		// Set the left positions of the map
		counter = 1;
		for (int i = 1; i < nrOfHeightPos; i++) {
			current = new AIFloat3(100, 0,
					((SpringHelper.mapToUnitPos(mAlIce.getMap().getHeight()) / nrOfHeightPos) * counter - 50));
			tempBot.add(current);
			counter++;
		}
		// Set the lower positions of the map
		counter = 1;
		for (int i = 0; i < nrOfWidthPos; i++) {
			current = new AIFloat3((SpringHelper.mapToUnitPos(mAlIce.getMap().getWidth()) / nrOfWidthPos) * counter - 50, 0,
					(SpringHelper.mapToUnitPos(mAlIce.getMap().getHeight()) - 100));
			tempBot.add(current);
			counter++;
		}
		// Add the roaming positions to mPositions
		int times = tempTop.size();
		if (times < tempBot.size()) {
			times = tempBot.size();
		}
		for (int i = 0; i < times; i++) {
			if (!tempTop.isEmpty()) {
				mPositions.add(tempTop.getFirst());
				tempTop.remove(tempTop.getFirst());
			}
			if (!tempBot.isEmpty()) {
				mPositions.add(tempBot.getFirst());
				tempBot.remove(tempBot.getFirst());
			}
		}

	}

	/**
	 * The MoveCloseTo task
	 */
	private TMoveCloseTo mMoveToDestination;
	/**
	 * The positions we have to choose from
	 */
	private LinkedList<AIFloat3> mPositions;

}
