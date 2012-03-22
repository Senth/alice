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
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import alice.AlIce;
import alice.Defs;
import alice.SpringHelper;
import alice.TaskUnit;
import alice.interfaces.ITaskObserver;
import alice.interfaces.IUnitEvents;

import com.springrts.ai.AICommand;
import com.springrts.ai.AIFloat3;
import com.springrts.ai.command.BuildUnitAICommand;
import com.springrts.ai.command.RepairUnitAICommand;
import com.springrts.ai.command.StopUnitAICommand;
import com.springrts.ai.oo.Resource;
import com.springrts.ai.oo.Unit;
import com.springrts.ai.oo.UnitDef;
import com.springrts.ai.oo.WeaponDef;

/**
 * Task that builds a new unit with the specified uint
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class TBuildUnitByUnit extends Task implements IUnitEvents, ITaskObserver {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            AI-interface
	 * @param buildDef
	 *            Build definition
	 * @param builder
	 *            The TaskUnit that should build the definition
	 */
	public TBuildUnitByUnit(AlIce alIce, String buildDef, TaskUnit builder) {
		super(alIce);
		mBuilder = builder;
		mBuildDefName = buildDef;
		mBuildDef = Defs.getUnitGroup(buildDef).unitDef;
		mCommandIssued = false;
		mConstructedUnit = null;
		mFailed = false;
		mBuildSuccess = false;
		mListenerAdded = false;
		mMinSpace = Defs.MIN_BUILD_SPACE;
		mAlIce.log(Level.FINEST, "builder = " + mBuilder.getDefName() + ", buildDefName = " + mBuildDefName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.Task#execute()
	 */
	@Override
	public Status execute() {
		if (mFailed || mBuilder.getUnit() == null) {
			mAlIce.log(Level.FINE, "Something happend, I died or something else");
			return Status.UNEXPECTED_ERROR;
		}

		if (!mListenerAdded) {
			mAlIce.addUnitEventListener(mBuilder.getUnitId(), this);
			// mListenerAdded = true;
		}

		if (!mCommandIssued) {
			mBuildDef = SpringHelper.canBuild(mBuilder.getDef(), mBuildDefName);
			if (mBuildDef == null) {
				mAlIce.log(Level.SEVERE, "Can't build " + mBuildDefName + " with " + mBuilder.getDefName());
				return Status.FAILED_CLEANLY;
			} else {
				mCommandIssued = true;

				// Send the build command
				boolean buildOK = sendBuildCommand();
				if (!buildOK) {
					mAlIce.log(Level.SEVERE, "sendBuildCommand failed");
					return Status.FAILED_CLEANLY;
				}
			}
		} else if (mBuildSuccess) {
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
		AICommand command = new StopUnitAICommand(mBuilder.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT);

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
		// Resume building if we have a building
		if (mBuilder.getUnit() != null && mConstructedUnit != null && mConstructedUnit.getUnit() != null) {
			AICommand command = new RepairUnitAICommand(mBuilder.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT,
					mConstructedUnit.getUnit());

			// If command is valid, execute it
			if (command != null) {
				int res = mAlIce.handleEngineCommand(command);

				// Check if there were some errors report it
				if (res != 0) {
					mAlIce.log(Level.WARNING, "ERROR! Couldn't continue building!");
					mFailed = true;
				}
			} else {
				mAlIce.log(Level.WARNING, "ERROR! Couldn't create continue building command!");
				mFailed = true;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuildUnitByUnit(" + mBuildDefName + ", " + mBuilder.getDefName() + ")";
	}

	/**
	 * Sends the build command to the engine
	 * 
	 * @return 0 if OK
	 */
	private boolean sendBuildCommand() {
		try {
			AIFloat3 buildPos = null;
			// If the builder is a mobile unit, build close to the unit.
			if (isMobileUnit(mBuilder.getDefName())) {
				buildPos = calculateValidBuildPosition();
			} else {
				buildPos = mBuilder.getUnit().getPos();
			}

			// mAlIce.log(Level.FINEST, "UnitPos: " + mBuilder.getUnitPos() +
			// ", BuildPos: " + buildPos);

			// Create build command
			AICommand command = new BuildUnitAICommand(mBuilder.getUnit(), -1, new ArrayList<AICommand.Option>(), Defs.TIME_OUT,
					mBuildDef, buildPos, -1);

			// If command is valid, execute it
			if (command != null) {
				int res = mAlIce.handleEngineCommand(command);
				// Check if something went wrong
				if (res != 0) {
					mAlIce.log(Level.SEVERE, "Failed issuing command!");
					return false;
				}
			}

		} catch (Exception ex) {
			mAlIce.log(Level.SEVERE, "Exception: " + ex.toString());
			for (StackTraceElement element : ex.getStackTrace()) {
				mAlIce.log(Level.SEVERE, element.toString());
			}
			return false;
		}

		return true;
	}

	/**
	 * Returns true if the unit definition name represents a moving unit
	 * 
	 * @param name
	 *            The unit definition name
	 * @return True if the unit is a moving unit
	 */
	private boolean isMobileUnit(String name) {
		return name.equals(Defs.TheOverseer.unitName) || name.equals(Defs.TheArchitectGround.unitName) ||
				name.equals(Defs.TheArchitectAir.unitName) || name.equals(Defs.TheErector.unitName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitEvents#enemyDamaged(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit, float, com.springrts.ai.AIFloat3,
	 * com.springrts.ai.oo.WeaponDef, boolean)
	 */
	@Override
	public void enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
		// Does nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IUnitEvents#enemyDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void enemyDestroyed(Unit enemy, Unit attacker) {
		// Does nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitEvents#unitCreated(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitCreated(Unit unit, Unit builder) {
		// Only set the constructed unit once
		if (mConstructedUnit == null) {
			// Set the unit that we are building
			mConstructedUnit = mAlIce.getTaskUnitHandler().getTaskUnit(unit.getUnitId());

			if (mConstructedUnit != null) {
				mAlIce.addUnitEventListener(mConstructedUnit.getUnitId(), this);
			} else {
				mAlIce.log(Level.SEVERE, "Constructed unit is null! UnitId: " + unit.getUnitId());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitEvents#unitDamaged(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit, float, com.springrts.ai.AIFloat3,
	 * com.springrts.ai.oo.WeaponDef, boolean)
	 */
	@Override
	public void unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
		// Does nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitEvents#unitDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitDestroyed(Unit unit, Unit attacker) {
		mFailed = true;

		if (mBuilder.getUnit() != null) {
			mAlIce.removeUnitEventListener(mBuilder.getUnitId(), this);
		}
		if (mConstructedUnit != null) {
			mAlIce.removeUnitEventListener(mConstructedUnit.getUnitId(), this);
			mConstructedUnit = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitEvents#unitFinished(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitFinished(Unit unit) {
		if (mConstructedUnit != null && mConstructedUnit.getUnitId() == unit.getUnitId()) {
			// mAlIce.log(Level.FINER, "" + toString() + " finished building");

			mAlIce.removeUnitEventListener(unit.getUnitId(), this);
			mAlIce.removeUnitEventListener(mBuilder.getUnitId(), this);
			mBuildSuccess = true;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitEvents#unitIdle(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitIdle(Unit unit) {
		// Usually means that we've failed to do something or has build on
		// another's unit structure. But only if we haven't started to build
		// the unit
		if (mConstructedUnit == null) {
			mFailed = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * alice.interfaces.IUnitEvents#unitMoveFailed(com.springrts.ai.oo.Unit)
	 */
	@Override
	public void unitMoveFailed(Unit unit) {
		// Just fail the task and it will probably try to build again and
		// hopefully in another direction...
		mFailed = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IUnitEvents#weaponFired(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.WeaponDef)
	 */
	@Override
	public void weaponFired(Unit unit, WeaponDef weaponDef) {
		// Does nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.ITaskObserver#onTaskFinished(alice.tasks.Task,
	 * alice.tasks.Task.Status)
	 */
	@Override
	public void onTaskFinished(Task task, Status status) {
		// We only listen to the move to task if we need one. If the move to
		// task is finished we can set the commandIssued to false to try
		// building again.
		mCommandIssued = false;
	}

	/**
	 * Calculates the minimum build distance depending on the size of the
	 * building we're building. Only called when the builder is a mobile unit.
	 * 
	 * @return minimum build distance for mobile units.
	 */
	protected float calculateMinBuildDistance() {
		final float x = (mBuildDef.getXSize() + mBuilder.getDef().getXSize()) * BUILD_OFFSET_MULTIPLIER;
		final float z = (mBuildDef.getZSize() + mBuilder.getDef().getZSize()) * BUILD_OFFSET_MULTIPLIER;

		return (float) Math.sqrt(x * x + z * z);
	}

	/**
	 * Calculates a valid build position for mobile units. This method makes
	 * sure that we don't try to build on our builder.
	 * 
	 * @return valid build position for mobile units
	 */
	protected AIFloat3 calculateValidBuildPosition() {
		float minDistance = calculateMinBuildDistance();
		float multiplier = 1.0f;
		AIFloat3 buildPos = null;
		Random randomizer = new Random();

		do {
			// Randomize direction
			AIFloat3 direction = new AIFloat3(randomizer.nextFloat() - 0.5f, 0.0f, randomizer.nextFloat() - 0.5f);

			// Normalize the direction
			float length = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);
			direction.x /= length;
			direction.z /= length;

			// Use the direction to calculate an offset for the position
			AIFloat3 searchPos = mBuilder.getUnitPos();
			searchPos.x += minDistance * direction.x * multiplier;
			searchPos.z += minDistance * direction.z * multiplier;

			// Find the closest build site
			buildPos = mAlIce.getMap().findClosestBuildSite(mBuildDef, searchPos, SEARCH_RADIUS, mMinSpace, FACING);

			multiplier += 0.2f;
		} while (!validBuildPos(buildPos, true));

		return buildPos;
	}

	/**
	 * Checks if we can build the building on the specified position without
	 * moving our unit.
	 * 
	 * @param buildPos
	 *            the build position we want to build on
	 * @param checkExtractionPoints
	 *            if we should check metal extraction points for invalid places
	 *            to build on
	 * @return true if the build position is valid.
	 */
	protected boolean validBuildPos(AIFloat3 buildPos, boolean checkExtractionPoints) {
		// Check for minimum range from the unit
		int xDiff = Math.abs((int) (mBuilder.getUnitPos().x - buildPos.x));
		int zDiff = Math.abs((int) (mBuilder.getUnitPos().z - buildPos.z));
		int xSize = Math.abs(mBuilder.getUnit().getDef().getXSize() + mBuildDef.getXSize());
		xSize *= BUILD_OFFSET_MULTIPLIER;
		int zSize = Math.abs(mBuilder.getUnit().getDef().getZSize() + mBuildDef.getZSize());
		zSize *= BUILD_OFFSET_MULTIPLIER;

		if ((xDiff * xDiff + zDiff * zDiff) < (xSize * xSize + zSize * zSize)) {
			return false;
		}

		if (checkExtractionPoints) {
			// Check so that we don't build on a metal extraction point.
			float buildingSize = (float) (Math.sqrt(mBuildDef.getXSize() * mBuildDef.getXSize() + mBuildDef.getZSize() *
					mBuildDef.getZSize()) * BUILD_OFFSET_MULTIPLIER);
			Resource metal = mAlIce.getResource(Defs.Metal);
			List<AIFloat3> extractionPoints = mAlIce.getMap().getResourceMapSpotsPositions(metal);
			for (AIFloat3 extractionPoint : extractionPoints) {
				float distance = SpringHelper.getDist(buildPos.x, buildPos.z, extractionPoint.x, extractionPoint.z);
				if (distance < buildingSize) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Minimum distance between buildings
	 */
	protected int mMinSpace;

	/**
	 * The search radius
	 */
	protected static final float SEARCH_RADIUS = 10000;
	/**
	 * The facing of the building
	 */
	protected static final int FACING = -1;
	/**
	 * The build offset from the builder. Need so we don't place the building on
	 * ourselves.
	 */
	private static final int BUILD_OFFSET_MULTIPLIER = 4;
	/**
	 * The build definition
	 */
	protected UnitDef mBuildDef;
	/**
	 * String representation of the build definition
	 */
	protected String mBuildDefName;
	/**
	 * The builder
	 */
	protected TaskUnit mBuilder;
	/**
	 * If we have issued the command to the spring engine
	 */
	protected boolean mCommandIssued;
	/**
	 * The building was completed successfully
	 */
	private boolean mBuildSuccess;
	/**
	 * If we have added the listener for the unit
	 */
	private boolean mListenerAdded;
	/**
	 * The newly constructed unit
	 */
	private TaskUnit mConstructedUnit;
	/**
	 * True if the task has failed, e.g. if one of the unit has died
	 */
	private boolean mFailed;
}
