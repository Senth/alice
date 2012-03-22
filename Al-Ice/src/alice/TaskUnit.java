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

import java.util.logging.Level;

import alice.Defs.UnitGroup;
import alice.interfaces.ITaskObserver;
import alice.tasks.Task;
import alice.tasks.Task.Status;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Unit;
import com.springrts.ai.oo.UnitDef;

/**
 * Wrapper for unit with tasks
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TaskUnit implements ITaskObserver {
	/**
	 * Initiate the TaskUnit
	 * 
	 * @param alIce
	 *            The AI interface
	 * @param unit
	 *            The unit reference
	 */
	public TaskUnit(AlIce alIce, Unit unit) {
		mUnit = unit;
		mTasks = new Task[TaskPriority.getSize()];
		mHighLevelTask = null;
		mAlIce = alIce;
		mUnitId = mUnit.getUnitId();

		// Set the unitgroup
		mUnitGroup = Defs.getUnitGroup(getDefName());
	}

	/**
	 * Set a priority task
	 * 
	 * @pre the task already exists in the TaskHandler
	 * @param task
	 *            The task to set
	 * @param taskPriority
	 *            The priority of the task
	 * @return True if the task at the priority is free
	 */
	public boolean setTask(Task task, TaskPriority taskPriority) {
		// Return false if we already have a task on that priority
		if (mTasks[taskPriority.ordinal()] != null) {
			mAlIce.log(Level.WARNING, "Unit: " + mUnit.getDef().getName() + " id: " + mUnitId + " allready have a task: " +
					mTasks[taskPriority.ordinal()].toString());
			return false;
		}

		mTasks[taskPriority.ordinal()] = task;
		// Check the tasks wit a lower priority. If one exists we halt that task
		TaskPriority[] priorities = TaskPriority.values();
		boolean foundPriority = false;
		boolean foundHigherPriority = false;
		for (TaskPriority currentPriority : priorities) {
			// If this is our priority we set that we have found it
			if (currentPriority == taskPriority) {
				foundPriority = true;

				// If we found a higher priority task we halt our task directly
			} else if (currentPriority.ordinal() < taskPriority.ordinal() && mTasks[currentPriority.ordinal()] != null) {
				foundHigherPriority = true;
				mAlIce.getTaskHandler().halt(task);

				// If we found a lower priority and we didn't have a higher
				// priority task we halt it
			} else if (foundPriority && !foundHigherPriority && mTasks[currentPriority.ordinal()] != null) {
				mAlIce.getTaskHandler().halt(mTasks[currentPriority.ordinal()]);
				break;
			}
		}
		return true;
	}

	/**
	 * Set a high level task
	 * 
	 * @pre the task already exists in the TaskHandler
	 * @param highLevelTask
	 *            The high level task to set
	 * @return True if the task unit does not allready have a high level task
	 */
	public boolean setHighLevelTask(Task highLevelTask) {
		// Return false if we already have a task on that priority
		if (mHighLevelTask != null) {
			mAlIce.log(Level.WARNING, "Unit: " + mUnit.getDef().getName() + " id: " + mUnitId + " allready have a task: " +
					mHighLevelTask);
			return false;
		}
		mHighLevelTask = highLevelTask;
		return true;
	}

	/**
	 * Returns the unit
	 * 
	 * @return The unit
	 */
	public Unit getUnit() {
		return mUnit;
	}

	/**
	 * Removes the unit from the task unit, i.e. sets the unit to null.
	 */
	public void destroyUnit() {
		mUnit = null;
	}

	/**
	 * Returns the current position of the unit
	 * 
	 * @return The position of the unit
	 */
	public AIFloat3 getUnitPos() {
		if (mUnit != null) {
			return mUnit.getPos();
		} else {
			return null;
		}
	}

	/**
	 * Returns the name of the unit definition
	 * 
	 * @return The name of the unit definition.
	 */
	public String getDefName() {
		if (mUnitGroup != null) {
			return mUnitGroup.unitName;
		} else if (mUnit != null) {
			return mUnit.getDef().getName();
		} else {
			return null;
		}
	}

	/**
	 * Returns the unit's Id
	 * 
	 * @return The unit's Id
	 */
	public int getUnitId() {
		return mUnitId;
	}

	/**
	 * Returns the definition of the unit
	 * 
	 * @return the definition of the unit
	 */
	public UnitDef getDef() {
		if (mUnitGroup != null) {
			return mUnitGroup.unitDef;
		} else if (mUnit != null) {
			return mUnit.getDef();
		} else {
			return null;
		}
	}

	/**
	 * Returns the task with specified priority
	 * 
	 * @param taskPriority
	 *            The priority of the task
	 * @return The priority task
	 */
	public Task getTask(TaskPriority taskPriority) {
		return mTasks[taskPriority.ordinal()];
	}

	/**
	 * Check if the unit is free, will return true even if it has an idle(LOW)
	 * task
	 * 
	 * @return True if the unit is free
	 */
	public boolean isFree() {
		if (mTasks[TaskPriority.HIGH.ordinal()] == null && mTasks[TaskPriority.MEDIUM.ordinal()] == null &&
				mHighLevelTask == null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the UnitGroup of the TaskUnit
	 * 
	 * @return the UnitGroup of the TaskUnit
	 */
	public UnitGroup getUnitGroup() {
		return mUnitGroup;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.TaskObserver#onTaskFinished(alice.tasks.Task,
	 * alice.tasks.Task.Status)
	 */
	@Override
	public void onTaskFinished(Task task, Status status) {
		// Check the tasks wit a lower priority. If one exists we resume that
		// task
		if (task == mHighLevelTask) {
			mHighLevelTask = null;
		}
		TaskPriority[] priorities = TaskPriority.values();
		boolean foundPriority = false;
		boolean foundHigherPriority = false;
		for (TaskPriority currentPriority : priorities) {
			// If this is our priority we set that we have found it
			if (mTasks[currentPriority.ordinal()] == task) {
				foundPriority = true;
				mTasks[currentPriority.ordinal()] = null;

			} else if (!foundPriority && mTasks[currentPriority.ordinal()] != null) {
				foundHigherPriority = true;

				// If we found a lower priority and we didn't have a higher
				// priority task we resume it
			} else if (foundPriority && !foundHigherPriority && mTasks[currentPriority.ordinal()] != null) {
				mAlIce.getTaskHandler().resume(mTasks[currentPriority.ordinal()]);
				break;
			}
		}
	}

	/**
	 * Returns a string with the task information about the unit
	 * 
	 * @return string with the task information
	 */
	public String getTaskInformation() {
		String info = "High-level task: " + mHighLevelTask + "\n";
		info += "HIGH: " + mTasks[TaskPriority.HIGH.ordinal()] + "\n";
		info += "MEDIUM: " + mTasks[TaskPriority.MEDIUM.ordinal()] + "\n";
		info += "LOW: " + mTasks[TaskPriority.LOW.ordinal()];
		return info;
	}

	/**
	 * Different priorities for tasks
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public enum TaskPriority {
		/**
		 * Emergency task, has highest priority
		 */
		HIGH,
		/**
		 * Normal task
		 */
		MEDIUM,
		/**
		 * Idle task, do this task when you have nothing else to do
		 */
		LOW;

		/**
		 * @return The number of different priorities
		 */
		public static int getSize() {
			return NR_OF_PRIORITIES;
		}

		/**
		 * -WARNING- must change this if you implement more kinds of priorities
		 * Current value is 3(HIGH, MEDIUM, LOW)
		 */
		private final static int NR_OF_PRIORITIES = 3;
	}

	/**
	 * Array with the units current tasks One for each priority
	 */
	private Task[] mTasks;
	/**
	 * A high-level task
	 */
	private Task mHighLevelTask;
	/**
	 * The unit
	 */
	private Unit mUnit;
	/**
	 * Reference to the AI interface
	 */
	private AlIce mAlIce;
	/**
	 * The UnitGroup of the task-unit
	 */
	private UnitGroup mUnitGroup;

	/**
	 * Saves the unitId. This is necessary if we want to get the id of the unit
	 * when after it has died.
	 */
	private int mUnitId;

}
