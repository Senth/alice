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

import java.util.ArrayList;
import java.util.logging.Level;

import alice.AlIce;
import alice.Defs;
import alice.SpringHelper;
import alice.TaskUnit;
import alice.General.GameTimeTypes;

import com.springrts.ai.AICommand;
import com.springrts.ai.AIFloat3;
import com.springrts.ai.command.GetNextWaypointPathAICommand;
import com.springrts.ai.command.InitPathAICommand;
import com.springrts.ai.command.MoveUnitAICommand;
import com.springrts.ai.command.StopUnitAICommand;

/**
 * Moves close to the specified destination
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 */
public class TMoveCloseTo extends Task {

	/**
	 * 
	 * @param alIce
	 *            The AI-Interface
	 * @param taskUnit
	 *            The unit to move
	 * @param destination
	 *            The position to move to
	 * @param closeRadius
	 *            The radius we consider close enough to the destination
	 */
	public TMoveCloseTo(AlIce alIce, TaskUnit taskUnit, AIFloat3 destination, float closeRadius) {
		super(alIce);

		mUnit = taskUnit;
		mDestination = destination;
		mPosition = taskUnit.getUnitPos();
		mCloseRadius = closeRadius;
		mReachedDestination = false;
		mCommandSent = false;
		mWpPos = new AIFloat3(0, 0, 0);
		mLastStopTime = mAlIce.getGameTime(GameTimeTypes.SECONDS);
		mLastStopPosition = mPosition;
		mRetries = 0;
		if (!mUnit.getDef().isAbleToFly()) {
			mPathId = initNewPath(mUnit, mDestination);
		} else {
			mPathId = 0;
			mWpPos = mDestination;
		}
		mAlIce.log(Level.FINE, "PathId: " + mPathId);

	}

	/**
	 * Check if we reached the target destination, or is in radius
	 */
	public void checkProgress() {
		if (mUnit != null) {
			float dist = SpringHelper.getDist(mUnit.getUnitPos().x, mUnit.getUnitPos().z, mDestination.x, mDestination.z);
			if (dist < mCloseRadius || mPosition == mDestination) {
				mReachedDestination = true;
			}
			if (mUnit.getDef().isAbleToFly()) {
				// mAlIce.log(Level.FINE, "Type: " + mUnit.getDefName() +
				// " Distance: " + dist + "  CloseRadius: " + mCloseRadius);
			}
		}

	}

	/**
	 * Sends a move command
	 */
	private void moveToDestination() {
		try {
			// Create move command
			if (!mUnit.getDef().isAbleToFly()) {
				// mAlIce.log(Level.FINE, "GROUND UNIT - Issuing command");
				mWpPos = getNextWaypoint(mPathId);
				if (mWpPos != null) {
					mWpPos.y = 0;

					AICommand command = null;
					if (mWpPos.y >= 0) {
						command = new MoveUnitAICommand(mUnit.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT,
								mWpPos);
					}

					// If command is valid, execute it
					if (command != null) {
						int res = mAlIce.handleEngineCommand(command);
						// Check if something went wrong
						if (res != 0) {
							mAlIce.log(Level.SEVERE, "Error sending command to engine!");
						}
					} else {
						mAlIce.log(Level.SEVERE, "Could not create AICommand for ground unit");
						mAlIce.log(Level.SEVERE, this.toString() + " unitPos: " + mUnit.getUnitPos());
					}
				}
			}
			// Flying
			else {
				// mAlIce.log(Level.FINE, "FLYING UNIT - Issuing command");
				AICommand command = null;
				command = new MoveUnitAICommand(mUnit.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT,
						mDestination);
				// If command is valid, execute it
				if (command != null) {
					int res = mAlIce.handleEngineCommand(command);
					// Check if something went wrong
					if (res != 0) {
						mAlIce.log(Level.SEVERE, "Sending move command failed!");
					}
				} else {
					mAlIce.log(Level.SEVERE, "Could not create AICommand for air unit");
				}
			}
		} catch (Exception e) {
			mAlIce.log(Level.SEVERE, "Move command = null | Exception:  " + e.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {

		if (mUnit.getUnit() == null || mUnit.getUnit().getDef() == null || mPathId == -1) {
			// The unit got destroyed or something else happend
			return Status.UNEXPECTED_ERROR;
		}

		mPosition = new AIFloat3(mUnit.getUnitPos());

		checkProgress();

		if (mReachedDestination) {
			mAlIce.log(Level.FINEST, mUnit.getDefName() + " Id: " + mUnit.getUnitId() + " reached destination!");
			return Status.COMPLETED_SUCCESSFULLY;
		} else if (!mCommandSent) {
			mCommandSent = true;
			moveToDestination();
		}
		// We're close to the destination go to the next
		else if (SpringHelper.getDist(mWpPos.x, mWpPos.z, mUnit.getUnitPos().x, mUnit.getUnitPos().z) <= Defs.CLOSE_TO_WP) {
			moveToDestination();
		}
		// We have stopped and are probably stuck and have not reached target
		// destination , send command again. Only try for not flying...
		else if (!mUnit.getDef().isAbleToFly() && unitHasStopped()) {
			mRetries++;

			if (mRetries > Defs.MOVE_CLOSE_TO_MAX_RETRIES) {
				return Status.UNEXPECTED_ERROR;
			} else {
				mAlIce.log(Level.FINE, "Unit has stopped, trying to initialize a new path");
				int tempPathId = initNewPath(mUnit, mDestination);
				if (tempPathId != -1) {
					mPathId = tempPathId;
					moveToDestination();
				} else {
					mAlIce.log(Level.INFO, "Unit stopped, returning false");
					return Status.UNEXPECTED_ERROR;
				}
				// moveToDestination();
				// mAlIce.log(Level.FINE, "Stopped, rethinking");
			}
		}

		return Status.EXECUTED_SUCCESSFULLY;
	}

	/**
	 * Asks the engine about a new path for the specified unit to the specified
	 * destination. Returns pathId (>0) if ok, -1 if failed.
	 * 
	 * @param unit
	 *            The unit
	 * @param dest
	 *            The destination
	 * @return pathId
	 */
	private int initNewPath(TaskUnit unit, AIFloat3 dest) {
		if (unit.getUnitPos() != null && dest != null && unit.getDef() != null && unit.getDef().getMoveData() != null) {
			InitPathAICommand command = new InitPathAICommand(unit.getUnitPos(), dest, unit.getDef().getMoveData().getPathType(),
					1);
			int res = mAlIce.handleEngineCommand(command);
			if (res == 0) {
				// Command succeeded
				return command.ret_pathId;
			}
		}
		// Failed command
		return -1;
	}

	/**
	 * Returns the next waypoint in a path. The path needs to be initiated with
	 * the initNewPath method before any waypoints can be retrieved. The pathId
	 * must also be the id returned from the initNewPath method.
	 * 
	 * The y value in the returned AIFloat3 (waypoint.y) is a status number as
	 * follows: -1: Path invalid or endpoint reached. -2: Still thinking. Call
	 * method later. >=0: OK.
	 * 
	 * @param pathId
	 *            pathId from initNewPath method
	 * @return Destination way point and a status message in the y-position
	 */
	private AIFloat3 getNextWaypoint(int pathId) {
		AIFloat3 wp = new AIFloat3(0, 0, 0);
		GetNextWaypointPathAICommand command = new GetNextWaypointPathAICommand(pathId, wp);
		int res = mAlIce.handleEngineCommand(command);
		if (res == 0) {
			// Command succeeded
			return wp;
		}
		// Failed command
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#halt()
	 */
	@Override
	public void halt() {
		// Remove the command from the unit if we have issued the command
		AICommand command = new StopUnitAICommand(mUnit.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT);

		// If command is valid, execute it
		if (command != null) {
			int res = mAlIce.handleEngineCommand(command);

			// Check if there were some errors report it
			if (res != 0) {
				mAlIce.log(Level.WARNING, "ERROR! Couldn't issuing stop command!");
			}
		} else {
			mAlIce.log(Level.WARNING, "ERROR! Couldn't create stop command!");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		// TODO figure out why we get null pointer exception when we try to init
		// a new path. And implement it
		if (mUnit.getUnit() != null) {
			// Only initialize new path if the unit can't fly
			if (mUnit.getDef().isAbleToFly()) {
				int pathId = initNewPath(mUnit, mDestination);
				// only set a new path if the returned path is valid
				if (pathId != -1) {
					mPathId = pathId;
				}
			}
			moveToDestination();
		}
	}

	/**
	 * Returns the bound task unit
	 * 
	 * @return the bound task unit that has the move to task
	 */
	public TaskUnit getBoundTaskUnit() {
		return mUnit;
	}

	@Override
	public String toString() {
		return "TMoveCloseTo(" + mUnit.getDefName() + ", " + mDestination + ", " + mCloseRadius + ")";
	}

	/**
	 * Checks if the unit has stopped, returns true if it has. Only checks each
	 * MOVE_CLOSE_TO_STOP_CHECK_TIME
	 * 
	 * @return true if the unit has stopped
	 */
	private boolean unitHasStopped() {
		if (mLastStopTime + Defs.MOVE_CLOSE_TO_STOP_CHECK_TIME < mAlIce.getGameTime(GameTimeTypes.SECONDS)) {
			if (Math.abs(mPosition.x - mLastStopPosition.x) <= 0.001 && Math.abs(mPosition.z - mLastStopPosition.z) <= 0.001) {
				return true;
			} else {
				mLastStopPosition = mPosition;
				mLastStopTime = mAlIce.getGameTime(GameTimeTypes.SECONDS);
			}
		}

		return false;
	}

	/**
	 * The destination of the unit
	 */
	private AIFloat3 mDestination;
	/**
	 * The position of the unit
	 */
	private AIFloat3 mPosition;
	/**
	 * The time we last tested if we have stopped
	 */
	private double mLastStopTime;
	/**
	 * The position when we tested the stop last time
	 */
	private AIFloat3 mLastStopPosition;
	/**
	 * Number of retries to create a new path
	 */
	private int mRetries;
	/**
	 * Current way point position
	 */
	private AIFloat3 mWpPos;
	/**
	 * The radius we consider close enough to the destination
	 */
	private float mCloseRadius;
	/**
	 * The unit to move
	 */
	private TaskUnit mUnit;
	/**
	 * True if we have reached a position within the radius of the destination
	 */
	private boolean mReachedDestination;
	/**
	 * Path id for Waypoint system
	 */
	private int mPathId;
	/**
	 * If the command was sent or not
	 */
	private boolean mCommandSent;

}
