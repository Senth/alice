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

import java.util.ArrayList;
import java.util.logging.Level;

import alice.AlIce;
import alice.Defs;
import alice.TaskUnit;
import alice.interfaces.IMessage;

import com.springrts.ai.AICommand;
import com.springrts.ai.command.RepairUnitAICommand;
import com.springrts.ai.command.StopUnitAICommand;

/**
 * Repairs a unit with the selected unit (if it can repair)
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TRepairUnit extends Task implements IMessage {

	/**
	 * @param alIce
	 *            the AI-Interface
	 * @param repairer
	 *            the unit that will repair the unitToRepair unit
	 * @param unitToRepair
	 *            the unit we want to repair
	 */
	public TRepairUnit(AlIce alIce, TaskUnit repairer, TaskUnit unitToRepair) {
		super(alIce);

		if (repairer == null) {
			mAlIce.log(Level.WARNING, "Repairer is null!");
		}

		if (unitToRepair == null) {
			mAlIce.log(Level.WARNING, "The unit to repair is null!");
		}

		mAlIce.addEventListener(this);

		mRepairer = repairer;
		mUnitToRepair = unitToRepair;
		mCommandIssued = false;
		mFailed = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {
		if (mFailed == true || mUnitToRepair.getUnit() == null || mRepairer.getUnit() == null) {
			return Status.UNEXPECTED_ERROR;
		} else if (!mCommandIssued) {
			mAlIce.log(Level.FINER, "Issuing repair command");
			boolean commandOk = sendRepairCommand();
			mAlIce.log(Level.FINER, "Repair command issued");

			mCommandIssued = true;

			if (!commandOk) {
				return Status.FAILED_CLEANLY;
			}
		}

		// If the unit we want to repair has full health (even before we start
		// repairing) we have completed the task
		if (mUnitToRepair.getUnit().getHealth() == mUnitToRepair.getUnit().getMaxHealth()) {
			return Status.COMPLETED_SUCCESSFULLY;
		}

		return Status.EXECUTED_SUCCESSFULLY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#halt()
	 */
	@Override
	public void halt() {
		// Remove the command from the unit if we have issued the command
		AICommand command = new StopUnitAICommand(mRepairer.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT);

		// If command is valid, execute it
		if (command != null) {
			int res = mAlIce.handleEngineCommand(command);

			// Check if there were some errors report it
			if (res != 0) {
				mAlIce.log(Level.WARNING, "ERROR! Couldn't issuing stop command!");
			}
		} else {
			mAlIce.log(Level.WARNING, "ERROR! Couldn't create stop command!");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		// reset the send command
		mCommandIssued = false;
	}

	/**
	 * Sends the repair command
	 * 
	 * @return true if successful
	 */
	private boolean sendRepairCommand() {
		AICommand command = new RepairUnitAICommand(mRepairer.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT,
				mUnitToRepair.getUnit());

		// If command is valid, execute it
		if (command != null) {
			int res = mAlIce.handleEngineCommand(command);
			// Check if everything went OK
			if (res == 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * The repair unit, i.e. the one that will repair the mUnitToRepair
	 */
	private TaskUnit mRepairer;

	/**
	 * The unit we want to repair.
	 */
	private TaskUnit mUnitToRepair;

	/**
	 * If the task has failed somehow
	 */
	private boolean mFailed;

	/**
	 * If we have sent the repair command
	 */
	private boolean mCommandIssued;

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IMessage#message(int, java.lang.String)
	 */
	@Override
	public void message(int player, String message) {
		if (message.equals("halt")) {
			mAlIce.getTaskHandler().halt(this);
		} else if (message.equals("resume")) {
			mAlIce.getTaskHandler().resume(this);
		}

	}
}
