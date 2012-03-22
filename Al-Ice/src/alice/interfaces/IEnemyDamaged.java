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
 * Listens to events when an enemy is damaged
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public interface IEnemyDamaged extends IListener {
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
	public void enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer);
}
