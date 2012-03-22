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

/**
 * Listens to messages sent by the chat
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public interface IMessage extends IListener {
	/**
	 * Event that is sent when a player, including our AI, sends a message
	 * 
	 * @param player
	 *            id of the player
	 * @param message
	 *            the message
	 */
	public void message(int player, String message);
}
