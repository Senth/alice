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

import com.springrts.ai.oo.Unit;

/**
 * Listens to events when a command is finished
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public interface ICommandFinished extends IListener {

	/**
	 * Event when a command is finished
	 * 
	 * @param unit
	 *            the unit who's command was finished
	 * @param commandId
	 *            the command id
	 * @param commandTopicId
	 *            the topic id of the command
	 */
	public void commandFinished(Unit unit, int commandId, int commandTopicId);
}
