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
import alice.TaskUnit;
import alice.interfaces.ITaskObserver;

import com.springrts.ai.AIFloat3;

/**
 * 
 * Abstract scouting class, subclasses implements the nextScoutPosition method
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public abstract class TScout extends Task implements ITaskObserver {

	/**
	 * @param alIce
	 *            The AI-interface
	 * @param taskUnit
	 *            The scout unit
	 */
	public TScout(AlIce alIce, TaskUnit taskUnit) {
		super(alIce);
		mMoveToDestination = null; // Set it first when we get a destination
		// from
		// subclass
		mScoutingComplete = false;
		mFirstDestination = false;
		mUnexpectedError = false;
		mReturningHome = false;
		mReachedScoutPosition = false;
		mScout = taskUnit;
		mHomeBase = mAlIce.getMap().getStartPos();

		mReturnToBase = new TMoveCloseTo(mAlIce, mScout, mHomeBase, 300);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {
		if (mUnexpectedError || mScout.getUnit() == null) {
			return Status.UNEXPECTED_ERROR;
		}
		if (mReachedScoutPosition) {
			mReachedScoutPosition = false;
			mMoveToDestination = nextScoutPosition();
			if (mMoveToDestination != null) {
				mAlIce.getTaskHandler().run(mMoveToDestination, this);
			} else {
				// Scouting complete, return to base
				if (!mReturningHome) {
					mReturningHome = true;
					mAlIce.getTaskHandler().run(mReturnToBase, this);
				}
			}
		}
		// First time, get a destination
		if (!mFirstDestination) {
			mMoveToDestination = nextScoutPosition();

			mFirstDestination = true;
			if (mMoveToDestination == null) {
				// First destination was null, something went wrong or the task
				// is ready at the beginning
				mAlIce.log(Level.WARNING, "First scout destination was null");
				return Status.FAILED_CLEANLY;
			} else {
				// Run the first MoveClostTo task
				mAlIce.getTaskHandler().run(mMoveToDestination, this);
			}

		}

		if (mScoutingComplete) {
			return Status.COMPLETED_SUCCESSFULLY;
		}
		return Status.EXECUTED_SUCCESSFULLY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#halt()
	 */
	@Override
	public void halt() {
		mAlIce.getTaskHandler().halt(mMoveToDestination);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		mAlIce.getTaskHandler().resume(mMoveToDestination);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.TaskObserver#onTaskFinished(alice.tasks.Task,
	 * alice.tasks.Task.Status)
	 */
	@Override
	public void onTaskFinished(Task task, Status status) {
		if (task == mMoveToDestination && status == Status.COMPLETED_SUCCESSFULLY) {
			// We have reached our destination, get new target
			mReachedScoutPosition = true;
		} else if (task == mMoveToDestination) {
			mUnexpectedError = true;
		} else if (task == mReturnToBase && status == Status.COMPLETED_SUCCESSFULLY) {
			mScoutingComplete = true;
		} else if (task == mReturnToBase) {
			// Returning to base failer or something
			mUnexpectedError = true;
		}
	}

	/**
	 * Get the next position to scout
	 * 
	 * @return Position to scout
	 */
	protected abstract TMoveCloseTo nextScoutPosition();

	/**
	 * Return to base task
	 */
	private TMoveCloseTo mReturnToBase;
	/**
	 * True if the scout is returning home to base
	 */
	private boolean mReturningHome;
	/**
	 * The scout unit
	 */
	protected TaskUnit mScout;
	/**
	 * True if the scout task is complete and we returned to home base
	 */
	private boolean mScoutingComplete;
	/**
	 * The home base position
	 */
	private AIFloat3 mHomeBase;
	/**
	 * The current destination
	 */
	private TMoveCloseTo mMoveToDestination;
	/**
	 * True if we reached a scout position
	 */
	private boolean mReachedScoutPosition;
	/**
	 * True if we got the first destination from subclass
	 */
	private boolean mFirstDestination;

	/**
	 * True if mMoveToDestination returned UNEXPECTED_ERROR
	 */
	private boolean mUnexpectedError;

}
