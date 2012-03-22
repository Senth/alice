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

package alice.interfaces;

import alice.tasks.Task;

/**
 * A interface for observing when a task is finished.
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public interface ITaskObserver {

	/**
	 * Gets called when a task is completed successfully or has failed.
	 * 
	 * @param task
	 *            The task that has finished
	 * @param status
	 *            The status of the finished task
	 */
	public void onTaskFinished(Task task, Task.Status status);
}
