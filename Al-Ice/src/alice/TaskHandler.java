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
import java.util.ListIterator;
import java.util.logging.Level;

import alice.General.GameTimeTypes;
import alice.interfaces.ITaskObserver;
import alice.tasks.Task;
import alice.tasks.Task.Status;

/**
 * A class that handles all active and halted tasks
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TaskHandler {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            the AI-Interface
	 */
	TaskHandler(AlIce alIce) {
		mAlIce = alIce;
		mActiveTasks = new LinkedList<Entry>();
		mHaltedTasks = new LinkedList<Entry>();
		mRecentlyHaltedTasks = new LinkedList<Entry>();

		mTotalTaskIteration = 0.0;
		mSecondsPerTask = 0.0;
	}

	/**
	 * Adds a task as active
	 * 
	 * @param task
	 *            The task to add
	 * @param taskObserver
	 *            The observer for the task, null if the task doesn't have any
	 *            observer
	 * @return True if the task could be added, false otherwise
	 */
	public boolean run(Task task, ITaskObserver taskObserver) {

		mAlIce.log(Level.FINE, "Added task: " + task);
		// Search if the task is already in the active or halted task lists,
		// return false if found
		for (Entry entry : mActiveTasks) {
			if (entry.mTask == task) {
				return (false);
			}
		}
		for (Entry entry : mHaltedTasks) {
			if (entry.mTask == task) {
				return (false);
			}
		}

		// The task does not already exists, add it to the active tasks
		Entry entry = new Entry(task, taskObserver);
		mActiveTasks.add(entry);

		return true;
	}

	/**
	 * Adds a task as active
	 * 
	 * @param task
	 *            The task to add
	 * @param taskObserver
	 *            The observer for the task, null if the task doesn't have any
	 *            observer
	 * @param taskUnit
	 *            The task unit bound to the task
	 * @param taskPriority
	 *            The priority of the task
	 * @return True if the task could be added, false otherwise
	 */
	public boolean run(Task task, ITaskObserver taskObserver, TaskUnit taskUnit, TaskUnit.TaskPriority taskPriority) {

		mAlIce.log(Level.FINE, "Added task: " + task + ", Priority: " + taskPriority);
		// Search if the task is already in the active or halted task lists,
		// return false if found
		for (Entry entry : mActiveTasks) {
			if (entry.mTask == task) {
				return (false);
			}
		}
		for (Entry entry : mHaltedTasks) {
			if (entry.mTask == task) {
				return (false);
			}
		}

		// The task does not already exists, add it to the active tasks
		LinkedList<ITaskObserver> taskObservers = new LinkedList<ITaskObserver>();
		if (taskObserver != null) {
			taskObservers.add(taskObserver);
		}
		taskObservers.add(taskUnit);
		Entry entry = new Entry(task, taskObservers);
		mActiveTasks.add(entry);

		boolean ok = taskUnit.setTask(task, taskPriority);
		if (!ok) {
			mAlIce.log(Level.SEVERE, "Failed to set task in taskUnit");
		}
		return true;
	}

	/**
	 * Adds a task as active
	 * 
	 * @param highLevelTask
	 *            The high level task to add
	 * @param taskObserver
	 *            The observer for the task, null if the task doesn't have any
	 *            observer
	 * @param taskUnits
	 *            The task units bound to the task
	 * @param taskPriority
	 *            The priority of the task
	 * @return True if the task could be added, false otherwise
	 */
	public boolean run(Task highLevelTask, ITaskObserver taskObserver, LinkedList<TaskUnit> taskUnits,
			TaskUnit.TaskPriority taskPriority) {

		mAlIce.log(Level.FINE, "Added high level task: " + highLevelTask);
		// Search if the task is already in the active or halted task lists,
		// return false if found
		for (Entry entry : mActiveTasks) {
			if (entry.mTask == highLevelTask) {
				return (false);
			}
		}
		for (Entry entry : mHaltedTasks) {
			if (entry.mTask == highLevelTask) {
				return (false);
			}
		}
		// The task does not already exists, add it to the active tasks and add
		// its observers
		LinkedList<ITaskObserver> taskObservers = new LinkedList<ITaskObserver>();
		if (taskObserver != null) {
			taskObservers.add(taskObserver);

		}
		for (TaskUnit taskUnit : taskUnits) {
			taskObservers.add(taskUnit);
			boolean ok = taskUnit.setHighLevelTask(highLevelTask);
			if (!ok) {
				mAlIce.log(Level.SEVERE, "Failed to set task in taskUnit: " + taskUnit.getUnitId());
				return false;
			}
		}
		Entry entry = new Entry(highLevelTask, taskObservers);
		mActiveTasks.add(entry);

		return true;
	}

	/**
	 * Removes an active or halted task
	 * 
	 * @param task
	 *            The task to be removed
	 * @return True if a task is found and removed
	 */
	public boolean remove(Task task) {

		// Search for the task in the active tasks list
		ListIterator<Entry> it = mActiveTasks.listIterator();
		while (it.hasNext()) {
			Entry currentEntry = it.next();
			if (currentEntry.mTask == task) {
				it.remove();

				// Call the observers
				for (ITaskObserver taskObserver : currentEntry.mTaskObservers) {
					taskObserver.onTaskFinished(currentEntry.mTask, Status.UNEXPECTED_ERROR);
				}
				return true;
			}
		}

		// Search for the task in the halted tasks list
		it = mHaltedTasks.listIterator();
		while (it.hasNext()) {
			Entry currentEntry = it.next();
			if (currentEntry.mTask == task) {
				it.remove();

				// Call the observers
				for (ITaskObserver taskObserver : currentEntry.mTaskObservers) {
					taskObserver.onTaskFinished(currentEntry.mTask, Status.UNEXPECTED_ERROR);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Halts an active task if it exists
	 * 
	 * @param task
	 *            The task to be halted
	 * @return True if the task was active and now halted
	 */
	public boolean halt(Task task) {

		// Search for the task in the active tasks list
		Entry foundEntry = null;
		ListIterator<Entry> it = mActiveTasks.listIterator();
		while (it.hasNext() && foundEntry == null) {
			Entry currentEntry = it.next();
			if (currentEntry.mTask == task) {
				foundEntry = currentEntry;
				it.remove();
				break;
			}
		}

		// Add the task to halted tasks and halt it
		if (foundEntry != null) {
			mHaltedTasks.add(foundEntry);

			// Also add it to the recently added
			mRecentlyHaltedTasks.add(foundEntry);
			foundEntry.mTask.halt();
			mAlIce.log(Level.FINER, "Halted task: " + foundEntry.mTask);
			mAlIce.log(Level.FINER, "Active tasks: " + mActiveTasks.size() + ", Halted tasks: " + mHaltedTasks.size());
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Resumes a halted task
	 * 
	 * @param task
	 *            The task to resume
	 * @return True if the task is found in the halted tasks list
	 */
	public boolean resume(Task task) {
		// Search for the task in the halted tasks list
		Entry foundEntry = null;
		ListIterator<Entry> it = mHaltedTasks.listIterator();
		while (it.hasNext() && foundEntry == null) {
			Entry currentEntry = it.next();
			if (currentEntry.mTask == task) {
				foundEntry = currentEntry;
				it.remove();
				break;
			}
		}

		// Add the task to the active tasks and resume it
		if (foundEntry != null) {
			mActiveTasks.add(foundEntry);
			foundEntry.mTask.resume();
			mAlIce.log(Level.FINER, "Resumed task: " + foundEntry.mTask);
			mAlIce.log(Level.FINER, "Active tasks: " + mActiveTasks.size() + ", Halted tasks: " + mHaltedTasks.size());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Unbinds a task for a task unit.
	 * 
	 * @param task
	 *            the task to unbind
	 * @param taskUnit
	 *            the task unit to ubind with the task
	 * @return True if the unbinding was removed successfully.
	 */
	public boolean unbindTask(Task task, TaskUnit taskUnit) {
		Entry foundEntry = null;
		boolean taskUnitFound = false;

		// Find the entry in active tasks
		ListIterator<Entry> it = mActiveTasks.listIterator();
		while (foundEntry == null && it.hasNext()) {
			Entry entry = it.next();
			if (entry.mTask == task) {
				foundEntry = entry;
			}
		}

		// Find the entry in halted tasks
		it = mHaltedTasks.listIterator();
		while (foundEntry == null && it.hasNext()) {
			Entry entry = it.next();
			if (entry.mTask == task) {
				foundEntry = entry;
			}
		}

		if (foundEntry != null) {
			ListIterator<ITaskObserver> observerIt = foundEntry.mTaskObservers.listIterator();
			while (!taskUnitFound && observerIt.hasNext()) {
				ITaskObserver observer = observerIt.next();

				if (observer == taskUnit) {
					taskUnitFound = true;
					observer.onTaskFinished(task, Status.UNEXPECTED_ERROR);
					observerIt.remove();
				}
			}
		}

		return taskUnitFound;
	}

	/**
	 * Binds a task for a task unit.
	 * 
	 * @param task
	 *            the task to bind
	 * @param taskUnit
	 *            the task unit to bind with the task
	 * @return True if the binding was successfull.
	 */
	public boolean bindTask(Task task, TaskUnit taskUnit) {
		Entry foundEntry = null;

		// Find the entry in active tasks
		ListIterator<Entry> it = mActiveTasks.listIterator();
		while (foundEntry == null && it.hasNext()) {
			Entry entry = it.next();
			if (entry.mTask == task) {
				foundEntry = entry;
			}
		}

		// Find the entry in halted tasks
		it = mHaltedTasks.listIterator();
		while (foundEntry == null && it.hasNext()) {
			Entry entry = it.next();
			if (entry.mTask == task) {
				foundEntry = entry;
			}
		}

		if (foundEntry != null) {
			foundEntry.mTaskObservers.add(taskUnit);
		}

		return foundEntry != null;
	}

	/**
	 * Executes the active tasks
	 */
	public void update() {

		// List with tasks to remove
		LinkedList<Entry> removedTasks = new LinkedList<Entry>();

		// Restart the task iterator again
		if (mTaskIterator == null || !mTaskIterator.hasNext()) {
			mTotalTaskIteration = 0.0;
			LinkedList<Entry> activeTasksClone = new LinkedList<Entry>(mActiveTasks);
			mTaskIterator = activeTasksClone.listIterator();
			mRecentlyHaltedTasks.clear();

			if (!activeTasksClone.isEmpty()) {
				mSecondsPerTask = Defs.TASK_SPLIT_LOAD_TIME / activeTasksClone.size();
			}
		}
		// Iterate through some tasks
		else {

			double deltaTime = mAlIce.getDeltaTime(GameTimeTypes.SECONDS);
			mTotalTaskIteration += deltaTime;

			while (mTaskIterator.hasNext() && mTotalTaskIteration >= mSecondsPerTask) {
				mTotalTaskIteration -= mSecondsPerTask;

				Entry currentEntry = mTaskIterator.next();

				// Iterate through the recently halted tasks and check if we can
				// find this task. If we can it means that we have halted the
				// task, thus we should skip processing it
				boolean processTask = true;

				for (Entry haltedEntry : mRecentlyHaltedTasks) {
					if (haltedEntry == currentEntry) {
						processTask = false;
						break;
					}
				}

				if (processTask) {
					// If the task has finished call the taskObserver and remove
					// it from the list
					if (currentEntry.mStatus == Task.Status.COMPLETED_SUCCESSFULLY ||
							currentEntry.mStatus == Task.Status.FAILED_CLEANLY ||
							currentEntry.mStatus == Task.Status.UNEXPECTED_ERROR) {
						// Remove the task
						removedTasks.add(currentEntry);

						mAlIce.log(Level.FINE, "Task: " + currentEntry.mTask + " finished with status: " + currentEntry.mStatus);

						for (ITaskObserver taskObserver : currentEntry.mTaskObservers) {
							taskObserver.onTaskFinished(currentEntry.mTask, currentEntry.mStatus);
						}

					} else {
						currentEntry.mStatus = currentEntry.mTask.execute();
					}
				}
			}

		}

		// Remove the entries from the original list. These are in entry so we
		// can iterate through the original list to avoid searching for multiple
		// tasks
		ListIterator<Entry> it = mActiveTasks.listIterator();
		while (!removedTasks.isEmpty() && it.hasNext()) {
			// Found the entry, remove it
			if (it.next() == removedTasks.getFirst()) {
				mAlIce.log(Level.FINE, "Removing a task: " + removedTasks.getFirst().mTask);
				it.remove();
				removedTasks.removeFirst();
				mAlIce.log(Level.FINE, "Active tasks: " + mActiveTasks.size());
			}
		}

		// If we still have tasks to remove print an error
		if (!removedTasks.isEmpty()) {
			mAlIce.log(Level.SEVERE, "ERROR! Still tasks to remove left.");
		}
	}

	/**
	 * Returns the number of tasks of the specified tasks that are active
	 * 
	 * @param taskType
	 *            the type (name) of the task
	 * @return the number of tasks with the specified name
	 */
	public int nrOfTasks(String taskType) {
		int tasks = 0;
		for (Entry entry : mActiveTasks) {
			if (entry.mTask.getClass().getName().equals(taskType)) {
				tasks++;
			}
		}

		return tasks;
	}

	/**
	 * A list of all the active tasks
	 */
	private LinkedList<Entry> mActiveTasks;
	/**
	 * A list of all the halted tasks
	 */
	private LinkedList<Entry> mHaltedTasks;
	/**
	 * A list with tasks that is recently halted
	 */
	private LinkedList<Entry> mRecentlyHaltedTasks;
	/**
	 * The task iterator
	 */
	private ListIterator<Entry> mTaskIterator;
	/**
	 * Total time of the iteration in seconds
	 */
	private double mTotalTaskIteration;
	/**
	 * The number of seconds that should elapse per task
	 */
	private double mSecondsPerTask;
	/**
	 * The AI-Interface
	 */
	private AlIce mAlIce;

	/**
	 * The task's entry in the task handler. Contains the task, latest task
	 * status and the observers.
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com> Wrapper for tasks
	 */
	private class Entry {
		/**
		 * Constructor for Entry
		 * 
		 * @param task
		 *            The task
		 * @param taskObservers
		 *            The observers,, null if the task doesn't have any observer
		 */
		public Entry(Task task, LinkedList<ITaskObserver> taskObservers) {
			mTask = task;
			mTaskObservers = taskObservers;
			mStatus = Task.Status.EXECUTED_SUCCESSFULLY;
		}

		/**
		 * Constructor for Entry
		 * 
		 * @param task
		 *            The task
		 * @param taskObserver
		 *            The observer, null if the task doesn't have any observer
		 */
		public Entry(Task task, ITaskObserver taskObserver) {
			mTask = task;

			mTaskObservers = new LinkedList<ITaskObserver>();
			if (taskObserver != null) {
				mTaskObservers.add(taskObserver);
			}
			mStatus = Task.Status.EXECUTED_SUCCESSFULLY;
		}

		/**
		 * The task
		 */
		public Task mTask;
		/**
		 * The status of the task
		 */
		public Task.Status mStatus;
		/**
		 * The observer of the task. null if the task doesn't have any observer
		 */
		public LinkedList<ITaskObserver> mTaskObservers;
	}

}
