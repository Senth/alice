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
import java.util.logging.Level;

import alice.AlIce;
import alice.Defs;
import alice.TaskUnit;
import alice.TaskUnit.TaskPriority;
import alice.interfaces.ITaskObserver;

import com.springrts.ai.AIFloat3;

/**
 * The initial build sequence used in the beginning of the game.
 * 
 * @note This task actually gets completed before the air-builder is completed.
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TInitialBuildSequence extends Task implements ITaskObserver {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            The AI-Interface
	 */
	public TInitialBuildSequence(AlIce alIce) {
		super(alIce);
		mAddedSequenceTask = false;
		mAirBuilderTaskAdded = false;
		mBuildSequenceComplete = false;
		mBuildSequence = new TSequence(mAlIce);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {
		if (!mAddedSequenceTask) {
			mAddedSequenceTask = true;
			setupBuildSequence();
		}

		if (!mAirBuilderTaskAdded) {
			// Try to find an aircraft plant that we can build the airborne
			// builder on
			LinkedList<TaskUnit> aircraftPlants = mAlIce.getTaskUnitHandler().getFreeUnitsByDef(Defs.AircraftPlant.unitName);
			if (!aircraftPlants.isEmpty()) {
				mAirBuilderTaskAdded = true;
				TaskUnit aircraftPlant = aircraftPlants.getFirst();
				Task airBuilderTask = new TBuildUnitByUnit(mAlIce, Defs.TheArchitectAir.unitName, aircraftPlant);
				mAlIce.getTaskHandler().run(airBuilderTask, null, aircraftPlant, TaskPriority.MEDIUM);
			}
		}

		if (mAirBuilderTaskAdded && mBuildSequenceComplete) {
			return Status.COMPLETED_SUCCESSFULLY;
		}

		return Status.EXECUTED_SUCCESSFULLY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.TaskObserver#onTaskFinished(alice.tasks.Task,
	 * alice.tasks.Task.Status)
	 */
	@Override
	public void onTaskFinished(Task task, Status status) {
		if (task == mBuildSequence) {
			mBuildSequenceComplete = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#halt()
	 */
	@Override
	public void halt() {
		mAlIce.log(Level.WARNING, "This task should never be halted!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		mAlIce.log(Level.WARNING, "This task should never be resumed!");
	}

	/**
	 * This should be run after the Erector task is complete to setup the build
	 * sequence
	 */
	private void setupBuildSequence() {
		LinkedList<TaskUnit> commanders = mAlIce.getTaskUnitHandler().getFreeUnitsByDef(Defs.TheOverseer.unitName);
		TaskUnit commander = null;
		if (commanders != null && !commanders.isEmpty()) {
			commander = commanders.getFirst();
		} else {
			mAlIce.log(Level.SEVERE, "No commanders found, can't set building tasks!");
			return;
		}

		LinkedList<AIFloat3> closeExtractionPoints = mAlIce.getExtractionPointMap().getCloseExtractionPoints(
				commander.getUnitPos(), null, Defs.CLOSE_INITAL_EXTRACTION_POINTS);
		// Add the close extraction points and then an air platform as the last
		for (AIFloat3 extractionPoint : closeExtractionPoints) {
			Task task = new TBuildUnitByUnitOnPos(mAlIce, Defs.MetalExtractor.unitName, commander, extractionPoint);
			mBuildSequence.addTaskToSequence(task);
		}
		Task airPlatformTask = new TBuildUnitByUnit(mAlIce, Defs.AircraftPlant.unitName, commander);
		mBuildSequence.addTaskToSequence(airPlatformTask);

		mAlIce.getTaskHandler().run(mBuildSequence, this, commander, TaskPriority.MEDIUM);
	}

	/**
	 * True if we sent the build erector command
	 */
	private boolean mAddedSequenceTask;
	/**
	 * True if all the buildings have been built
	 */
	private boolean mBuildSequenceComplete;
	/**
	 * True if we build an air engineer
	 */
	private boolean mAirBuilderTaskAdded;
	/**
	 * Task containing all the starting build buildings tasks
	 */
	private TSequence mBuildSequence;

}
