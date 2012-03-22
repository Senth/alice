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

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Unit;
import com.springrts.ai.oo.WeaponDef;

/**
 * Interface containing all the unit events
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public interface IUnitEvents {
	/**
	 * Event that is sent when an enemy is damaged.
	 * 
	 * @param enemy
	 *            the enemy that was attacked
	 * @param attacker
	 *            the attacker (doesn't have to be our unit)
	 * @param damage
	 *            the damage that was inflicted
	 * @param dir
	 *            the direction of the damage had?
	 * @param weaponDef
	 *            the weaponDef that inflicted the damage
	 * @param paralyzer
	 *            if the damaged was a paralyzer?
	 */
	void enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer);

	/**
	 * Event that is sent when an enemy is destroyed
	 * 
	 * @param enemy
	 *            the enemy that got destroyed
	 * @param attacker
	 *            the attacker that killed the enemy. Can be null if there
	 *            wasn't any specific attacker
	 */
	void enemyDestroyed(Unit enemy, Unit attacker);

	/**
	 * Event when a unit is created.
	 * 
	 * @param unit
	 *            the unit that was created
	 * @param builder
	 *            the builder that created the unit
	 */
	void unitCreated(Unit unit, Unit builder);

	/**
	 * Event that is sent when a unit is damaged.
	 * 
	 * @param unit
	 *            the unit that was damaged
	 * @param attacker
	 *            the attacker (doesn't have to be our unit)
	 * @param damage
	 *            the damage that was inflicted
	 * @param dir
	 *            the direction of the damage had?
	 * @param weaponDef
	 *            the weaponDef that inflicted the damage
	 * @param paralyzer
	 *            if the damaged was a paralyzer?
	 */
	void unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer);

	/**
	 * Event that is sent when a unit we own is destroyed
	 * 
	 * @param unit
	 *            the unit that was destroyed
	 * @param attacker
	 *            the attacker that destroyed the unit, can be null
	 */
	void unitDestroyed(Unit unit, Unit attacker);

	/**
	 * Event that is sent when a unit is finished
	 * 
	 * @param unit
	 *            the unit that was finished
	 */
	void unitFinished(Unit unit);

	/**
	 * Event that is sent when a unit is idle
	 * 
	 * @param unit
	 *            The unit being idle
	 */
	void unitIdle(Unit unit);

	/**
	 * Event that is sent when a unit failed to move
	 * 
	 * @param unit
	 *            The unit that failed to move
	 */
	void unitMoveFailed(Unit unit);

	/**
	 * Event that is sent when a unit fired
	 * 
	 * @param unit
	 *            The unit that fired
	 * @param weaponDef
	 *            Definition of weapon
	 */
	void weaponFired(Unit unit, WeaponDef weaponDef);
}
