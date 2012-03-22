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
import java.util.ListIterator;
import java.util.logging.Level;

import alice.AlIce;

/**
 * A task that takes a list of units and checks if we have all of those. When we
 * have the task is completed
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class THaveUnits extends Task {

	/**
	 * Constructor that takes a list of unit definitions and another list (of
	 * equal size to the unit definition list).
	 * 
	 * @param alIce
	 *            the AI-Interface
	 * @param unitDefs
	 *            list of unit definitions
	 * @param cUnits
	 *            list (of equal size to unitDefs) that specifies the number of
	 *            units we need to have
	 */
	public THaveUnits(AlIce alIce, LinkedList<String> unitDefs, LinkedList<Integer> cUnits) {
		super(alIce);
		if (unitDefs.size() != cUnits.size()) {
			mAlIce.log(Level.SEVERE, "The lists doesn't have equal size!");
		}

		mUnitDefs = unitDefs;
		mcUnits = cUnits;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {

		// Iterate through all the units. As soon as one isn't done we return
		ListIterator<String> unitDefIt = mUnitDefs.listIterator();
		ListIterator<Integer> cUnitIt = mcUnits.listIterator();

		while (unitDefIt.hasNext()) {
			String unitDef = unitDefIt.next();
			int cUnit = cUnitIt.next();

			if (mAlIce.getTaskUnitHandler().getNrFinishedUnits(unitDef) < cUnit) {
				return Status.EXECUTED_SUCCESSFULLY;
			}
		}
		return Status.COMPLETED_SUCCESSFULLY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#halt()
	 */
	@Override
	public void halt() {
		// Does nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		// Does nothing
	}

	/**
	 * List with all the unit definitions that we need to have to complete this
	 * goal
	 */
	private LinkedList<String> mUnitDefs;

	/**
	 * Number of the units that we need to have to complete this goal
	 */
	private LinkedList<Integer> mcUnits;
}
