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
import alice.SightedEnemies;
import alice.TaskUnit;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Unit;

/**
 * Task for creating an attack group that attacks ground units
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TGroundAttack extends TBaseAttack {

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
	public TGroundAttack(AlIce alIce, LinkedList<TaskUnit> attackForce, LinkedList<TaskUnit> healers) {
		super(alIce, attackForce, healers);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.TBaseAttack#getCloseEnemy()
	 */
	@Override
	protected Unit getCloseEnemy() {
		return mAlIce.getClosestEnemyInRange(getGroupPosition(), Defs.ATTACK_SEARCH_RADIUS, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.TBaseAttack#getTargetPosition()
	 */
	@Override
	protected AIFloat3 getTargetLocation() {
		SightedEnemies sightedEnemies = mAlIce.getSightedEnemies();
		AIFloat3 targetLocation = null;

		// Check with an extraction point first
		targetLocation = sightedEnemies.getClosestEnemyPositionByDef(getGroupPosition(), Defs.MetalExtractor.unitName);

		// No extraction point was found, attack a close unit
		if (targetLocation == null) {
			targetLocation = sightedEnemies.getClosestEnemyPosition(getGroupPosition(), false);
		}

		return targetLocation;
	}
}
