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
import java.util.ListIterator;
import java.util.logging.Level;

import alice.AlIce;
import alice.Defs;
import alice.TaskHandler;
import alice.TaskUnit;
import alice.TaskUnit.TaskPriority;
import alice.interfaces.ITaskObserver;
import alice.tasks.TAttackTarget.FailTypes;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Unit;

/**
 * An abstract task that attacks with a group of units.
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public abstract class TBaseAttack extends Task implements ITaskObserver {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            The AI-interface
	 * @param attackForce
	 *            The attacking force
	 * @param healers
	 *            The healer force
	 */
	public TBaseAttack(AlIce alIce, LinkedList<TaskUnit> attackForce, LinkedList<TaskUnit> healers) {
		super(alIce);

		// TODO_LOW use one list for both healer and attack force instead

		mAttackForce = new LinkedList<TaskUnit>();
		mAttackForce.addAll(attackForce);
		if (healers != null) {
			mHealForce = healers;
		} else {
			mHealForce = new LinkedList<TaskUnit>();
		}
		mTargetLocation = null;
		mAttackTasks = new LinkedList<TAttackTarget>();
		mMoveToTasks = new LinkedList<TMoveCloseTo>();
		mHasIssuedMovedTo = false;
		mHasIssuedRegroup = false;
		mGroupPosition = new AIFloat3(0, 0, 0);

		// Calculate group position
		updateGroupPosition();

		mRegroupTasks = new LinkedList<TMoveCloseTo>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {
		// Remove all null units
		ListIterator<TaskUnit> it = mAttackForce.listIterator();
		while (it.hasNext()) {
			if (it.next().getUnit() == null) {
				it.remove();
			}
		}
		it = mHealForce.listIterator();
		while (it.hasNext()) {
			if (it.next().getUnit() == null) {
				it.remove();
			}
		}

		// Fail the task if the attack force is empty!
		if (mAttackForce.isEmpty()) {
			mAlIce.log(Level.FINE, "Whole attack force destroyed");
			return Status.UNEXPECTED_ERROR;
		}

		// Update group position
		updateGroupPosition();

		// If we don't have any targets check for close enemies
		if (mAttackTasks.isEmpty()) {
			Unit closestEnemy = getCloseEnemy();

			if (closestEnemy != null) {
				// mAlIce.log(Level.FINE, "Close enemy found! " +
				// closestEnemy.getDef().getName() + " id: " +
				// closestEnemy.getUnitId() + " position: " +
				// closestEnemy.getPos() + " | Our position: " + mGroupPosition
				// +
				// " herp: " + this);
				// Create attack tasks for attack force
				for (TaskUnit taskUnit : mAttackForce) {
					TAttackTarget tempAttack = new TAttackTarget(mAlIce, taskUnit, closestEnemy.getUnitId());
					mAttackTasks.add(tempAttack);
					mAlIce.getTaskHandler().run(tempAttack, this, taskUnit, TaskPriority.HIGH);
				}
			}
		}
		// Issue regroup command
		if (!mHasIssuedRegroup && mAttackTasks.isEmpty()) {
			mHasIssuedRegroup = true;
			// Create move to tasks for attack/healer force
			float radius = Defs.CLOSE_TO_WP + REGROUP_RADIUS_PER_UNIT * (mAttackForce.size() + mHealForce.size() + 1);
			mAlIce.log(Level.FINE, "Creating regroup commamd: " + mGroupPosition + " size of group: " + mAttackForce.size());
			for (TaskUnit taskUnit : mAttackForce) {
				TMoveCloseTo tempMove = new TMoveCloseTo(mAlIce, taskUnit, mGroupPosition, radius);
				mRegroupTasks.add(tempMove);
				mAlIce.getTaskHandler().run(tempMove, this, taskUnit, TaskPriority.MEDIUM);
			}
			for (TaskUnit taskUnit : mHealForce) {
				TMoveCloseTo tempMove = new TMoveCloseTo(mAlIce, taskUnit, mGroupPosition, radius);
				mRegroupTasks.add(tempMove);
				mAlIce.getTaskHandler().run(tempMove, this, taskUnit, TaskPriority.MEDIUM);
			}
		}

		// Issue move to target location command
		if (!mHasIssuedMovedTo && mAttackTasks.isEmpty() && mRegroupTasks.isEmpty()) {
			mTargetLocation = getTargetLocation();

			if (mTargetLocation != null) {
				mHasIssuedMovedTo = true;

				for (TaskUnit taskUnit : mAttackForce) {
					TMoveCloseTo tempMove = new TMoveCloseTo(mAlIce, taskUnit, mTargetLocation, Defs.CLOSE_TO_WP);
					mMoveToTasks.add(tempMove);
					mAlIce.getTaskHandler().run(mMoveToTasks.getLast(), this, taskUnit, TaskPriority.MEDIUM);
				}
				for (TaskUnit taskUnit : mHealForce) {
					TMoveCloseTo tempMove = new TMoveCloseTo(mAlIce, taskUnit, mTargetLocation, Defs.CLOSE_TO_WP);
					mMoveToTasks.add(tempMove);
					mAlIce.getTaskHandler().run(tempMove, this, taskUnit, TaskPriority.MEDIUM);
				}
			} else {
				mAlIce.log(Level.WARNING, "No target location was return...");
			}

		}

		// Restart the attack command if the there are no target at our
		// target location, or only select a new target location
		if (mHasIssuedMovedTo && mAttackTasks.isEmpty() && mMoveToTasks.isEmpty()) {
			mHasIssuedMovedTo = false;
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
		// TODO_LOW Implement halt()

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		// TODO_LOW Implement resume()

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.ITaskObserver#onTaskFinished(alice.tasks.Task,
	 * alice.tasks.Task.Status)
	 */
	@Override
	public void onTaskFinished(Task task, Status status) {
		if (task instanceof TMoveCloseTo) {
			// Remove move to tasks
			ListIterator<TMoveCloseTo> it = mMoveToTasks.listIterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				if (it.next() == task) {
					it.remove();
					found = true;
					mAlIce.log(Level.FINE, "MoveToTask has finished with status: " + status + ", left:" + mMoveToTasks.size());
				}
			}
			// Remove regroup tasks
			it = mRegroupTasks.listIterator();
			while (it.hasNext() && !found) {
				if (it.next() == task) {
					it.remove();
					found = true;
					mAlIce.log(Level.FINE, "RegroupTask has finished with status: " + status + ", left: " + mRegroupTasks.size());
				}
			}

			// If we found the task and it was returned with unexpected error
			// remove the unit from this attack base
			if (found && status == Status.UNEXPECTED_ERROR) {
				TMoveCloseTo moveCloseToTask = (TMoveCloseTo) task;
				TaskUnit taskUnit = moveCloseToTask.getBoundTaskUnit();
				mAlIce.getTaskHandler().unbindTask(task, taskUnit);

				// Remove the task unit from the list
				boolean removedUnit = false;
				ListIterator<TaskUnit> taskUnitIt = mAttackForce.listIterator();
				while (!removedUnit && taskUnitIt.hasNext()) {
					TaskUnit currentUnit = taskUnitIt.next();
					if (currentUnit == taskUnit) {
						removedUnit = true;
						taskUnitIt.remove();
					}
				}

				taskUnitIt = mHealForce.listIterator();
				while (!removedUnit && taskUnitIt.hasNext()) {
					TaskUnit currentUnit = taskUnitIt.next();
					if (currentUnit == taskUnit) {
						removedUnit = true;
						taskUnitIt.remove();
					}
				}
			}
		} else if (task instanceof TAttackTarget) {
			// If a task was completed, remove all the tasks.
			TAttackTarget attackTargetTaks = (TAttackTarget) task;

			if (status == Status.COMPLETED_SUCCESSFULLY || attackTargetTaks.getFailType() == FailTypes.TARGET_LEFT_LOS) {
				LinkedList<TAttackTarget> removedTasks = new LinkedList<TAttackTarget>(mAttackTasks);
				mAttackTasks.clear();

				for (TAttackTarget removedTask : removedTasks) {
					mAlIce.getTaskHandler().remove(removedTask);
				}
			}
			// Else just remove the task that failed
			else {
				ListIterator<TAttackTarget> it = mAttackTasks.listIterator();
				while (it.hasNext()) {
					if (it.next() == task) {
						it.remove();
						mAlIce.log(Level.FINE, "AttackTask has finished with status: " + status + ", left: " +
								mAttackTasks.size());
						break;
					}
				}
			}
		}
	}

	/**
	 * Clears the task and frees all it's units and returns the removed units
	 * that now are free
	 * 
	 * @return all the removed units that now are free
	 */
	public LinkedList<TaskUnit> clear() {
		TaskHandler taskHandler = mAlIce.getTaskHandler();

		for (Task task : mAttackTasks) {
			taskHandler.remove(task);
		}

		for (Task task : mRegroupTasks) {
			taskHandler.remove(task);
		}

		for (Task task : mMoveToTasks) {
			taskHandler.remove(task);
		}

		LinkedList<TaskUnit> removedUnits = new LinkedList<TaskUnit>();

		ListIterator<TaskUnit> taskUnits = mAttackForce.listIterator();
		while (taskUnits.hasNext()) {
			TaskUnit taskUnit = taskUnits.next();
			taskHandler.unbindTask(this, taskUnit);
			removedUnits.add(taskUnit);
			taskUnits.remove();
		}

		taskUnits = mHealForce.listIterator();
		while (taskUnits.hasNext()) {
			TaskUnit taskUnit = taskUnits.next();
			taskHandler.unbindTask(this, taskUnit);
			// removedUnits.add(taskUnit);
			taskUnits.remove();
		}

		return removedUnits;
	}

	/**
	 * Add more units to the attack task
	 * 
	 * @param taskUnits
	 *            new units to add to the attack force
	 */
	public void addUnits(LinkedList<TaskUnit> taskUnits) {
		mAttackForce.addAll(taskUnits);

		// Create goals for the new task units depending on the active
		// goals atm, but not any attack task
		if (!mRegroupTasks.isEmpty()) {
			float radius = Defs.CLOSE_TO_WP + REGROUP_RADIUS_PER_UNIT * (mAttackForce.size() + mHealForce.size() + 1);
			for (TaskUnit taskUnit : taskUnits) {
				TMoveCloseTo task = new TMoveCloseTo(mAlIce, taskUnit, mGroupPosition, radius);
				mRegroupTasks.add(task);
				mAlIce.getTaskHandler().run(task, this, taskUnit, TaskPriority.MEDIUM);
			}
		} else if (!mMoveToTasks.isEmpty()) {
			for (TaskUnit taskUnit : taskUnits) {
				TMoveCloseTo task = new TMoveCloseTo(mAlIce, taskUnit, mTargetLocation, Defs.CLOSE_TO_WP);
				mMoveToTasks.add(task);
				mAlIce.getTaskHandler().run(task, this, taskUnit, TaskPriority.MEDIUM);
			}
		}
	}

	/**
	 * Returns the number of units in the task
	 * 
	 * @return the number of units in the task
	 */
	public int getNrOfUnits() {
		return mAttackForce.size() + mHealForce.size();
	}

	/**
	 * Returns the group position
	 * 
	 * @return the group's center position
	 */
	public AIFloat3 getGroupPosition() {
		return mGroupPosition;
	}

	/**
	 * Returns a close enemy unit to the group.
	 * 
	 * @return a close enemy unit to the group.
	 */
	protected abstract Unit getCloseEnemy();

	/**
	 * Returns a position of the target we want to attack
	 * 
	 * @return a position of the target we want to attack
	 */
	protected abstract AIFloat3 getTargetLocation();

	/**
	 * Updates the group position
	 */
	private void updateGroupPosition() {

		double tempX = 0.0;
		double tempZ = 0.0;
		// Since java did not want to divide we had to do this...
		double failFix = 1.0 / mAttackForce.size();
		for (TaskUnit taskUnit : mAttackForce) {
			tempX += taskUnit.getUnitPos().x;
			tempZ += taskUnit.getUnitPos().z;
		}
		tempX *= failFix;
		tempZ *= failFix;
		mGroupPosition.x = (float) tempX;
		mGroupPosition.z = (float) tempZ;

		mGroupPosition.y = mAlIce.getMap().getElevationAt(mGroupPosition.x, mGroupPosition.z);
	}

	/**
	 * Gather all the attack/healer force at one place
	 */
	private LinkedList<TMoveCloseTo> mRegroupTasks;
	/**
	 * The group position
	 */
	private AIFloat3 mGroupPosition;
	/**
	 * True if we have sent move close to command
	 */
	private boolean mHasIssuedMovedTo;
	/**
	 * True if we sent regroup command
	 */
	private boolean mHasIssuedRegroup;
	/**
	 * The move close to tasks, one for each unit in mAttackForce and mHealForce
	 */
	private LinkedList<TMoveCloseTo> mMoveToTasks;
	/**
	 * The attacking tasks, one for each unit in mAttackForce
	 */
	private LinkedList<TAttackTarget> mAttackTasks;
	/**
	 * The target location, the General decides this
	 */
	private AIFloat3 mTargetLocation;
	/**
	 * The attack force
	 */
	private LinkedList<TaskUnit> mAttackForce;
	/**
	 * The healer force
	 */
	private LinkedList<TaskUnit> mHealForce;
	/**
	 * The radius should be bigger the more units we have
	 */
	private static final float REGROUP_RADIUS_PER_UNIT = 100;
}
