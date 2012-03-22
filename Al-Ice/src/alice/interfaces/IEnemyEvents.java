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
 * Interface containing all the enemy unit events
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public interface IEnemyEvents {
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
	 * Event that is sent when an enemy enters line of sight. This event will
	 * probably not be very usefull.
	 * 
	 * @param enemy
	 *            the enemy that entered line of sight
	 */
	void enemyEnterLOS(Unit enemy);

	/**
	 * Event that is sent when an enemy enters the radar. This event will
	 * probably not be very usefull.
	 * 
	 * @param enemy
	 *            the enemy unit that entered our radar
	 */
	void enemyEnterRadar(Unit enemy);

	/**
	 * Event that is sent when an enemy leaves our line of sight and thus we
	 * might not be able to use it's unit variable any more after this event. If
	 * it's still under our radar we can still access the unit (not documented,
	 * can it be accessed?)
	 * 
	 * @param enemy
	 *            the enemy that left our line of sight
	 */
	void enemyLeaveLOS(Unit enemy);

	/**
	 * Event that is sent when an enemy leaves our radar and thus we might not
	 * be able to use it's unit variable any more after this event. If it's
	 * still under our line of sight we can still access the unit (not
	 * documented, can it be accessed?)
	 * 
	 * @param enemy
	 *            the enemy that left our radar
	 */
	void enemyLeaveRadar(Unit enemy);

	/**
	 * Event that is sent when a unit is damaged.
	 * 
	 * @param unit
	 *            the unit that was damaged
	 * @param attacker
	 *            in this case the attacker will always be the enemy we listen
	 *            to
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
	 *            in this case the attacker will always be the enemy we listen
	 *            to
	 */
	void unitDestroyed(Unit unit, Unit attacker);
}
