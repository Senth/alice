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

package alice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import alice.Defs.UnitGroup;
import alice.General.GameTimeTypes;
import alice.interfaces.IEnemyDestroyed;
import alice.interfaces.IEnemyEnterLOS;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Unit;
import com.springrts.ai.oo.WeaponDef;
import com.springrts.ai.oo.WeaponMount;

/**
 * 
 * A class containing all the sighted enemy units and latest known information
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class SightedEnemies implements IEnemyDestroyed, IEnemyEnterLOS {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            The AI-Interface
	 */
	public SightedEnemies(AlIce alIce) {
		mAlIce = alIce;
		mAlIce.addEventListener(this);
		// Initiate
		mAllEnemies = new HashMap<Integer, Enemy>();
		mEnemyGroups = new HashMap<String, HashMap<Integer, Enemy>>();

		// Initialize groups
		mGroupArmorTypeHealth = new HashMap<String, HashMap<String, Double>>();
		mGroupDamageTypeDps = new HashMap<String, HashMap<String, Double>>();
		for (String group : Defs.UnitGroup.TYPES) {
			mGroupArmorTypeHealth.put(group, new HashMap<String, Double>());
			mGroupDamageTypeDps.put(group, new HashMap<String, Double>());
		}
	}

	/**
	 * Returns a HashMap with all the enemies of a specific group
	 * 
	 * @param groupName
	 *            The wanted group of enemies
	 * @return A HashMap containing all the enemies of the specified group
	 */
	public HashMap<Integer, Enemy> getEnemiesByGroup(String groupName) {
		return mEnemyGroups.get(groupName);
	}

	/**
	 * Returns the enemy's total health of the specific armor type
	 * 
	 * @param groupType
	 *            the group we want to get the armor health from
	 * @param armorType
	 *            the health of the armor type we want to get
	 * @return the total health of the specified armor type from the group
	 */
	public double getEnemyArmorTypeHealth(String groupType, String armorType) {
		HashMap<String, Double> group = mGroupArmorTypeHealth.get(groupType);
		if (group != null) {
			Double health = group.get(armorType);
			if (health != null) {
				return health;
			}
		}

		return 0.0;
	}

	/**
	 * @return Returns all enemies
	 */
	public HashMap<Integer, Enemy> getAllEnemies() {
		return mAllEnemies;
	}

	/**
	 * Returns the enemy's total DPS of the specified damage type
	 * 
	 * @param groupType
	 *            the group we want to get the armor health from
	 * @param damageType
	 *            the damage type to get the total DPS of
	 * @return the total DPS of the specified damage type from the group
	 */
	public double getEnemyDamageTypeDps(String groupType, String damageType) {
		HashMap<String, Double> group = mGroupDamageTypeDps.get(groupType);
		if (group != null) {
			Double dps = group.get(damageType);
			if (dps != null) {
				return dps;
			}
		}

		return 0.0;
	}

	/**
	 * Returns the total health of the enemies' flying units
	 * 
	 * @return the flying health
	 */
	public double getFlyingHealth() {
		return mFlyingHealth;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyDestroyed#enemyDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyDestroyed(Unit enemy, Unit attacker) {
		Enemy existingEnemy = mAllEnemies.remove(enemy.getUnitId());
		// Only remove it if we have added it. existingEnemy will be null
		// if we destroyed the enemy before we have seen it (e.g. nuke).
		mAlIce.log(Level.FINE, "Killed unit: " + enemy.getDef().getName());
		if (existingEnemy != null && !existingEnemy.mUnitGroup.groups.isEmpty()) {
			HashMap<Integer, Enemy> enemyGroup = mEnemyGroups.get(existingEnemy.mUnitGroup.groups.getFirst());
			if (enemyGroup != null) {
				enemyGroup.remove(enemy.getUnitId());
				removeEnemy(existingEnemy);
			} else {
				mAlIce.log(Level.SEVERE, "Enemy group was null. Should never happen!");
			}

		} else if (existingEnemy != null && existingEnemy.mUnitGroup.groups.isEmpty()) {

			mAlIce.log(Level.SEVERE, "Group is empty: " + existingEnemy.mUnitGroup.unitName);
		}
	}

	/**
	 * Returns the closest enemy position
	 * 
	 * @param fromPosition
	 *            The position to look from
	 * @param isFlying
	 *            If the enemy should be able to fly or not
	 * @return The closest enemy position, null if none are found
	 */
	public AIFloat3 getClosestEnemyPosition(AIFloat3 fromPosition, boolean isFlying) {
		HashMap<Integer, Enemy> allEnemies = mAllEnemies;
		Iterator<Entry<Integer, Enemy>> it = allEnemies.entrySet().iterator();
		float distance = Float.MAX_VALUE;
		float min = Float.MAX_VALUE;
		AIFloat3 closestEnemyPos = null;
		while (it.hasNext()) {
			Enemy currentEnemy = it.next().getValue();
			boolean isAbleToFly = false;
			if (currentEnemy.mUnitGroup != null) {
				if (currentEnemy.mUnitGroup.unitDef != null) {
					isAbleToFly = currentEnemy.mUnitGroup.unitDef.isAbleToFly();
				}
			}

			if ((isAbleToFly && isFlying) || (!isFlying && !isAbleToFly)) {
				distance = SpringHelper.getDist(fromPosition, currentEnemy.getPosition());
				if (min > distance && currentEnemy != null) {
					min = distance;
					closestEnemyPos = currentEnemy.getPosition();
				}

			}
		}
		return closestEnemyPos;
	}

	/**
	 * Returns the closest enemy position of a specified unit.
	 * 
	 * @param fromPosition
	 *            The position to look from
	 * @param unitDef
	 *            the name of the unit to look for
	 * @return The closest enemy position by the definition, null if none are
	 *         found
	 */
	public AIFloat3 getClosestEnemyPositionByDef(AIFloat3 fromPosition, String unitDef) {
		HashMap<Integer, Enemy> allEnemies = mAllEnemies;
		Iterator<Entry<Integer, Enemy>> it = allEnemies.entrySet().iterator();
		float distance = Float.MAX_VALUE;
		float min = Float.MAX_VALUE;
		AIFloat3 closestEnemyPos = null;
		while (it.hasNext()) {
			Enemy currentEnemy = it.next().getValue();
			if (currentEnemy.mUnitGroup.unitName.equals(unitDef)) {
				distance = SpringHelper.getDist(fromPosition, currentEnemy.getPosition());
				if (min > distance && currentEnemy != null) {
					min = distance;
					closestEnemyPos = currentEnemy.getPosition();
				}

			}
		}
		return closestEnemyPos;
	}

	/**
	 * Updates the positions of sighted enemies
	 * 
	 * @param enemies
	 *            the enemies we can see atm.
	 */
	public void update(List<Unit> enemies) {
		// Iterate through the enemies and update their last seen position
		for (Unit enemy : enemies) {
			Enemy localEnemy = mAllEnemies.get(enemy.getUnitId());
			if (localEnemy != null) {
				localEnemy.mPosition = new AIFloat3(enemy.getPos());
				localEnemy.mLastSeen = mAlIce.getGameTime(GameTimeTypes.SECONDS);
			}
			// If the enemy don't exist create the enemy
			else {
				createEnemy(enemy);
			}
		}
		// Update map, check if last seen is more than a certant threshold, if
		// it is, remove the enemy
		Iterator<Entry<Integer, Enemy>> it = mAllEnemies.entrySet().iterator();
		double currentTime = mAlIce.getGameTime(GameTimeTypes.SECONDS);
		while (it.hasNext()) {
			Enemy currentEnemy = it.next().getValue();
			if (currentTime - currentEnemy.mLastSeen > Defs.LAST_SEEN_TIMELIMIT) {
				it.remove();
				removeEnemy(currentEnemy);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEnterLOS#enemyEnterLOS(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyEnterLOS(Unit enemy) {
		Enemy existingEnemy = mAllEnemies.get(enemy.getUnitId());

		// Add the unit if we don't have it
		if (existingEnemy == null) {
			createEnemy(enemy);
		} else {
			existingEnemy.mPosition = enemy.getPos();
		}
	}

	/**
	 * Creates an enemy and adds it to the list
	 * 
	 * @param enemy
	 *            the new enemy to add
	 */
	private void createEnemy(Unit enemy) {
		// Get the enemy's group
		UnitGroup unitGroup = Defs.getUnitGroup(enemy.getDef().getName());
		if (unitGroup == null) {
			mAlIce.log(Level.FINE, "The enemy's unitGroup is null! WTH!?");
			mAlIce.log(Level.FINE, "The retarded enemy is a: " + enemy.getDef().getName());
		}
		Enemy newEnemy = new Enemy(enemy.getPos(), unitGroup, enemy.getMaxHealth());
		mAllEnemies.put(enemy.getUnitId(), newEnemy);

		// Put the enemy's health into flying if it's flying
		if (enemy.getDef().isAbleToFly()) {
			mFlyingHealth += enemy.getMaxHealth();
		}

		if (unitGroup != null) {
			String groupName = unitGroup.groups.getFirst();
			HashMap<Integer, Enemy> enemyGroup = mEnemyGroups.get(groupName);

			// If the group does not exists, create it.
			if (enemyGroup == null) {
				enemyGroup = new HashMap<Integer, Enemy>();
				mEnemyGroups.put(groupName, enemyGroup);
			}
			// Put the enemy in right group
			enemyGroup.put(enemy.getUnitId(), newEnemy);

			// Get the dps of the unit
			List<WeaponMount> weaponMounts = newEnemy.mUnitGroup.unitDef.getWeaponMounts();
			if (!weaponMounts.isEmpty()) {
				WeaponDef weaponDef = weaponMounts.get(0).getWeaponDef();
				List<Float> types = weaponDef.getDamage().getTypes();
				newEnemy.mDps = (double) types.get(0) / weaponDef.getReload();
			}

			// Add the health to the armor type of it's groups
			for (String groupType : newEnemy.mUnitGroup.groups) {
				// Health
				HashMap<String, Double> armorTypeHealth = mGroupArmorTypeHealth.get(groupType);
				Double health = armorTypeHealth.get(newEnemy.mUnitGroup.armorType);

				if (health == null) {
					health = new Double(0.0);
				}

				health += enemy.getMaxHealth();
				armorTypeHealth.put(newEnemy.mUnitGroup.armorType, health);

				// Add the DPS of the unit to the damage type if we had a
				// damage type and DPS
				if (newEnemy.mUnitGroup.damageType != null && newEnemy.mDps != 0.0) {
					// Add the dps to the damage type dps
					HashMap<String, Double> damageTypeDps = mGroupDamageTypeDps.get(groupType);
					Double dps = damageTypeDps.get(newEnemy.mUnitGroup.damageType);
					if (dps == null) {
						dps = new Double(0.0);
					}
					dps += newEnemy.mDps;
					// Java is stupid. Re-add the value
					damageTypeDps.put(newEnemy.mUnitGroup.damageType, dps);
				}
			}
		}
	}

	/**
	 * Removes the specified enemy
	 * 
	 * @param enemy
	 *            the existing enemy that is being removed
	 */
	public void removeEnemy(Enemy enemy) {
		// If the enemy is flying remove it's health from flying
		if (enemy.mUnitGroup.unitDef.isAbleToFly()) {
			mFlyingHealth -= enemy.mMaxHealth;
		}

		// Remove the unit's health and DPS from the armor/damage type
		// of the groups it belongs to
		for (String group : enemy.mUnitGroup.groups) {
			HashMap<String, Double> groupArmor = mGroupArmorTypeHealth.get(group);
			if (groupArmor != null) {
				Double armorHealth = groupArmor.get(enemy.mUnitGroup.armorType);
				if (armorHealth != null) {
					armorHealth -= enemy.mMaxHealth;
					groupArmor.put(enemy.mUnitGroup.armorType, armorHealth);
				}
			}
			HashMap<String, Double> groupDps = mGroupDamageTypeDps.get(group);
			if (groupDps != null) {
				Double dps = groupDps.get(enemy.mUnitGroup.damageType);
				if (dps != null) {
					dps -= enemy.mDps;
					groupDps.put(enemy.mUnitGroup.damageType, dps);
				}
			}
		}
	}

	/**
	 * The AI-Interface
	 */
	private AlIce mAlIce;
	/**
	 * Map of all sighted enemies
	 */
	private HashMap<Integer, Enemy> mAllEnemies;
	/**
	 * Map containing enemies by group
	 */
	private HashMap<String, HashMap<Integer, Enemy>> mEnemyGroups;
	/**
	 * Contains the total health of the specified armor type by group
	 */
	private HashMap<String, HashMap<String, Double>> mGroupArmorTypeHealth;
	/**
	 * Contains the DPS of the specified weapon type by group
	 */
	private HashMap<String, HashMap<String, Double>> mGroupDamageTypeDps;
	/**
	 * The total health of flying units
	 */
	private double mFlyingHealth;

	/**
	 * A class representing the enemy and it's information
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public class Enemy {
		/**
		 * Constructor
		 * 
		 * @param unitPos
		 *            The position of the unit
		 * @param unitGroup
		 *            The group the unit belongs to
		 * @param maxHealth
		 *            The maximum health of the unit
		 */
		public Enemy(AIFloat3 unitPos, UnitGroup unitGroup, float maxHealth) {
			mUnitGroup = unitGroup;
			mDps = 0.0;
			mPosition = new AIFloat3(unitPos);
			mLastSeen = mAlIce.getGameTime(GameTimeTypes.SECONDS);
			mMaxHealth = (double) maxHealth;
		}

		/**
		 * Returns the latest known position
		 * 
		 * @return The position
		 */
		public AIFloat3 getPosition() {
			return mPosition;
		}

		/**
		 * Returns the unit group
		 * 
		 * @return the unit group
		 */
		public UnitGroup getUnitGroup() {
			return mUnitGroup;
		}

		/**
		 * Time when the unit was last seen
		 */
		private double mLastSeen;
		/**
		 * The latest position
		 */
		private AIFloat3 mPosition;
		/**
		 * The DPS of the unit
		 */
		private Double mDps;
		/**
		 * The max health of the unit
		 */
		private Double mMaxHealth;
		/**
		 * The unit group
		 */
		private UnitGroup mUnitGroup;
	}

}
