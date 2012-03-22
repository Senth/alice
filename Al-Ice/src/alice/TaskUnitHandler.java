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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;

import alice.Defs.UnitGroup;
import alice.interfaces.IUnitCreated;
import alice.interfaces.IUnitDestroyed;
import alice.interfaces.IUnitFinished;

import com.springrts.ai.oo.Unit;

/**
 * Handles all the units and builders
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TaskUnitHandler implements IUnitCreated, IUnitDestroyed, IUnitFinished {

	/**
	 * Set the AI interface and initiate HashMaps
	 * 
	 * @param alIce
	 *            The AI interface
	 */
	public TaskUnitHandler(AlIce alIce) {
		mAlIce = alIce;
		mFinishedUnits = new HashMap<Integer, TaskUnit>();
		mCreatedUnits = new LinkedHashMap<Integer, TaskUnit>();
		mcFinishedUnits = new HashMap<String, Integer>();
		mcAllUnits = new HashMap<String, Integer>();

		// Add groups
		mGroupedUnits = new HashMap<String, LinkedList<TaskUnit>>();
		for (String groupType : Defs.UnitGroup.TYPES) {
			mGroupedUnits.put(groupType, new LinkedList<TaskUnit>());
		}

		// Add ourself to the event listener
		mAlIce.addEventListener(this);
	}

	/**
	 * Returns specified unit
	 * 
	 * @param unitId
	 *            ID of the unit
	 * @return TaskUnit with the specified unitId, null if not found
	 */
	public TaskUnit getTaskUnit(int unitId) {
		TaskUnit foundUnit = mFinishedUnits.get(unitId);

		// Try to find the unit in the created units instead
		if (foundUnit == null) {
			foundUnit = mCreatedUnits.get(unitId);
		}

		return foundUnit;
	}

	/**
	 * Searches the units for free units by the specified def
	 * 
	 * @param def
	 *            Definition of the unit
	 * @return A linked list containing all the free units by the specified def
	 */
	public LinkedList<TaskUnit> getFreeUnitsByDef(String def) {
		LinkedList<TaskUnit> freeUnits = new LinkedList<TaskUnit>();
		Iterator<Entry<Integer, TaskUnit>> it = mFinishedUnits.entrySet().iterator();

		while (it.hasNext()) {
			TaskUnit currentTaskUnit = it.next().getValue();
			if (currentTaskUnit.getDefName().equals(def) && currentTaskUnit.isFree()) {
				freeUnits.add(currentTaskUnit);
			}
		}

		return freeUnits;
	}

	/**
	 * Returns the number of finished units by the specified definition.
	 * 
	 * @param def
	 *            Definition of the unit
	 * @return Number of finished units by the specified definition
	 */
	public int getNrFinishedUnits(String def) {
		Integer cUnits = mcFinishedUnits.get(def);

		if (cUnits != null) {
			return cUnits;
		} else {
			return 0;
		}
	}

	/**
	 * Returns the number of units by the specified definition. This includes
	 * all the units that haven't been finished yet. oe
	 * 
	 * @param def
	 *            Definition of the unit
	 * @return Number of finished units by the specified definition
	 */
	public int getNrAllUnits(String def) {
		Integer cUnits = mcAllUnits.get(def);

		if (cUnits != null) {
			return cUnits;
		} else {
			return 0;
		}
	}

	/**
	 * Returns the number of anti-air units we have
	 * 
	 * @return the number of anti-air units we have
	 */
	public int getNrAntiAirUnits() {
		return mcAntiAirUnits;
	}

	/**
	 * Returns a list of all the units belonging to that group
	 * 
	 * @param groupName
	 *            The units should belong to this group
	 * 
	 * @return A list of units belonging to specified group. null if the group
	 *         doesn't exist. Don't modify this list directly since it's a
	 *         direct reference.
	 */
	public LinkedList<TaskUnit> getUnitsByGroup(String groupName) {
		return mGroupedUnits.get(groupName);
	}

	/**
	 * Returns a list of free units belonging to that group
	 * 
	 * @param groupName
	 *            The units should belong to this group
	 * 
	 * @return A list of free units belonging to specified group
	 */
	public LinkedList<TaskUnit> getFreeUnitsByGroup(String groupName) {
		LinkedList<TaskUnit> groupUnits = mGroupedUnits.get(groupName);
		if (groupUnits == null) {
			return null;
		}

		// Iterate through all the units and add those that are free
		LinkedList<TaskUnit> freeGroupUnits = new LinkedList<TaskUnit>();
		for (TaskUnit taskUnit : groupUnits) {
			if (taskUnit.isFree()) {
				freeGroupUnits.add(taskUnit);
			}
		}

		return freeGroupUnits;
	}

	// Events

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.IUnitDestroyed#unitDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitDestroyed(Unit unit, Unit attacker) {
		TaskUnit removedUnit = mFinishedUnits.remove(unit.getUnitId());
		if (removedUnit != null) {
			// Set the unit to null in the TaskUnit
			removedUnit.destroyUnit();

			String unitDef = unit.getDef().getName();
			// Decrement the values
			Integer cUnits = mcFinishedUnits.get(unitDef);
			// Only decrement if the unit isn't building
			if (cUnits != null && !unit.isBeingBuilt()) {
				mcFinishedUnits.put(unitDef, --cUnits);
			}
			cUnits = mcAllUnits.get(unitDef);
			if (cUnits != null) {
				mcAllUnits.put(unitDef, --cUnits);
			}
			// Remove the unit from the groups
			UnitGroup unitGroup = removedUnit.getUnitGroup();
			if (unitGroup != null) {
				for (String group : unitGroup.groups) {
					LinkedList<TaskUnit> groupUnits = mGroupedUnits.get(group);
					if (groupUnits != null) {
						groupUnits.remove(removedUnit);
					}
				}
			}

			if (removedUnit.getUnitGroup().canAttackAir && removedUnit.getDef().isAbleToMove()) {
				mcAntiAirUnits--;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.IUnitFinished#unitFinished(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitFinished(Unit unit) {
		// Move the unit from created units to units. I.e. remove from one and
		// add in the other
		TaskUnit taskUnit = mCreatedUnits.remove(unit.getUnitId());
		mFinishedUnits.put(unit.getUnitId(), taskUnit);

		// Add the unit to the groups it belongs to.
		UnitGroup unitGroup = taskUnit.getUnitGroup();
		if (unitGroup != null) {
			for (String group : unitGroup.groups) {
				LinkedList<TaskUnit> groupUnits = mGroupedUnits.get(group);
				if (groupUnits != null) {
					groupUnits.add(taskUnit);
				}
			}
		}

		String unitDef = unit.getDef().getName();

		// Increment the number of finished units we have
		Integer cUnits = mcFinishedUnits.get(unitDef);
		// If we don't have a unit with that name yet, add it to the map
		if (cUnits == null) {
			mcFinishedUnits.put(unitDef, new Integer(1));
		} else {
			// Since Java is stupid we have to put the value back
			mcFinishedUnits.put(unitDef, ++cUnits);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.IUnitCreated#unitCreated(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitCreated(Unit unit, Unit builder) {
		// Add the unit to the created list
		TaskUnit createdUnit = new TaskUnit(mAlIce, unit);
		mCreatedUnits.put(unit.getUnitId(), createdUnit);
		mAlIce.log(Level.FINE, "Added new TaskUnit: " + unit.getUnitId());

		String unitDef = unit.getDef().getName();

		// Increment the number of all units we have
		Integer cUnits = mcAllUnits.get(unitDef);
		// If we don't have a unit with that name yet, add it to the map
		if (cUnits == null) {
			mcAllUnits.put(unitDef, new Integer(1));
		} else {
			// Since Java is stupid we have to put the value back
			mcAllUnits.put(unitDef, ++cUnits);
		}

		if (createdUnit.getUnitGroup().canAttackAir && createdUnit.getDef().isAbleToMove()) {
			mcAntiAirUnits++;
		}
	}

	/**
	 * The AI interface
	 */
	private AlIce mAlIce;

	/**
	 * HashMap for all the units
	 */
	private HashMap<Integer, TaskUnit> mFinishedUnits;

	/**
	 * Map for created units, but not finished.
	 */
	private HashMap<Integer, TaskUnit> mCreatedUnits;

	/**
	 * Counter for the active and available units. Doesn't count those that
	 * haven't finished yet.
	 */
	private HashMap<String, Integer> mcFinishedUnits;

	/**
	 * Counter for all the units, also counts those that aren't finished.
	 */
	private HashMap<String, Integer> mcAllUnits;

	/**
	 * Number of anti-air units we have
	 */
	private int mcAntiAirUnits;

	/**
	 * List of the finished units grouped by the group they belong in. This is
	 * used for faster access.
	 * 
	 * @note if a unit belongs to more than one group you may get duplicates if
	 *       you merge two groups.
	 */
	private HashMap<String, LinkedList<TaskUnit>> mGroupedUnits;
}
