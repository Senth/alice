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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;

import alice.Defs.Priority;
import alice.Defs.UnitGroup;
import alice.ExtractionPointMap.Owner;
import alice.TaskUnit.TaskPriority;
import alice.interfaces.IMessage;
import alice.interfaces.ITaskObserver;
import alice.tasks.TAirAttack;
import alice.tasks.TBuildUnitByUnit;
import alice.tasks.TBuildUnitByUnitOnPos;
import alice.tasks.TGroundAttack;
import alice.tasks.TInitialBuildSequence;
import alice.tasks.TScout;
import alice.tasks.TScoutExtractionPoints;
import alice.tasks.TScoutRandom;
import alice.tasks.TScoutRoaming;
import alice.tasks.Task;
import alice.tasks.Task.Status;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.UnitDef;

/**
 * 
 * The General makes all the important decisions, sets tasks
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class General implements ITaskObserver, IMessage {

	/**
	 * Constructor
	 * 
	 * @param alIce
	 *            AI-Interface
	 */
	public General(AlIce alIce) {
		mAlIce = alIce;
		mInitBuildCommandSent = false;
		mInitBuildSequence = null; // Can't be set before update
		mcScouting = 0;
		mIsScouting = false;
		mScoutRandom = null;
		mScoutRoaming = null;
		mScoutExtraction = null;
		mLastAttackTime = 0.0;
		mLastBuilderForceBuilds = new HashMap<String, Double>();
		mLastPriorityTime = 0.0;
		mState = State.INITIAL_BUILD;
		mFreeBuilders = new LinkedList<TaskUnit>();
		mAlIce.addEventListener(this);
		mUnitPriorities = new Vector<UnitPriority>(Defs.getAllUnits().size());
		mLastMergeTime = mAlIce.getGameTime(GameTimeTypes.SECONDS);

		mGroundAttackAir = new LinkedList<TGroundAttack>();
		mGroundAttackGround = new LinkedList<TGroundAttack>();
		mAirAttackAir = new LinkedList<TAirAttack>();
		mAirAttackGround = new LinkedList<TAirAttack>();

		// Initialize the attack force priorities matrix
		mAttackForcePriorities = new HashMap<String, HashMap<String, Double>>();
		for (String armorType : Defs.ArmorType.TYPES) {
			mAttackForcePriorities.put(armorType, new HashMap<String, Double>());
		}

		// Initialize the armored building priorities list (map)
		mArmoredBuildingPriorities = new HashMap<String, Double>();
	}

	/**
	 * Update, gets called from AlIce's update method
	 */
	public void update() {

		updateFreeBuilders();

		// USE MESSAGES INSTEAD - e.g: 0 attack - to call attack();
		if (mState != State.ATTACK && mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(Defs.UnitGroup.ATTACK_FORCE).size() >= 10) {
			mAlIce.log(Level.FINER, "Attacking state!");
			mState = State.ATTACK;
		}
		switch (mState) {
		case ATTACK:
			attackUpdate();
			// mergeAttackTasks();
			scouting();
			generatePrioritiesAndDelegateBuilders();
			break;

		case DEFEND:

			generatePrioritiesAndDelegateBuilders();
			break;

		case IDLE:
			generatePrioritiesAndDelegateBuilders();
			scouting();
			break;

		case INITIAL_BUILD:
			if (!mInitBuildCommandSent) {
				mInitBuildCommandSent = true;
				mInitBuildSequence = new TInitialBuildSequence(mAlIce);
				mAlIce.getTaskHandler().run(mInitBuildSequence, this);
			}
			break;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.TaskObserver#onTaskFinished(alice.tasks.Task,
	 * alice.tasks.Task.Status)
	 */
	@Override
	public void onTaskFinished(Task task, Status status) {
		if (task == mInitBuildSequence && status == Status.COMPLETED_SUCCESSFULLY) {
			mState = State.IDLE;
			mAlIce.setInitialBuildFrame();

			// Set the initial income
			Defs.Priority.ENERGY_INCOME_START = mAlIce.getResourceIncome(Defs.Energy);
			Defs.Priority.ENERGY_STORAGE_START = mAlIce.getResourceStorage(Defs.Energy);
			Defs.Priority.METAL_INCOME_START = mAlIce.getResourceIncome(Defs.Metal);
			Defs.Priority.METAL_STORAGE_START = mAlIce.getResourceStorage(Defs.Metal);

		} else if (task == mScoutExtraction || task == mScoutRandom || task == mScoutRoaming || task instanceof TScout) {
			mIsScouting = false;
		} else if (task instanceof TGroundAttack) {
			// Remove the task from the ground or air units
			boolean removed = mGroundAttackGround.remove(task);
			if (!removed) {
				mGroundAttackAir.remove(task);
			}
		} else if (task instanceof TAirAttack) {
			// Remove the task from the ground or air units
			boolean removed = mAirAttackGround.remove(task);
			if (!removed) {
				mAirAttackAir.remove(task);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see alice.interfaces.IMessage#message(int, java.lang.String)
	 */
	@Override
	public void message(int player, String message) {
		LinkedList<String> arguments = new LinkedList<String>(Arrays.asList(message.split(" ")));

		// Only parse messages that are for us
		if (arguments.getFirst().equals(Integer.toString(mAlIce.getTeamId())) && arguments.size() > 1) {
			arguments.removeFirst();

			// // --- SCOUT ---
			// // Start scouting
			// if (arguments.getFirst().equals("scout")) {
			// arguments.removeFirst();
			//
			// LinkedList<TaskUnit> freeScouts =
			// mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(Defs.UnitGroup.SCOUT);
			// if (!freeScouts.isEmpty()) {
			// TaskUnit freeScout = freeScouts.getFirst();
			//
			// if (arguments.getFirst().equals("random")) {
			// mScoutRandom = new TScoutRandom(mAlIce, freeScout);
			// mAlIce.getTaskHandler().run(mScoutRandom, this, freeScout,
			// TaskPriority.MEDIUM);
			// } else if (arguments.getFirst().equals("roaming")) {
			// mScoutRoaming = new TScoutRoaming(mAlIce, freeScout);
			// mAlIce.getTaskHandler().run(mScoutRoaming, this, freeScout,
			// TaskPriority.MEDIUM);
			// } else if (arguments.getFirst().equals("extraction")) {
			// mScoutExtraction = new TScoutExtractionPoints(mAlIce, freeScout);
			// mAlIce.getTaskHandler().run(mScoutExtraction, this, freeScout,
			// TaskPriority.MEDIUM);
			// }
			// } else {
			// mAlIce.sendTextMsg("Error! There are no available scouts");
			// }
			// }
			// --- ATTACK ---
			// Tell the AI to start attacking
			if (arguments.getFirst().equals("attack")) {
				arguments.removeFirst();

				attack();
			}
			// --- GET ---
			// Command used for getting some specific information
			else if (arguments.getFirst().equals("get")) {
				arguments.removeFirst();
				if (arguments.size() == 0) {
					mAlIce.sendTextMsg("Error, invalid command!");
				}

				// ----- PRIORITY -----
				if (arguments.getFirst().equals("priority")) {
					arguments.removeFirst();

					final int NAME_LENGTH = 15;

					// If we specified a unit, search for that unit
					if (arguments.size() >= 1) {
						ListIterator<UnitPriority> it = mUnitPriorities.listIterator();
						while (it.hasNext()) {
							UnitPriority unit = it.next();
							if (unit.unitGroup.unitName.equals(arguments.getFirst())) {
								// int padLength = NAME_LENGTH -
								// (unit.unitDef.length() + 1);
								String paddedUnitDef = String.format("%1$-" + NAME_LENGTH + "s", unit.unitGroup.unitName + ":");
								mAlIce.log(Level.FINE, paddedUnitDef + unit.priority);
								mAlIce.sendTextMsg(unit.unitGroup.unitName + ": " + unit.priority);
								break;
							}
						}
					}
					// Else just print all the priorities
					else {
						for (UnitPriority unit : mUnitPriorities) {
							// int padLength = NAME_LENGTH -
							// (unit.unitDef.length() + 1);
							String paddedUnitDef = String.format("%1$-" + NAME_LENGTH + "s", unit.unitGroup.unitName + ":");
							mAlIce.log(Level.FINE, paddedUnitDef + unit.priority);
							mAlIce.sendTextMsg(paddedUnitDef + ": " + unit.priority);
						}
					}
				}
				// ----- UNIT INFO -----
				else if (arguments.getFirst().equals("unit")) {
					arguments.removeFirst();
					TaskUnitHandler taskUnitHandler = mAlIce.getTaskUnitHandler();

					if (arguments.isEmpty()) {
						// Only some basic unit information
						mAlIce.sendTextMsg("--- Unit Info ---");
						mAlIce.sendTextMsg("Anti-air units: " + taskUnitHandler.getNrAntiAirUnits());

						LinkedList<TaskUnit> freeAttackingUnits = mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(
								Defs.UnitGroup.ATTACK_FORCE);

						int freeAir = 0;
						int freeGround = 0;
						for (TaskUnit taskUnit : freeAttackingUnits) {
							if (taskUnit.getDef().isAbleToFly()) {
								freeAir++;
							} else {
								freeGround++;
							}
						}

						mAlIce.sendTextMsg("Free attack air units: " + freeAir);
						mAlIce.sendTextMsg("Free attack ground units: " + freeGround);
					} else {
						int unitId = Integer.parseInt(arguments.getFirst());
						TaskUnit taskUnit = taskUnitHandler.getTaskUnit(unitId);

						if (taskUnit != null) {
							// get the task info
							String taskInfo = taskUnit.getTaskInformation();
							mAlIce.sendTextMsg("--- Unit Info ---");
							mAlIce.sendTextMsg(taskInfo);
						}
					}
				}
				// ----- TIME -----
				else if (arguments.getFirst().equals("time")) {
					arguments.removeFirst();

					mAlIce.sendTextMsg("--- Game Time ---");
					mAlIce.sendTextMsg("Minutes:" + mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.MINUTES));
					mAlIce.sendTextMsg("Seconds: " + mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.SECONDS));
					mAlIce.sendTextMsg("Frames: " + mAlIce.getCurrentFrame());
				}
				// ----- EXTRACTION POINTS -----
				else if (arguments.getFirst().equals("extraction")) {
					arguments.removeFirst();
					ExtractionPointMap extMap = mAlIce.getExtractionPointMap();

					// Print extraction points
					mAlIce.sendTextMsg("--- Extraction Points ---");
					mAlIce.sendTextMsg("Free: " + extMap.getNumberOfExtractionPoints(Owner.NONE));
					mAlIce.sendTextMsg("Our: " + extMap.getNumberOfExtractionPoints(Owner.SELF));
					mAlIce.sendTextMsg("Enemy: " + extMap.getNumberOfExtractionPoints(Owner.ENEMY));
					mAlIce.sendTextMsg("All: " + extMap.getNumberOfExtractionPoints(null));
				}
				// ----- SIGHTED ENEMIES - INFO -----
				else if (arguments.getFirst().equals("enemies")) {
					arguments.removeFirst();

					SightedEnemies sightedEnemies = mAlIce.getSightedEnemies();

					if (!arguments.isEmpty()) {
						String groupType = arguments.getFirst();
						mAlIce.sendTextMsg("--- Sighted Enemies Info By Group: " + groupType + " ---");
						mAlIce.sendTextMsg("-- Armors --");
						double totalHealth = 0.0;
						for (String armorType : Defs.ArmorType.TYPES) {
							double armorHealth = sightedEnemies.getEnemyArmorTypeHealth(groupType, armorType);
							totalHealth += armorHealth;
							mAlIce.sendTextMsg(armorType + ": " + armorHealth);
						}
						mAlIce.sendTextMsg("Total Health: " + totalHealth);

						// Dps
						mAlIce.sendTextMsg("-- DPS --");
						double totalDps = 0.0;
						for (String damageType : Defs.DamageType.TYPES) {
							double dps = sightedEnemies.getEnemyDamageTypeDps(groupType, damageType);
							totalDps += dps;
							mAlIce.sendTextMsg(damageType + ": " + dps);
						}
						mAlIce.sendTextMsg("Total DPS: " + totalDps);
					} else {
						mAlIce.sendTextMsg("--- Sighted Enemies Info ---");
						mAlIce.sendTextMsg("-- Armors --");
						double totalHealth = 0.0;
						for (String armorType : Defs.ArmorType.TYPES) {
							double armorHealth = 0.0;
							for (String groupType : Defs.UnitGroup.TYPES) {
								armorHealth += sightedEnemies.getEnemyArmorTypeHealth(groupType, armorType);
							}
							totalHealth += armorHealth;
							mAlIce.sendTextMsg(armorType + ": " + armorHealth);
						}
						// And flying health
						mAlIce.sendTextMsg("Flying health: " + sightedEnemies.getFlyingHealth());
						mAlIce.sendTextMsg("Total Health: " + totalHealth);

						// Dps
						mAlIce.sendTextMsg("-- DPS --");
						double totalDps = 0.0;
						for (String damageType : Defs.DamageType.TYPES) {
							double dps = 0.0;
							for (String groupType : Defs.UnitGroup.TYPES) {
								dps += sightedEnemies.getEnemyDamageTypeDps(groupType, damageType);
							}
							totalDps += dps;
							mAlIce.sendTextMsg(damageType + ": " + dps);
						}
						mAlIce.sendTextMsg("Total DPS: " + totalDps);
					}
				}
				// ------------ TASK TYPES -----------
				else if (arguments.getFirst().equals("task")) {
					arguments.removeFirst();

					if (!arguments.isEmpty()) {
						int tasks = mAlIce.getTaskHandler().nrOfTasks(arguments.getFirst());
						mAlIce.sendTextMsg("--- Number Of Tasks ---");
						mAlIce.sendTextMsg(arguments.getFirst() + ": " + tasks);
					}
				} else {
					mAlIce.sendTextMsg("Error, invalid command!");
				}
			}
		}
	}

	/**
	 * States the General can be in
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public enum State {
		/**
		 * When we are attacking
		 */
		ATTACK,
		/**
		 * When we are defending
		 */
		DEFEND,
		/**
		 * When we are "idle"
		 */
		IDLE,
		/**
		 * State at start of the game
		 */
		INITIAL_BUILD
	}

	/**
	 * Updates the list of free builders
	 */
	private void updateFreeBuilders() {
		mFreeBuilders.clear();
		mFreeBuilders.addAll(mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(UnitGroup.BUILDER));
	}

	/**
	 * Runs attack() if its time
	 */
	private void attackUpdate() {

		double gameTime = mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.SECONDS);
		if (mLastAttackTime + Defs.ATTACK_UPDATE_TIME < gameTime &&
				mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(Defs.UnitGroup.ATTACK_FORCE).size() >= 10) {
			mAlIce.log(Level.FINE, "Time to update and order new attacks if we got the force for it!");
			mLastAttackTime = gameTime;
			attack();
		}
	}

	/**
	 * Method that runs the group attack task
	 */
	private void attack() {
		// Update the positions before we attack
		mAlIce.getSightedEnemies().update(mAlIce.getEnemyUnits());

		LinkedList<TaskUnit> airAttackForce = new LinkedList<TaskUnit>();
		LinkedList<TaskUnit> groundAttackForce = new LinkedList<TaskUnit>();
		LinkedList<TaskUnit> airAntiAirForce = new LinkedList<TaskUnit>();
		LinkedList<TaskUnit> groundAntiAirForce = new LinkedList<TaskUnit>();

		mAlIce.log(Level.FINE, "Total free attacking units: " +
				mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(Defs.UnitGroup.ATTACK_FORCE).size());
		// Sort the attack force into two groups, flying and ground
		for (TaskUnit taskUnit : mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(Defs.UnitGroup.ATTACK_FORCE)) {
			if (taskUnit.getUnit() != null) {
				// Flying, anti-air
				if (taskUnit.getUnitGroup().canAttackAir && taskUnit.getDef().isAbleToFly()) {
					airAntiAirForce.add(taskUnit);
				}
				// Ground, anti-air
				else if (taskUnit.getUnitGroup().canAttackAir) {
					groundAntiAirForce.add(taskUnit);
				}
				// Flying
				else if (taskUnit.getDef().isAbleToFly()) {
					airAttackForce.add(taskUnit);
				}
				// Ground
				else {
					groundAttackForce.add(taskUnit);
				}
			}
		}
		mAlIce.log(Level.FINE, "AirForce size: " + airAttackForce.size());
		mAlIce.log(Level.FINE, "GroudnForce size: " + groundAttackForce.size());

		// Air anti-air
		if (airAntiAirForce.size() >= Defs.ATTACK_FORCE_AIR_ANTI_AIR_MIN) {
			mAlIce.log(Level.FINE, "Attacking with air anti-air force)");

			// No healers are assigned to air units
			TAirAttack antiAirAttack = new TAirAttack(mAlIce, airAntiAirForce, null);
			mAirAttackAir.add(antiAirAttack);
			mAlIce.getTaskHandler().run(antiAirAttack, this, airAntiAirForce, TaskPriority.MEDIUM);
		}

		// Ground anti-air
		if (groundAntiAirForce.size() >= Defs.ATTACK_FORCE_GROUND_ANTI_AIR_MIN) {
			mAlIce.log(Level.FINE, "Attacking with ground anti-air force)");

			// No healers are assigned to ground anti-air attacks
			TAirAttack antiAirAttack = new TAirAttack(mAlIce, groundAntiAirForce, null);
			mAirAttackGround.add(antiAirAttack);
			mAlIce.getTaskHandler().run(antiAirAttack, this, groundAntiAirForce, TaskPriority.MEDIUM);
		}

		// Air ground-attack
		if (airAttackForce.size() >= Defs.ATTACK_FORCE_AIR_MIN) {
			mAlIce.log(Level.FINE, "Attacking with air force");

			// No healers are assigned to air units
			TGroundAttack groupAirAttack = new TGroundAttack(mAlIce, airAttackForce, null);
			mGroundAttackAir.add(groupAirAttack);
			mAlIce.getTaskHandler().run(groupAirAttack, this, airAttackForce, TaskPriority.MEDIUM);
		}

		// Ground ground-attack
		if (groundAttackForce.size() >= Defs.ATTACK_FORCE_GROUND_MIN) {
			mAlIce.log(Level.FINE, "Attacking with ground force");

			LinkedList<TaskUnit> healers = mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(Defs.UnitGroup.HEALER);
			TGroundAttack groupGroundAttack = new TGroundAttack(mAlIce, groundAttackForce, healers);
			mGroundAttackGround.add(groupGroundAttack);
			mAlIce.getTaskHandler().run(groupGroundAttack, this, groundAttackForce, TaskPriority.MEDIUM);
		}
	}

	/**
	 * Merges close attack tasks of the same type. Only merges two attack tasks
	 * in one method call.
	 */
	private void mergeAttackTasks() {
		if (mLastMergeTime + Defs.MERGE_ATTACK_GOALS_UPDATE_TIME < mAlIce.getGameTime(GameTimeTypes.SECONDS)) {
			mLastMergeTime = mAlIce.getGameTime(GameTimeTypes.SECONDS);

			// Ground Attack - Ground
			ListIterator<TGroundAttack> attackGroundItFirst = mGroundAttackGround.listIterator();
			while (attackGroundItFirst.hasNext()) {
				TGroundAttack firstAttack = attackGroundItFirst.next();
				ListIterator<TGroundAttack> attackGroundItSecond = mGroundAttackGround.listIterator(attackGroundItFirst
						.nextIndex());

				while (attackGroundItSecond.hasNext()) {
					TGroundAttack secondAttack = attackGroundItSecond.next();

					if (SpringHelper.getDist(firstAttack.getGroupPosition(), secondAttack.getGroupPosition()) < Defs.MERGE_CLOSE_RADIUS) {
						// We're close, check which task has least units
						TGroundAttack leastUnits;
						TGroundAttack mostUnits;
						if (firstAttack.getNrOfUnits() < secondAttack.getNrOfUnits()) {
							leastUnits = firstAttack;
							mostUnits = secondAttack;
							attackGroundItFirst.remove();
						} else {
							leastUnits = secondAttack;
							mostUnits = firstAttack;
							attackGroundItSecond.remove();
						}

						// Clear the one with least units and add them to the
						// task with most units
						LinkedList<TaskUnit> removedUnits = leastUnits.clear();

						// Bind the units
						for (TaskUnit taskUnit : removedUnits) {
							mAlIce.getTaskHandler().bindTask(mostUnits, taskUnit);
						}

						mostUnits.addUnits(removedUnits);

						return;
					}
				}
			}

			// Ground Attack - Air
			attackGroundItFirst = mGroundAttackAir.listIterator();
			while (attackGroundItFirst.hasNext()) {
				TGroundAttack firstAttack = attackGroundItFirst.next();
				ListIterator<TGroundAttack> attackItSecond = mGroundAttackAir.listIterator(attackGroundItFirst.nextIndex());

				while (attackItSecond.hasNext()) {
					TGroundAttack secondAttack = attackItSecond.next();

					if (SpringHelper.getDist(firstAttack.getGroupPosition(), secondAttack.getGroupPosition()) < Defs.MERGE_CLOSE_RADIUS) {
						// We're close, check which task has least units
						TGroundAttack leastUnits;
						TGroundAttack mostUnits;
						if (firstAttack.getNrOfUnits() < secondAttack.getNrOfUnits()) {
							leastUnits = firstAttack;
							mostUnits = secondAttack;
							attackGroundItFirst.remove();
						} else {
							leastUnits = secondAttack;
							mostUnits = firstAttack;
							attackItSecond.remove();
						}

						// Clear the one with least units and add them to the
						// task with most units
						LinkedList<TaskUnit> removedUnits = leastUnits.clear();

						// Bind the units
						for (TaskUnit taskUnit : removedUnits) {
							mAlIce.getTaskHandler().bindTask(mostUnits, taskUnit);
						}

						mostUnits.addUnits(removedUnits);

						return;
					}
				}
			}

			// Air Attack - Ground
			ListIterator<TAirAttack> attackAirItFirst = mAirAttackGround.listIterator();
			while (attackAirItFirst.hasNext()) {
				TAirAttack firstAttack = attackAirItFirst.next();
				ListIterator<TAirAttack> attackAirItSecond = mAirAttackGround.listIterator(attackAirItFirst.nextIndex());

				while (attackAirItSecond.hasNext()) {
					TAirAttack secondAttack = attackAirItSecond.next();

					if (SpringHelper.getDist(firstAttack.getGroupPosition(), secondAttack.getGroupPosition()) < Defs.MERGE_CLOSE_RADIUS) {
						// We're close, check which task has least units
						TAirAttack leastUnits;
						TAirAttack mostUnits;
						if (firstAttack.getNrOfUnits() < secondAttack.getNrOfUnits()) {
							leastUnits = firstAttack;
							mostUnits = secondAttack;
						} else {
							leastUnits = secondAttack;
							mostUnits = firstAttack;
							attackAirItSecond.remove();
						}

						// Clear the one with least units and add them to the
						// task with most units
						LinkedList<TaskUnit> removedUnits = leastUnits.clear();

						// Bind the units
						for (TaskUnit taskUnit : removedUnits) {
							mAlIce.getTaskHandler().bindTask(mostUnits, taskUnit);
						}

						mostUnits.addUnits(removedUnits);

						return;
					}
				}
			}

			// Air Attack - Air
			attackAirItFirst = mAirAttackAir.listIterator();
			while (attackAirItFirst.hasNext()) {
				TAirAttack firstAttack = attackAirItFirst.next();
				ListIterator<TAirAttack> attackAirItSecond = mAirAttackAir.listIterator(attackAirItFirst.nextIndex());

				while (attackAirItSecond.hasNext()) {
					TAirAttack secondAttack = attackAirItSecond.next();

					if (SpringHelper.getDist(firstAttack.getGroupPosition(), secondAttack.getGroupPosition()) < Defs.MERGE_CLOSE_RADIUS) {
						// We're close, check which task has least units
						TAirAttack leastUnits;
						TAirAttack mostUnits;
						if (firstAttack.getNrOfUnits() < secondAttack.getNrOfUnits()) {
							leastUnits = firstAttack;
							mostUnits = secondAttack;
							attackAirItFirst.remove();
						} else {
							leastUnits = secondAttack;
							mostUnits = firstAttack;
						}

						// Clear the one with least units and add them to the
						// task with most units
						LinkedList<TaskUnit> removedUnits = leastUnits.clear();

						// Bind the units
						for (TaskUnit taskUnit : removedUnits) {
							mAlIce.getTaskHandler().bindTask(mostUnits, taskUnit);
						}

						mostUnits.addUnits(removedUnits);

						return;
					}
				}
			}
		}
	}

	/**
	 * Handles scouting tasks and updating of the mScoutForce And makes the
	 * decision whether we need to scout or not
	 */
	private void scouting() {
		if (!mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(Defs.UnitGroup.SCOUT).isEmpty()) {
			if (!mIsScouting) {
				mScoutExtraction = new TScoutExtractionPoints(mAlIce, mAlIce.getTaskUnitHandler().getFreeUnitsByGroup(
						Defs.UnitGroup.SCOUT).getFirst());
				mAlIce.getTaskHandler().run(mScoutExtraction, this);
				mIsScouting = true;
				mcScouting++;
				mAlIce.log(Level.FINE, "Times scouted: " + mcScouting);
			}
			// } else {
			//
			// // Fail safe for scouting, if we are scouting and the last time
			// // since we finished a scouting task set mIsScouting to false
			// if (System.currentTimeMillis() - mLastScoutedTime > 2000) {
			// mAlIce.log(Level.FINE, "HERP DEPR, TIME TO SCOUT AGAIN LOLOO" +
			// (System.currentTimeMillis() - mLastScoutedTime));
			// mLastScoutedTime = System.currentTimeMillis();
			// mIsScouting = false;
			// }
			// }
		}
	}

	/**
	 * Wrapper for generating priorities and delegate builders
	 */
	private void generatePrioritiesAndDelegateBuilders() {
		double gameTime = mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.SECONDS);
		if (mLastPriorityTime + Defs.FREE_BUILDER_UPDATE_TIME < gameTime) {
			mLastPriorityTime = gameTime;

			generatePriorities();
			delegateBuildTaskToFreeBuilder();
		}
	}

	/**
	 * Generates the build priorities for all the units. Doesn't generate any
	 * priorities if we don't have a free builder.
	 */
	private void generatePriorities() {
		// Return if we don't have any free builders.
		if (mFreeBuilders.isEmpty()) {
			return;
		}
		generateAttackForcePriorities();
		generateArmoredBuildingPriorities();

		// Clear the priority vector iterate through all the units again
		Set<Entry<String, UnitGroup>> units = Defs.getAllUnits().entrySet();
		mUnitPriorities.clear();

		for (Entry<String, UnitGroup> unit : units) {
			// Only generate a priority for units we want to build.
			UnitGroup unitGroup = unit.getValue();
			if (unitGroup.build) {
				UnitPriority unitPriority = generatePriority(unitGroup);
				if (unitPriority != null && unitPriority.priority >= Priority.PRIORITY_MUST_HAVE) {
					mUnitPriorities.add(unitPriority);
				}
			}
		}

		sortPrioList();

		adjustBuilderPriorities();
	}

	/**
	 * Generate a priority for the specified unit definition (group).
	 * 
	 * @param unit
	 *            the unit we want to generate the priority for
	 * @return The unit priority
	 */
	private UnitPriority generatePriority(UnitGroup unit) {

		// Check if we only should build economics
		boolean onlyEconomics = Priority.ECONOMICS_ONLY_TIME > mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.MINUTES);

		UnitPriority unitPriority = new UnitPriority(unit);
		for (String resourceGroup : unit.groups) {
			// We only keep the highest priority for now
			double tempPriority = 0.0;

			if (!onlyEconomics) {
				if (resourceGroup.equals(UnitGroup.ATTACK_FORCE)) {
					tempPriority = generateAttackForcePriority(unit);
				} else if (resourceGroup.equals(UnitGroup.MOBILE_BUILDER)) {
					tempPriority = generateMobileBuilderPriority(unit.unitName);
				} else if (resourceGroup.equals(UnitGroup.FACTORY)) {
					tempPriority = generateFactoryPriority(unit.unitName);
				} else if (resourceGroup.equals(UnitGroup.ECONOMIC)) {
					tempPriority = generateEconomyPriority(unit.unitName);
				} else if (resourceGroup.equals(UnitGroup.BUILDING)) {
					tempPriority = generateBuildingPriority(unit.unitName);
				} else if (resourceGroup.equals(UnitGroup.ARMORED_BUILDING)) {
					tempPriority = generateArmoredBuildingPriority(unit);
				} else if (resourceGroup.equals(UnitGroup.SCOUT)) {
					tempPriority = generateScoutPriority(unit.unitName);
				} else if (resourceGroup.equals(UnitGroup.HEALER)) {
					tempPriority = generateHealerPriority(unit.unitName);
				}
			} else if (resourceGroup.equals(UnitGroup.ECONOMIC)) {
				tempPriority = generateEconomyPriority(unit.unitName);
			}

			if (tempPriority > unitPriority.priority) {
				unitPriority.priority = tempPriority;
			}
		}

		return unitPriority;
	}

	/**
	 * Generate the priority matrix for the attacking units
	 */
	private void generateAttackForcePriorities() {

		// Set priority for all weapon types
		HashMap<String, Double> tempDamageTypeHealth = new HashMap<String, Double>();
		List<String> allDamageTypes = Defs.DamageType.TYPES;
		List<String> allArmorTypes = Defs.ArmorType.TYPES;
		double maxHealth = Double.MIN_VALUE;
		SightedEnemies sightedEnemies = mAlIce.getSightedEnemies();
		for (String damageType : allDamageTypes) {
			double health = 0.0;
			for (String armorType : allArmorTypes) {
				health += (sightedEnemies.getEnemyArmorTypeHealth(Defs.UnitGroup.ATTACK_FORCE, armorType) + sightedEnemies
						.getEnemyArmorTypeHealth(Defs.UnitGroup.ARMORED_BUILDING, armorType)) /
						Defs.getDamageMultiplier(armorType, damageType);
			}
			if (maxHealth < health) {
				maxHealth = health;
			}
			tempDamageTypeHealth.put(damageType, health);
		}
		// Scale and put the damage type priorities in the real priority map
		double scaleFactor = Defs.Priority.DAMAGE_TYPE_PRIO_MAX - Defs.Priority.DAMAGE_TYPE_PRIO_MIN;
		Iterator<Entry<String, Double>> damageTypeIt = tempDamageTypeHealth.entrySet().iterator();
		while (damageTypeIt.hasNext()) {
			Entry<String, Double> currentDamageType = damageTypeIt.next();
			double scaledPriority = scaleFactor * currentDamageType.getValue() / maxHealth;
			scaledPriority = scaleFactor - scaledPriority + Defs.Priority.DAMAGE_TYPE_PRIO_MIN;

			for (String armorType : allArmorTypes) {
				HashMap<String, Double> damagePriority = mAttackForcePriorities.get(armorType);
				damagePriority.put(currentDamageType.getKey(), scaledPriority);
			}
		}

		// Set priority for all armor types
		HashMap<String, Double> tempArmorTypeDamage = new HashMap<String, Double>();
		double maxDamage = Double.MIN_VALUE;
		for (String armorType : allArmorTypes) {
			double dps = 1.0;
			for (String damageType : allDamageTypes) {
				dps += (sightedEnemies.getEnemyDamageTypeDps(Defs.UnitGroup.ARMORED_BUILDING, damageType) + sightedEnemies
						.getEnemyDamageTypeDps(Defs.UnitGroup.ATTACK_FORCE, damageType)) *
						Defs.getDamageMultiplier(armorType, damageType);
			}
			if (maxDamage < dps) {
				maxDamage = dps;
			}
			tempArmorTypeDamage.put(armorType, dps);
		}
		// Scale and put the armor type priorities in the real priority map
		scaleFactor = Defs.Priority.ARMOR_TYPE_PRIO_MAX - Defs.Priority.ARMOR_TYPE_PRIO_MIN;
		Iterator<Entry<String, Double>> armorTypeIt = tempArmorTypeDamage.entrySet().iterator();
		while (armorTypeIt.hasNext()) {
			Entry<String, Double> currentArmorType = armorTypeIt.next();
			double scaledPriority = scaleFactor * currentArmorType.getValue() / maxDamage;
			scaledPriority = scaleFactor - scaledPriority + Defs.Priority.ARMOR_TYPE_PRIO_MIN;
			HashMap<String, Double> armorPriority = mAttackForcePriorities.get(currentArmorType.getKey());
			for (String damageType : allDamageTypes) {
				double oldPriority = armorPriority.get(damageType);
				armorPriority.put(damageType, oldPriority * scaledPriority);
			}
		}
	}

	/**
	 * Generate the priority list for armored buildings. Since all armored
	 * buildings have armored_building as armor we don't need to check what
	 * armor is good for that armor.
	 */
	private void generateArmoredBuildingPriorities() {

		SightedEnemies sightedEnemies = mAlIce.getSightedEnemies();

		// Set priority for all weapon types
		HashMap<String, Double> tempDamageTypeHealth = new HashMap<String, Double>();
		List<String> allDamageTypes = Defs.DamageType.TYPES;
		List<String> allArmorTypes = Defs.ArmorType.TYPES;
		double maxHealth = Double.MIN_VALUE;
		// SightedEnemies sightedEnemies = mAlIce.getSightedEnemies();
		for (String damageType : allDamageTypes) {
			double health = 0.0;
			for (String armorType : allArmorTypes) {
				// NOTE: This differs from the attack force calculation. We
				// multiplies with the damage multiplier here...
				health += (sightedEnemies.getEnemyArmorTypeHealth(Defs.UnitGroup.ATTACK_FORCE, armorType)) *
						Defs.getDamageMultiplier(armorType, damageType);
			}
			if (maxHealth < health) {
				maxHealth = health;
			}
			tempDamageTypeHealth.put(damageType, health);
		}

		// Scale and add the priorities to the real priority map
		Iterator<Entry<String, Double>> damageTypeIt = tempDamageTypeHealth.entrySet().iterator();
		double prioMultiplier = Priority.ARMORED_BUILDING_PRIO / Priority.ARMORED_BUILDING_HEALTH_PER;
		while (damageTypeIt.hasNext()) {
			Entry<String, Double> currentDamageType = damageTypeIt.next();
			double scaledPriority = currentDamageType.getValue() * prioMultiplier;
			mArmoredBuildingPriorities.put(currentDamageType.getKey(), scaledPriority);
		}
	}

	/**
	 * Sorts the mUnitPriorities list
	 */
	private void sortPrioList() {
		Collections.sort(mUnitPriorities);
	}

	/**
	 * Generates the priority for creating economic units
	 * 
	 * @param unitDef
	 *            the unit definition (name) of the unit to generate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateEconomyPriority(String unitDef) {
		// Apply initial priority
		double priority = Priority.ECONOMY_MIN;

		// TaskUnitHandler used in almost all the ifs
		TaskUnitHandler taskUnitHandler = mAlIce.getTaskUnitHandler();

		if (unitDef.equals(Defs.FusionReactor.unitName)) {
			// We want our energy to increase ENERGY_INCREMENT_START +
			// linearIncrement each minute.
			double gameTime = mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.MINUTES);
			double currentIncome = mAlIce.getResourceIncome(Defs.Energy) - Priority.ENERGY_INCOME_START;

			// Only set priority if we are below the max for energy
			if (currentIncome < Priority.ENERGY_INCOME_MAX) {
				// Increase the income with the number of fusion reactors we're
				// currently building
				int cBuildingReactors = taskUnitHandler.getNrAllUnits(Defs.FusionReactor.unitName) -
						taskUnitHandler.getNrFinishedUnits(Defs.FusionReactor.unitName);
				currentIncome += cBuildingReactors * Priority.FUSION_INCOME;
				double increment = Priority.ENERGY_INCREMENT_EXP * gameTime + Priority.ENERGY_INCREMENT_START;

				// Clamp the increment to max
				if (increment > Priority.ENERGY_INCREMENT_MAX) {
					increment = Priority.ENERGY_INCREMENT_MAX;
				}
				double shouldHave = increment * gameTime;
				double diffIncome = shouldHave - currentIncome;

				priority += Priority.FUSION_PRIORITY * diffIncome / Priority.FUSION_INCOME;
			}

		} else if (unitDef.equals(Defs.MetalExtractor.unitName)) {
			// Get how many there are free
			ExtractionPointMap extMap = mAlIce.getExtractionPointMap();
			int freeExtractionPoints = extMap.getNumberOfExtractionPoints(ExtractionPointMap.Owner.NONE);
			if (freeExtractionPoints > 0) {
				// Get how many we extraction point own
				int ownedExtractionPoints = extMap.getNumberOfExtractionPoints(ExtractionPointMap.Owner.SELF);
				int totalExtractionPoints = extMap.getNumberOfExtractionPoints(null);

				double partOwned = (double) ownedExtractionPoints / (double) totalExtractionPoints;
				double multiplier = Priority.METAL_EXTRACTOR_PRIORITY;

				// If we own less than we should own. Add multiplier and
				// addition to the priority
				if (partOwned < Priority.EXTRACTION_POINT_SHOULD_OWN) {
					multiplier *= Priority.EXTRACTION_POINT_MULTIPLIER;
					priority += Priority.METAL_EXTRACTOR_LESS_OWN_ADDITION;
				}
				// Scale the ones that are left so we get a higher priority for
				// the number that's left.
				else {
					// Calculate how many that at least needs to be free
					double freePartExtraction = (double) freeExtractionPoints / (double) extMap.getNumberOfExtractionPoints(null);

					// If the number of free points or the part is higher than
					// the minimum we calculate a multiplier. Else set to 0.
					if (freeExtractionPoints >= Priority.EXTRACTION_MIN_FREE_NUMBER ||
							freePartExtraction >= Priority.EXTRACTION_MIN_FREE_PART) {
						multiplier *= (Priority.EXTRACTION_POINT_MULTIPLIER - 1) * freePartExtraction + 1;
					}
				}

				// We want our metal to increase METAL_INCREMENT each minute
				double gameTime = mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.MINUTES);
				double currentIncome = mAlIce.getResourceIncome(Defs.Metal) - Priority.METAL_INCOME_START;

				// Increase the income with the current metal extractors and
				// metal makers we're currently building (use a value of 1 for
				// each a.t.m.)
				int cBuildingIncome = taskUnitHandler.getNrAllUnits(Defs.MetalExtractor.unitName) -
						taskUnitHandler.getNrFinishedUnits(Defs.MetalExtractor.unitName) +
						taskUnitHandler.getNrAllUnits(Defs.MetalMaker.unitName) -
						taskUnitHandler.getNrFinishedUnits(Defs.MetalMaker.unitName);
				currentIncome += cBuildingIncome;

				double shouldHave = Priority.METAL_INCREMENT * gameTime;
				double diffIncome = shouldHave - currentIncome;

				priority += multiplier * diffIncome;
			}

		} else if (unitDef.equals(Defs.MetalMaker.unitName)) {
			boolean addPriority = false;

			// Get the number of free extraction points
			ExtractionPointMap extMap = mAlIce.getExtractionPointMap();

			// If there are some free extraction points, we might not build the
			// extraction point.
			if (extMap.getNumberOfExtractionPoints(Owner.NONE) > 0) {
				int ownedExtractionPoints = extMap.getNumberOfExtractionPoints(ExtractionPointMap.Owner.SELF);
				int totalExtractionPoints = extMap.getNumberOfExtractionPoints(null);

				double partOwned = (double) ownedExtractionPoints / (double) totalExtractionPoints;

				// If we own the part that we should own we can add a priority
				// But only if there are no free metal extractors
				if (partOwned >= Priority.EXTRACTION_POINT_SHOULD_OWN) {
					// Calculate how many that at least needs to be free
					double freeExtractionPoints = mAlIce.getExtractionPointMap().getNumberOfExtractionPoints(Owner.NONE);
					double freePartExtraction = (double) freeExtractionPoints / (double) extMap.getNumberOfExtractionPoints(null);

					// If there are less extraction points free that we should
					// need and the part is less than we need we should build a
					// metal maker.
					if (freeExtractionPoints <= Priority.EXTRACTION_MIN_FREE_NUMBER &&
							freePartExtraction <= Priority.EXTRACTION_MIN_FREE_PART) {
						addPriority = true;
					}
				}
			} else {
				addPriority = true;
			}

			if (addPriority) {
				// We want our metal to increase METAL_INCREMENT each minute
				double gameTime = mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.MINUTES);
				double currentIncome = mAlIce.getResourceIncome(Defs.Metal) - Priority.METAL_INCOME_START;

				// Increase the income with the current metal extractors and
				// metal makers we're currently building (use a value of 1 for
				// each a.t.m.)
				int cBuildingIncome = taskUnitHandler.getNrAllUnits(Defs.MetalExtractor.unitName) -
						taskUnitHandler.getNrFinishedUnits(Defs.MetalExtractor.unitName) +
						taskUnitHandler.getNrAllUnits(Defs.MetalMaker.unitName) -
						taskUnitHandler.getNrFinishedUnits(Defs.MetalMaker.unitName);
				currentIncome += cBuildingIncome;

				double shouldHave = Priority.METAL_INCREMENT * gameTime;
				double diffIncome = shouldHave - currentIncome;

				priority += diffIncome;
			}

			// Always decrease the metal maker priority
			priority -= Priority.METAL_MAKER_DECREMENT;

		} else if (unitDef.equals(Defs.Storage.unitName)) {
			double nrOfStorages = mAlIce.getTaskUnitHandler().getNrAllUnits(Defs.Storage.unitName);
			double currentEnergyIncome = mAlIce.getResourceIncome(Defs.Energy);
			double shouldHave = (currentEnergyIncome - Priority.ENERGY_INCOME_START) / Priority.ENERGY_PER_STORAGE;
			double diffStorage = shouldHave - nrOfStorages;

			priority += diffStorage * Priority.STORAGE_PRIORITY;
		}

		// Clamp the priority if it's to high
		if (priority > Priority.ECONOMY_MAX) {
			priority = Priority.ECONOMY_MAX;
		}

		return priority;
	}

	/**
	 * Generates the priority for creating force units
	 * 
	 * @param unitGroup
	 *            the UnitGroup of the attack force unit to calculate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateAttackForcePriority(UnitGroup unitGroup) {
		double priority = 0.0;
		HashMap<String, Double> armorPriority = mAttackForcePriorities.get(unitGroup.armorType);
		if (armorPriority != null) {
			Double tempPrio = armorPriority.get(unitGroup.damageType);
			if (tempPrio != null) {
				priority += tempPrio;

				// Decrement with the number of units
				priority -= mAlIce.getTaskUnitHandler().getNrAllUnits(unitGroup.unitName) * Priority.ATTACK_FORCE_UNIT_DECREMENT;
			}
		}

		// Add extra priority for anti-air
		if (unitGroup.canAttackAir) {
			double antiAirUnits = mAlIce.getTaskUnitHandler().getNrAntiAirUnits();
			double antiAirPrio = (mAlIce.getSightedEnemies().getFlyingHealth() / Priority.ANTI_AIR_PER_FLYING_HEALTH) -
					antiAirUnits;
			antiAirPrio *= Priority.ANTI_AIR_MULTIPLIER;

			priority += antiAirPrio;
		}

		return priority;
	}

	/**
	 * Generates the priority for creating armored building units
	 * 
	 * @param unitGroup
	 *            the UnitGroup of the attack force unit to calculate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateArmoredBuildingPriority(UnitGroup unitGroup) {
		double priority = Priority.ARMORED_BUILDING_PRIO_MIN;
		Double tempPrio = mArmoredBuildingPriorities.get(unitGroup.damageType);
		if (tempPrio != null) {
			priority += tempPrio;

			// Decrement with the number of units
			priority -= mAlIce.getTaskUnitHandler().getNrAllUnits(unitGroup.unitName);
		}

		// Add extra priority for anti-air
		if (unitGroup.canAttackAir) {
			double antiAirUnits = mAlIce.getTaskUnitHandler().getNrAntiAirUnits();
			double shouldHave = mAlIce.getSightedEnemies().getFlyingHealth() / Priority.ANTI_AIR_PER_FLYING_HEALTH;
			double antiAirPrio = shouldHave - antiAirUnits;

			// Extra priority for copperhead
			if (unitGroup.unitName.equals(Defs.Copperhead)) {
				antiAirPrio *= Priority.COPPERHEAD_PRIORITY;
			}

			priority += antiAirPrio;
		}

		if (unitGroup.unitName.equals(Defs.Executioner.unitName)) {
			priority += Priority.EXECUTIONER_EXTRA_PRIORITY;
		}

		if (priority > Priority.ARMORED_BUILDING_PRIO_MAX) {
			priority = Priority.ARMORED_BUILDING_PRIO_MAX;
		}

		return priority;
	}

	/**
	 * Generates the priority for mobile builders
	 * 
	 * @param unitDef
	 *            the unit definition (name) of the unit to generate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateMobileBuilderPriority(String unitDef) {
		double priority = Priority.BUILDER_PRIORITY_MIN;

		if (unitDef.equals(Defs.TheArchitectAir.unitName)) {
			double builders = mAlIce.getTaskUnitHandler().getNrAllUnits(unitDef);
			double gameTime = mAlIce.getGameTime(GameTimeTypes.MINUTES);
			double shouldHave = gameTime / Priority.BUILDER_MINUTES_PER_FLYING;
			double diffBuilders = shouldHave - builders;

			priority += Priority.BUILDER_FLYING_PRIORITY * diffBuilders;
		}

		return priority;
	}

	/**
	 * Generates the priority for the unit definition
	 * 
	 * @param unitDef
	 *            the unit definition (name) of the unit to generate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateFactoryPriority(String unitDef) {
		double priority = Priority.BUILDER_PRIORITY_MIN;

		double gameTime = mAlIce.getGameTime(GameTimeTypes.MINUTES);
		double factories = mAlIce.getTaskUnitHandler().getNrAllUnits(unitDef);
		double shouldHave = gameTime / Priority.FACTORY_MINUTES_PER;
		double diffBuilders = shouldHave - factories;

		priority += Priority.FACTORY_PRIORITY * diffBuilders;

		return priority;
	}

	/**
	 * Adjusts the builder priorities. If we got a unit with high priority that
	 * can't be built because we don't have that kind of builder we set that
	 * builder to FORCE_BUILD priority.
	 * 
	 * Also priorities builders that can build the top priority unit if we have
	 * enough metal
	 * 
	 * @note This method should be called after the priorities has been
	 *       generated.
	 */
	private void adjustBuilderPriorities() {
		UnitPriority unitPriority = mUnitPriorities.get(0);

		double gameTime = mAlIce.getGameTimeSinceInitialBuild(GameTimeTypes.SECONDS);
		boolean buildBuilder = false;
		// If we have much metal
		if (mAlIce.getResourceCurrent(Defs.Metal) >= Priority.BUILDER_NEW_MIN_METAL) {
			buildBuilder = true;
		} else {
			boolean canBuildHighestPriority = false;
			for (TaskUnit taskUnit : mAlIce.getTaskUnitHandler().getUnitsByGroup(Defs.UnitGroup.BUILDER)) {
				if (SpringHelper.canBuild(taskUnit.getDef(), unitPriority.unitGroup.unitName) != null) {
					canBuildHighestPriority = true;
					break;
				}
			}

			if (!canBuildHighestPriority) {
				buildBuilder = true;
			}
		}

		if (buildBuilder) {
			// Check so that we didn't build this builder too long ago
			Double lastForceBuild = mLastBuilderForceBuilds.get(unitPriority.unitGroup.unitName);

			if (lastForceBuild == null || lastForceBuild + Priority.BUILDER_WAIT_TIME < gameTime) {
				// Find the builder in the priority queue
				ListIterator<UnitPriority> it = mUnitPriorities.listIterator();
				boolean foundBuilder = false;
				while (it.hasNext() && !foundBuilder) {
					// Only check builders
					UnitPriority builderUnitPriority = it.next();
					if (builderUnitPriority.unitGroup.groups.getFirst().equals(Defs.UnitGroup.BUILDER) &&
							SpringHelper.canBuild(builderUnitPriority.unitGroup.unitDef, unitPriority.unitGroup.unitName) != null) {
						// set the priority for this unit to FORCE_BUILD
						builderUnitPriority.priority = Priority.FORCE_BUILD;
						foundBuilder = true;
					}
				}

				if (foundBuilder) {
					mLastBuilderForceBuilds.put(unitPriority.unitGroup.unitName, gameTime);

					// Sort the priority list again
					sortPrioList();
				}
			}
		}
	}

	/**
	 * Generates the priority for creating scout units
	 * 
	 * @param unitDef
	 *            the unit definition (name) of the unit to generate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateScoutPriority(String unitDef) {
		double priority = 0.0;

		if (mAlIce.getTaskUnitHandler().getUnitsByGroup(Defs.UnitGroup.SCOUT).size() < (int) Priority.SCOUTS_MIN) {
			priority = Priority.FORCE_BUILD;
		}

		return priority;
	}

	/**
	 * Generates the priority for creating building units
	 * 
	 * @param unitDef
	 *            the unit definition (name) of the unit to generate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateBuildingPriority(String unitDef) {
		double priority = 0.0;
		// TODO Generate building priority
		return priority;
	}

	/**
	 * Generates the priority for creating building units
	 * 
	 * @param unitDef
	 *            the unit definition (name) of the unit to generate the
	 *            priority for
	 * @return the current priority of the unit
	 */
	private double generateHealerPriority(String unitDef) {
		double priority = 0.0;
		// TODO_LOW generate healer priority
		return priority;
	}

	/**
	 * Assigns build task to the first free unit. They should try to build the
	 * unit with the highest priority that they can build.
	 * 
	 * @TODO implement the functionality to skip some free builders when we
	 *       don't have enough resources to build what we want to build.
	 */
	private void delegateBuildTaskToFreeBuilder() {
		if (mFreeBuilders.isEmpty()) {
			// No free builders, return
			return;
		}

		TaskUnit freeBuilder = null;
		ListIterator<TaskUnit> freeBuilderIt = mFreeBuilders.listIterator();
		UnitDef buildDef = null;

		// Iterate through free builders until we can build something
		do {
			freeBuilder = freeBuilderIt.next();
			ListIterator<UnitPriority> prioIt = mUnitPriorities.listIterator();

			// Get the buildDef with the highest priority that they can build.
			while (prioIt.hasNext() && buildDef == null) {
				UnitPriority unitPriority = prioIt.next();
				buildDef = SpringHelper.canBuild(freeBuilder.getDef(), unitPriority.unitGroup.unitName);
			}
		} while (buildDef == null && freeBuilderIt.hasNext());

		// Build the unit if we found one
		if (buildDef != null) {
			// Special case for metal extraction points
			if (buildDef.getName().equals(Defs.MetalExtractor.unitName)) {
				// Find closest free extraction point
				AIFloat3 buildPos = mAlIce.getExtractionPointMap().getClosestExtractorByOwner(freeBuilder.getUnitPos(),
						Owner.NONE);
				if (buildPos != null) {
					// Create build task on specified position
					mAlIce.getTaskHandler().run(new TBuildUnitByUnitOnPos(mAlIce, buildDef.getName(), freeBuilder, buildPos),
							null, freeBuilder, TaskPriority.MEDIUM);
				} else {
					mAlIce.log(Level.WARNING, "Could not find build position for metal extractor!");
				}
			} else {
				// Create build task
				mAlIce.getTaskHandler().run(new TBuildUnitByUnit(mAlIce, buildDef.getName(), freeBuilder), null, freeBuilder,
						TaskPriority.MEDIUM);
			}
		}
	}

	/**
	 * The different game time types.
	 * 
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public enum GameTimeTypes {
		/**
		 * Game time in seconds
		 */
		SECONDS,
		/**
		 * Game time in minutes
		 */
		MINUTES
	}

	/**
	 * The AI-Interface
	 */
	private AlIce mAlIce;
	/**
	 * All the ground attack with ground units tasks
	 */
	private LinkedList<TGroundAttack> mGroundAttackGround;
	/**
	 * All the ground attack with air units tasks
	 */
	private LinkedList<TGroundAttack> mGroundAttackAir;
	/**
	 * All the anti air attack with ground units tasks
	 */
	private LinkedList<TAirAttack> mAirAttackGround;
	/**
	 * All the ant air attack with air units tasks
	 */
	private LinkedList<TAirAttack> mAirAttackAir;
	/**
	 * Last time we checked for merging
	 */
	private double mLastMergeTime;
	/**
	 * The current state we are in
	 */
	private State mState;
	/**
	 * The initial build sequence
	 */
	private TInitialBuildSequence mInitBuildSequence;
	/**
	 * True if we sent TInitialBuildSequence to the TaskHandler
	 */
	private boolean mInitBuildCommandSent;
	/**
	 * List of free builders
	 */
	private LinkedList<TaskUnit> mFreeBuilders;
	/**
	 * Scout roaming positions task
	 */
	private TScoutRoaming mScoutRoaming;
	/**
	 * Scout random position task
	 */
	private TScoutRandom mScoutRandom;
	/**
	 * Scout all free extraction points
	 */
	private TScoutExtractionPoints mScoutExtraction;
	/**
	 * Nr of times i have scouted
	 */
	private int mcScouting;
	/**
	 * True if we are scouting
	 */
	private boolean mIsScouting;
	/**
	 * All the units' priorities. Should be sorted.
	 */
	private Vector<UnitPriority> mUnitPriorities;
	/**
	 * The attack force priorities matrix
	 */
	private HashMap<String, HashMap<String, Double>> mAttackForcePriorities;
	/**
	 * The last time we checked and sent attack command
	 */
	private double mLastAttackTime;
	/**
	 * The armored building priority list
	 */
	private HashMap<String, Double> mArmoredBuildingPriorities;
	/**
	 * The last time we set a builder to FORCE_BUILD
	 */
	private HashMap<String, Double> mLastBuilderForceBuilds;
	/**
	 * The last time we updated iterated through a builder
	 */
	private double mLastPriorityTime;

	/**
	 * Unit priority of the a unit.
	 * 
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	private class UnitPriority implements Comparable<UnitPriority> {
		/**
		 * Constructor that takes the UnitGroup. The default priority is 0.
		 * 
		 * @param unitGroup
		 *            the UnitGroup of the unit
		 */
		UnitPriority(UnitGroup unitGroup) {
			this.unitGroup = unitGroup;
			priority = 0;
		}

		/**
		 * The unit definition (name) of the unit.
		 */
		public UnitGroup unitGroup;
		/**
		 * The priority of the unit
		 */
		public double priority;

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(UnitPriority unitPriority) {
			if (unitPriority.priority < priority) {
				return -1;
			} else if (unitPriority.priority > priority) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
