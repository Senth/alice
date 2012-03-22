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
 * Listens to when a unit is finished, i.e. when it has been fully built.
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public interface IUnitFinished extends IListener {
	/**
	 * Event that is sent when a unit is finished
	 * 
	 * @param unit
	 *            the unit that was finished
	 */
	public void unitFinished(Unit unit);
}
