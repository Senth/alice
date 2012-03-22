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

import java.util.logging.Level;

import alice.AlIce;
import alice.Defs;
import alice.TaskUnit;
import alice.General.GameTimeTypes;
import alice.interfaces.IEnemyEvents;

import com.springrts.ai.AICommand;
import com.springrts.ai.AIFloat3;
import com.springrts.ai.command.AttackUnitAICommand;
import com.springrts.ai.oo.Unit;
import com.springrts.ai.oo.WeaponDef;

/**
 * Assigns a unit to attack a target. Completes when the unit dies, fails when
 * the unit comes disappears.
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TAttackTarget extends Task implements IEnemyEvents {

	/**
	 * @param alIce
	 *            The AI-interface
	 * @param taskUnit
	 *            The unit that should attack the target
	 * @param targetId
	 *            The id of the target to attack
	 */
	public TAttackTarget(AlIce alIce, TaskUnit taskUnit, int targetId) {
		super(alIce);
		mUnit = taskUnit;
		mTargetId = targetId;
		mAttackCommandSent = false;
		mKilledTarget = false;
		mFailType = FailTypes.NO_FAIL;
		mLastEnemyCheck = mAlIce.getGameTime(GameTimeTypes.SECONDS);
		mStartAttackTime = 0.0;

		mAlIce.addEnemyEventListener(mTargetId, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#execute()
	 */
	@Override
	public Status execute() {
		Status status = Status.EXECUTED_SUCCESSFULLY;

		if (mAttackCommandSent && mStartAttackTime + Defs.ATTACK_TARGET_TIMEOUT < mAlIce.getGameTime(GameTimeTypes.SECONDS)) {
			mFailType = FailTypes.TIMED_OUT;
		} else if (mLastEnemyCheck + Defs.ENEMY_CHECK_TIME < mAlIce.getGameTime(GameTimeTypes.SECONDS)) {
			mLastEnemyCheck = mAlIce.getGameTime(GameTimeTypes.SECONDS);

			if (!mAlIce.enemyExists(mTargetId)) {
				mFailType = FailTypes.TARGET_LOST;
			}
		} else if (mUnit.getUnit() == null) {
			mFailType = FailTypes.UNIT_DIED;
			mAlIce.log(Level.FINE, "Unit trying to attack is destroyed");
		} else if (mKilledTarget) {
			mAlIce.log(Level.FINE, "Target is destroyed!");
			status = Status.COMPLETED_SUCCESSFULLY;
		}

		if (mFailType != FailTypes.NO_FAIL) {
			mAlIce.log(Level.FINE, "Attack failed: " + mFailType.toString());
			status = Status.UNEXPECTED_ERROR;
		} else if (!mAttackCommandSent) {
			sendAICommand();
		}

		// the task is done, remove the listener
		if (status != Status.EXECUTED_SUCCESSFULLY) {
			mAlIce.removeEnemyEventListener(mTargetId, this);
		}

		return status;
	}

	/**
	 * Returns the current fail type of the task
	 * 
	 * @return the current fail type of the task
	 */
	public FailTypes getFailType() {
		return mFailType;
	}

	/**
	 * The fail types that the attack task can have
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public enum FailTypes {
		/**
		 * When the task hasn't failed
		 */
		NO_FAIL,
		/**
		 * The target left line-of-sight
		 */
		TARGET_LEFT_LOS,
		/**
		 * Target was lost by some means
		 */
		TARGET_LOST,
		/**
		 * Timed out
		 */
		TIMED_OUT,
		/**
		 * Could not issue the command
		 */
		COMMAND_FAILED,
		/**
		 * Our unit died
		 */
		UNIT_DIED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#halt()
	 */
	@Override
	public void halt() {
		// TODO_LOW halt() - Implement this later
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.tasks.Task#resume()
	 */
	@Override
	public void resume() {
		// TODO_LOW resume() - Implement this later
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IEnemyEvents#enemyDamaged(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit, float, com.springrts.ai.AIFloat3,
	 * com.springrts.ai.oo.WeaponDef, boolean)
	 */
	@Override
	public void enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
		// Don't implement this
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEvents#enemyDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyDestroyed(Unit enemy, Unit attacker) {
		mKilledTarget = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEvents#enemyEnterLOS(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyEnterLOS(Unit enemy) {
		// TODO_LOW enemyEnterLOS - See enemyLeaveLOS

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEvents#enemyEnterRadar(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyEnterRadar(Unit enemy) {
		// TODO_LOW enemyEnterRadar() - See enemyLeaveLOS
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEvents#enemyLeaveLOS(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyLeaveLOS(Unit enemy) {
		mFailType = FailTypes.TARGET_LEFT_LOS;
		// TODO_LOW Implement a functionality when we don't attack the target if
		// it has both left our line of sight and radar.

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEvents#enemyLeaveRadar(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyLeaveRadar(Unit enemy) {
		// TODO_LOW enemyLeaveRadar() - See enemyLeaveLOS
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IEnemyEvents#unitDamaged(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit, float, com.springrts.ai.AIFloat3,
	 * com.springrts.ai.oo.WeaponDef, boolean)
	 */
	@Override
	public void unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
		// Don't implement this
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IEnemyEvents#unitDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitDestroyed(Unit unit, Unit attacker) {
		// Don't implement this
	}

	/**
	 * Send the attack target command
	 */
	private void sendAICommand() {
		mAlIce.log(Level.FINE, "Sending AI Attack command!");
		// Send the attack command to the engine
		AICommand attackCommand = new AttackUnitAICommand(mUnit.getUnitId(), -1, 0, Defs.TIME_OUT, mTargetId);
		// If command is valid, execute it
		if (attackCommand != null) {
			mAttackCommandSent = true;
			int res = mAlIce.handleEngineCommand(attackCommand);
			// Check if everything went OK
			if (res != 0) {
				mAlIce.log(Level.SEVERE, "Sending attack command failed!");
				mFailType = FailTypes.COMMAND_FAILED;
			}
		}

		// Set the start time
		mStartAttackTime = mAlIce.getGameTime(GameTimeTypes.SECONDS);
	}

	/**
	 * The last time we checked for enemies
	 */
	private double mLastEnemyCheck;
	/**
	 * Start attack time
	 */
	private double mStartAttackTime;
	/**
	 * True if the AttackUnitAICommand has been sent to the engine
	 */
	private boolean mAttackCommandSent;
	/**
	 * The unit that should attack the target
	 */
	private TaskUnit mUnit;
	/**
	 * The target to attack
	 */
	private int mTargetId;
	/**
	 * The fail type of the task
	 */
	private FailTypes mFailType;
	/**
	 * If "we" have killed the target
	 */
	private boolean mKilledTarget;

}
