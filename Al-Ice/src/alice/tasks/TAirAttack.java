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

import alice.AlIce;
import alice.Defs;
import alice.TaskUnit;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Unit;

/**
 * Attacks a air targets
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TAirAttack extends TBaseAttack {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            The AI-interface
	 * @param attackForce
	 *            The attacking force
	 * @param healers
	 *            The healer force
	 */
	public TAirAttack(AlIce alIce, LinkedList<TaskUnit> attackForce, LinkedList<TaskUnit> healers) {
		super(alIce, attackForce, healers);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.TBaseAttack#getCloseEnemy()
	 */
	@Override
	protected Unit getCloseEnemy() {
		return mAlIce.getClosestEnemyInRange(getGroupPosition(), Defs.ATTACK_SEARCH_RADIUS, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.TBaseAttack#getTargetLocation()
	 */
	@Override
	protected AIFloat3 getTargetLocation() {
		return mAlIce.getSightedEnemies().getClosestEnemyPosition(getGroupPosition(), true);
	}

}
