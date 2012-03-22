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
import alice.interfaces.ITaskObserver;

/**
 * 
 * A task that contains a sequence of tasks
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TSequence extends Task implements ITaskObserver {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            The AI-interface
	 */
	public TSequence(AlIce alIce) {
		super(alIce);
		mTaskSequence = new LinkedList<Task>();
		mStartExecute = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {
		if (mTaskSequence.isEmpty()) {
			return Status.COMPLETED_SUCCESSFULLY;
		} else if (mStartExecute) {
			mAlIce.getTaskHandler().run(mTaskSequence.getFirst(), this);
			mStartExecute = false;
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
		// For now, always continue with the next task
		if (mTaskSequence.getFirst() == task) {
			// A task is done, run next task
			mAlIce.log(Level.FINE, "Task finished, run next task in sequence");
			mTaskSequence.removeFirst();
			if (!mTaskSequence.isEmpty()) {
				mAlIce.getTaskHandler().run(mTaskSequence.getFirst(), this);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#halt()
	 */
	@Override
	public void halt() {
		// Halt the first task in the queue
		mAlIce.getTaskHandler().halt(mTaskSequence.getFirst());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		// Resume the first task in the queue
		mAlIce.getTaskHandler().resume(mTaskSequence.getFirst());
	}

	/**
	 * Adds a task to the list of tasks
	 * 
	 * @param task
	 *            The task to be added
	 */
	public void addTaskToSequence(Task task) {
		mTaskSequence.add(task);
	}

	/**
	 * The list of task that should be executed in a sequence
	 */
	private LinkedList<Task> mTaskSequence;
	/**
	 * True if it's the first time we are in execute()
	 */
	private boolean mStartExecute;

}
