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

import alice.AlIce;

/**
 * An abstract class that all tasks inherit from
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public abstract class Task {

	/**
	 * Sets the AI interface
	 * 
	 * @param alIce
	 *            The AI interface
	 */
	public Task(AlIce alIce) {
		mAlIce = alIce;
	}

	/**
	 * Executes the task
	 * 
	 * @return status of task after executing
	 */
	public abstract Status execute();

	/**
	 * Halts the task
	 */
	public abstract void halt();

	/**
	 * Resumes the task
	 */
	public abstract void resume();

	/**
	 * Status result after executing the task.
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public enum Status {
		/**
		 * The task executed successfully
		 */
		EXECUTED_SUCCESSFULLY,
		/**
		 * The task completed successfully and the task is now finished
		 */
		COMPLETED_SUCCESSFULLY,
		/**
		 * The task failed without affecting the environment
		 */
		FAILED_CLEANLY,
		/**
		 * The task failed while running
		 */
		UNEXPECTED_ERROR
	}

	/**
	 * The AI interface
	 */
	protected AlIce mAlIce;
}
