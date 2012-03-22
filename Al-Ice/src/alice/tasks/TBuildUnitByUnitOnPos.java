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

import java.util.logging.Level;

import alice.AlIce;
import alice.SpringHelper;
import alice.TaskUnit;
import alice.interfaces.ITaskObserver;

import com.springrts.ai.AIFloat3;

/**
 * Tries to construct a building close to the specified position.
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TBuildUnitByUnitOnPos extends TBuildUnitByUnit implements ITaskObserver {

	/**
	 * Constructor that takes the unit we want to build and the builder. Also
	 * the position we want to build close to
	 * 
	 * @param alIce
	 *            the AI-Interface
	 * @param buildDef
	 *            the building to build
	 * @param builder
	 *            the builder
	 * @param position
	 *            the position we want to build on
	 */
	public TBuildUnitByUnitOnPos(AlIce alIce, String buildDef, TaskUnit builder, AIFloat3 position) {
		super(alIce, buildDef, builder);

		// Only apply a small minimum distance when we try to build
		mMinSpace = 2;
		mSearchPosition = position;
		mBuildDistanceOk = false;
		mMoveToTask = null;
		mMinBuildDistance = calculateMinBuildDistance();
		mClosestBuildPos = mAlIce.getMap().findClosestBuildSite(mBuildDef, mSearchPosition, SEARCH_RADIUS, mMinSpace, FACING);

		// Autoset buildDistanceOk to true when our unit is flying
		if (mBuilder.getUnitGroup().unitDef.isAbleToFly()) {
			mBuildDistanceOk = true;
		}
	}

	@Override
	public Status execute() {
		if (mBuilder.getUnit() == null) {
			return Status.UNEXPECTED_ERROR;
		}

		Status status = Status.EXECUTED_SUCCESSFULLY;

		if (!mBuildDistanceOk && mMoveToTask == null) {
			float distance = SpringHelper.getDist(mClosestBuildPos, mBuilder.getUnitPos());
			mAlIce.log(Level.FINEST, "Distance: " + distance + ", Min distance: " + mMinBuildDistance);
			if (distance < mMinBuildDistance) {
				AIFloat3 direction = SpringHelper.getDiff(mBuilder.getUnitPos(), mClosestBuildPos);
				SpringHelper.normalizeVec(direction, false);

				// Calculate the position we should move to
				AIFloat3 movePos = SpringHelper.getSum(mBuilder.getUnitPos(), SpringHelper.getProduct(direction,
						MIN_MOVE_DISTANCE * mMinBuildDistance));

				// Get the elevation (y) from the map
				movePos.y = mAlIce.getMap().getElevationAt(movePos.x, movePos.z);
				mAlIce.log(Level.FINEST, "Unit position: " + mBuilder.getUnitPos() + ", Build position: " + mClosestBuildPos);

				mMoveToTask = new TMoveCloseTo(mAlIce, mBuilder, movePos, CLOSE_RADIUS * mMinBuildDistance);
				mAlIce.getTaskHandler().run(mMoveToTask, this);
			} else {
				mBuildDistanceOk = true;
			}
		}

		if (mBuildDistanceOk) {
			status = super.execute();
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuildUnitByUnitOnPos(" + mBuildDefName + ", " + mSearchPosition + ", " + mBuilder.getDefName() + ")";
	}

	@Override
	public void onTaskFinished(Task task, Status status) {
		// If our move to task finished, set it to null.
		if (task == mMoveToTask) {
			mAlIce.log(Level.FINE, "MoveCloseTo finished!");
			mMoveToTask = null;
		} else {
			super.onTaskFinished(task, status);
		}
	}

	@Override
	protected AIFloat3 calculateValidBuildPosition() {
		return mClosestBuildPos;
	}

	/**
	 * The distance from the current position to move. This is multiplied by
	 * mMinBuildDistance.
	 */
	private float MIN_MOVE_DISTANCE = 2.0f;
	/**
	 * The close radius of the move to task. This is also multiplied with
	 * mMinBuildDistance. //
	 */
	private float CLOSE_RADIUS = 1.0f;
	/**
	 * the position we want to search for a close building position
	 */
	private AIFloat3 mSearchPosition;
	/**
	 * The closest building pos we can build on
	 */
	private AIFloat3 mClosestBuildPos;
	/**
	 * If we are in a ok distance to start building
	 */
	private boolean mBuildDistanceOk;
	/**
	 * The TMoveCloseTo task if we need one to move away from our current
	 * position
	 */
	private Task mMoveToTask;
	/**
	 * The minimum distance we need to be from the building site.
	 */
	private float mMinBuildDistance;
}
