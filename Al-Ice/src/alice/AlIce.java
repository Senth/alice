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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import alice.Defs.UnitGroup;
import alice.General.GameTimeTypes;
import alice.interfaces.ICommandFinished;
import alice.interfaces.IEnemyDamaged;
import alice.interfaces.IEnemyDestroyed;
import alice.interfaces.IEnemyEnterLOS;
import alice.interfaces.IEnemyEvents;
import alice.interfaces.IListener;
import alice.interfaces.IMessage;
import alice.interfaces.IUnitCreated;
import alice.interfaces.IUnitDestroyed;
import alice.interfaces.IUnitEvents;
import alice.interfaces.IUnitFinished;

import com.springrts.ai.AICommand;
import com.springrts.ai.AIFloat3;
import com.springrts.ai.command.SendTextMessageAICommand;
import com.springrts.ai.oo.AbstractOOAI;
import com.springrts.ai.oo.Info;
import com.springrts.ai.oo.Map;
import com.springrts.ai.oo.OOAI;
import com.springrts.ai.oo.OOAICallback;
import com.springrts.ai.oo.Resource;
import com.springrts.ai.oo.Unit;
import com.springrts.ai.oo.UnitDef;
import com.springrts.ai.oo.WeaponDef;
import com.springrts.ai.oo.WeaponMount;

/**
 * The main class for the AI Al Ice. This class handles all the events that are
 * sent by the Spring Engine.
 * 
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 * @author Tobias Hall <kazzoa@gmail.com>
 */
public class AlIce extends AbstractOOAI implements OOAI {

	/**
	 * Default zone (should be zero)
	 */
	private static final int DEFAULT_ZONE = 0;

	/**
	 * Constructor
	 * 
	 * @param teamId
	 *            the team id of the AI
	 * @param callback
	 *            callback method to Spring
	 */
	AlIce(int teamId, OOAICallback callback) {
		mTeamId = teamId;
		mCallback = callback;
		mCallback.getLog().log("Before get skirmish");

		mInfo = new Properties();
		Info info = callback.getSkirmishAI().getInfo();
		mCallback.getLog().log("Before properties");
		for (int i = 0; i < info.getSize(); i++) {
			String key = info.getKey(i);
			String value = info.getValue(i);
			mInfo.setProperty(key, value);
		}
		mCallback.getLog().log("After properties");
	}

	/**
	 * Logs a message into the file.
	 * 
	 * @param level
	 *            the level of severity.
	 * @param message
	 *            the message to log.
	 */
	public void log(Level level, String message) {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

		String strCallingMethod = null;
		// If the number of stack trace elements are two or more, then the
		// calling method should be the second element.
		int i = 0;
		while (strCallingMethod == null) {
			if (stackTraceElements.length > i + 1 && stackTraceElements[i].getMethodName().equals("log")) {
				strCallingMethod = stackTraceElements[i + 1].getClassName() + "::" + stackTraceElements[i + 1].getMethodName() +
						"()";
			}
			i++;
		}

		mLog.log(level, strCallingMethod + " | " + message);
	}

	/**
	 * Returns the reference to the TaskHandler
	 * 
	 * @return Reference to the TaskHandler
	 */
	public TaskHandler getTaskHandler() {
		return mTaskHandler;
	}

	/**
	 * Returns the reference to the ExtractionPointMap
	 * 
	 * @return Reference to the ExtractionPointMap
	 */
	public ExtractionPointMap getExtractionPointMap() {
		return mExtractionPointMap;
	}

	/**
	 * Returns the reference to SightedEnemies
	 * 
	 * @return Reference to SightedEnemies
	 */
	public SightedEnemies getSightedEnemies() {
		return mSightedEnemies;
	}

	/**
	 * Returns the current frame
	 * 
	 * @return The current frame
	 */
	public int getCurrentFrame() {
		return mGameTime.mcCurrentFrame;
	}

	/**
	 * Returns the previous frame
	 * 
	 * @return the previous frame
	 */
	public int getPreviousFrame() {
		return mGameTime.mcPreviousFrame;
	}

	/**
	 * Returns our teamId
	 * 
	 * @return our teamId
	 */
	public int getTeamId() {
		return mTeamId;
	}

	/**
	 * Adds an event listener if it already hasn't been added
	 * 
	 * @param eventListener
	 *            the event listener to add
	 */
	public void addEventListener(IListener eventListener) {

		// Search through the listeners to make sure we don't have it already.
		boolean found = false;
		for (IListener listener : mEventListeners) {
			if (listener == eventListener) {
				found = true;
				break;
			}
		}

		if (!found) {
			log(Level.FINER, "Adding new event listener: " + eventListener.toString());
			mEventListeners.add(eventListener);
		} else {
			log(Level.WARNING, "Tried to add a listener we already had");
		}
	}

	/**
	 * Removes an event listener
	 * 
	 * @param eventListener
	 *            the event listener to remove
	 */
	public void removeEventListener(IListener eventListener) {
		ListIterator<IListener> it = mEventListeners.listIterator();

		boolean found = false;
		while (it.hasNext() && !found) {
			if (it.next() == eventListener) {
				found = true;
				it.remove();
			}
		}

		if (!found) {
			log(Level.WARNING, "Could not find the specified listener");
		}
	}

	/**
	 * Tells the engine to execute a command
	 * 
	 * @param command
	 *            The command to be executed
	 * @return 0 if OK
	 */
	public int handleEngineCommand(AICommand command) {
		return mCallback.getEngine().handleCommand(com.springrts.ai.AICommandWrapper.COMMAND_TO_ID_ENGINE, -1, command);
	}

	/**
	 * Sends a message to the in game console window
	 * 
	 * @param msg
	 *            The message to be sent
	 * @return 0 if OK
	 */
	public int sendTextMsg(String msg) {
		SendTextMessageAICommand msgCmd = new SendTextMessageAICommand(msg, DEFAULT_ZONE);
		return handleEngineCommand(msgCmd);
	}

	/**
	 * Returns the map
	 * 
	 * @return The map
	 */
	public Map getMap() {
		return mCallback.getMap();
	}

	/**
	 * Returns the general
	 * 
	 * @return The general
	 */
	public General getGeneral() {
		return mGeneral;
	}

	/**
	 * Returns the elapsed time since initial build was completed
	 * 
	 * @param gameTimeType
	 *            type of time to return
	 * @return elapsed time since the game was started.
	 */
	public double getGameTimeSinceInitialBuild(GameTimeTypes gameTimeType) {
		double gameTime = 0.0;

		switch (gameTimeType) {
		case SECONDS:
			gameTime = mGameTime.mElapsedInitialBuildSeconds;
			break;

		case MINUTES:
			gameTime = mGameTime.mElapsedInitialBuildMinutes;
			break;
		}

		return gameTime;
	}

	/**
	 * Returns the elapsed time since the game started.
	 * 
	 * @param gameTimeType
	 *            type of time to return
	 * @return elapsed time since the game was started.
	 */
	public double getGameTime(GameTimeTypes gameTimeType) {
		double gameTime = 0.0;

		switch (gameTimeType) {
		case SECONDS:
			gameTime = mGameTime.mElapsedGameTimeSeconds;
			break;

		case MINUTES:
			gameTime = mGameTime.mElapsedGameTimeMinutes;
			break;
		}

		return gameTime;
	}

	/**
	 * Returns the delta time since the last frame.
	 * 
	 * @param gameTimeType
	 *            the type of time to return
	 * @return time elapsed since last frame
	 */
	public double getDeltaTime(GameTimeTypes gameTimeType) {
		double gameTime = 0.0;

		switch (gameTimeType) {
		case SECONDS:
			gameTime = mGameTime.mDeltaTimeSeconds;
			break;

		case MINUTES:
			gameTime = mGameTime.mDeltaTimeMinutes;
			break;
		}

		return gameTime;
	}

	/**
	 * Sets the frame when initial build was completed
	 */
	void setInitialBuildFrame() {
		mGameTime.mInitialBuildFrame = mGameTime.mcCurrentFrame;
	}

	/**
	 * Adds a units event listener
	 * 
	 * @param unitId
	 *            The id of the unit we are listening to
	 * @param listener
	 *            The event listener
	 */
	public void addUnitEventListener(int unitId, IUnitEvents listener) {
		LinkedList<IUnitEvents> unitListeners = mAddedUnitEventListeners.get(unitId);
		// If we don't have a listener for the unit already, add one
		if (unitListeners == null) {
			unitListeners = new LinkedList<IUnitEvents>();
			unitListeners.add(listener);
			mAddedUnitEventListeners.put(unitId, unitListeners);
		} else {
			unitListeners.add(listener);
		}
	}

	/**
	 * Removes a listener from the list
	 * 
	 * @param unitId
	 *            The id of the unit we want to remove a listener from
	 * @param listener
	 *            The listener to remove
	 */
	public void removeUnitEventListener(int unitId, IUnitEvents listener) {

		LinkedList<IUnitEvents> unitListeners = mRemovedUnitEventListeners.get(unitId);
		if (unitListeners == null) {
			unitListeners = new LinkedList<IUnitEvents>();
			mRemovedUnitEventListeners.put(unitId, unitListeners);
		}
		unitListeners.add(listener);
	}

	/**
	 * Adds an enemy event listener
	 * 
	 * @param enemyId
	 *            The id of the enemy we're listening to
	 * @param listener
	 *            The enemy event listener
	 */
	public void addEnemyEventListener(int enemyId, IEnemyEvents listener) {
		LinkedList<IEnemyEvents> unitListeners = mAddedEnemyEventListeners.get(enemyId);
		// If we don't have a listener for the unit already, add one
		if (unitListeners == null) {
			unitListeners = new LinkedList<IEnemyEvents>();
			unitListeners.add(listener);
			mAddedEnemyEventListeners.put(enemyId, unitListeners);
		} else {
			unitListeners.add(listener);
		}
	}

	/**
	 * Removes an enemy event listener if it exists in the list
	 * 
	 * @param enemyId
	 *            The id of the enemy we're listening to
	 * @param listener
	 *            The enemy event listener to remove
	 */
	public void removeEnemyEventListener(int enemyId, IEnemyEvents listener) {
		LinkedList<IEnemyEvents> unitListeners = mRemovedEnemyEventListeners.get(enemyId);
		if (unitListeners == null) {
			unitListeners = new LinkedList<IEnemyEvents>();
			mRemovedEnemyEventListeners.put(enemyId, unitListeners);
		}
		unitListeners.add(listener);
	}

	// -------------------------------------------------------------------------
	// ECONOMY METHODS
	// -------------------------------------------------------------------------

	/**
	 * Returns the current value of the specified resource
	 * 
	 * @param resourceName
	 *            the name of the resource
	 * @return the current value of the resource, 0.0 if not found
	 */
	public float getResourceCurrent(String resourceName) {
		Resource resource = mResources.get(resourceName);
		if (resource != null) {
			return mCallback.getEconomy().getCurrent(resource);
		}

		return 0.0f;
	}

	/**
	 * Returns the income of the specified resource
	 * 
	 * @param resourceName
	 *            the name of the resource
	 * @return the income of the resource, 0.0 if not found
	 */
	public float getResourceIncome(String resourceName) {
		Resource resource = mResources.get(resourceName);
		if (resource != null) {
			return mCallback.getEconomy().getIncome(resource);
		}

		return 0.0f;
	}

	/**
	 * Returns the storage of the specified resource
	 * 
	 * @param resourceName
	 *            the name of the resource
	 * @return the storage of the resource, 0.0 if not found
	 */
	public float getResourceStorage(String resourceName) {
		Resource resource = mResources.get(resourceName);
		if (resource != null) {
			return mCallback.getEconomy().getStorage(resource);
		}

		return 0;
	}

	/**
	 * Returns the usage of the specified resource
	 * 
	 * @param resourceName
	 *            the name of the resource
	 * @return the usage of the resource, 0.0 if not found
	 */
	public float getResourceUsage(String resourceName) {
		Resource resource = mResources.get(resourceName);
		if (resource != null) {
			return mCallback.getEconomy().getUsage(resource);
		}

		return 0;
	}

	/**
	 * Returns the specified resource
	 * 
	 * @param resourceName
	 *            the name of the resource
	 * @return the resource, null if not found
	 */
	public Resource getResource(String resourceName) {
		return mResources.get(resourceName);
	}

	/**
	 * Returns the TaskUnitHandler
	 * 
	 * @return the taskUnitHandler
	 */
	public TaskUnitHandler getTaskUnitHandler() {
		return mTaskUnitHandler;
	}

	/**
	 * Returns the closest enemy to the specified position. Uses a radius to
	 * only include those in the radius
	 * 
	 * @param position
	 *            the position to search from
	 * @param radius
	 *            the radius of the search
	 * @param flying
	 *            if the enemy should be flying or not
	 * @return the closest enemy, null if none was found
	 */
	public Unit getClosestEnemyInRange(AIFloat3 position, float radius, boolean flying) {
		List<Unit> enemies = mCallback.getEnemyUnitsIn(position, radius);

		Unit closestEnemy = null;
		float minDistance = Float.MAX_VALUE;
		for (Unit enemy : enemies) {
			boolean canFly = enemy.getDef().isAbleToFly();
			if ((canFly && flying) || (!canFly && !flying)) {
				float distance = SpringHelper.getDist(position, enemy.getPos());
				if (distance < minDistance && enemy != null) {
					minDistance = distance;
					closestEnemy = enemy;
				}
			}
		}

		return closestEnemy;
	}

	/**
	 * Returns a list of sighted enemeis and their positions so that we can
	 * update sighted enemies
	 * 
	 * @return List of sighted enemies
	 */
	public List<Unit> getEnemyUnits() {
		return mCallback.getEnemyUnits();
	}

	/**
	 * Returns true if the specified enemy unit exists
	 * 
	 * @param enemyId
	 *            the id of the enemy
	 * @return true if the specified enemy exists
	 */
	public boolean enemyExists(int enemyId) {
		for (Unit enemy : mCallback.getEnemyUnits()) {
			if (enemy.getUnitId() == enemyId) {
				return true;
			}
		}

		return false;
	}

	// -------------------------------------------------------------------------
	// OVERRIDDEN METHODS
	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#commandFinished(com.springrts.ai.oo.
	 * Unit, int, int)
	 */
	@Override
	public int commandFinished(Unit unit, int commandId, int commandTopicId) {
		try {
			for (IListener eventListener : mEventListeners) {
				if (eventListener instanceof ICommandFinished) {
					ICommandFinished listener = (ICommandFinished) eventListener;
					if (listener != null) {
						listener.commandFinished(unit, commandId, commandTopicId);
					}
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#enemyDamaged(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit, float, com.springrts.ai.AIFloat3,
	 * com.springrts.ai.oo.WeaponDef, boolean)
	 */
	@Override
	public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
		try {
			for (IListener eventListener : mEventListeners) {
				if (eventListener instanceof IEnemyDamaged) {
					IEnemyDamaged listener = (IEnemyDamaged) eventListener;
					if (listener != null) {
						listener.enemyDamaged(enemy, attacker, damage, dir, weaponDef, paralyzer);
					}
				}
			}

			// Iterate through the attacker's listeners (unit event listeners)
			if (attacker != null) {
				LinkedList<IUnitEvents> unitListeners = mUnitEventListeners.get(attacker.getUnitId());
				if (unitListeners != null) {
					for (IUnitEvents listener : unitListeners) {
						listener.enemyDamaged(enemy, attacker, damage, dir, weaponDef, paralyzer);
					}
				}
			}

			// Iterate through the enemy (enemy event listeners)
			LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(enemy.getUnitId());
			if (enemyListeners != null) {
				for (IEnemyEvents listener : enemyListeners) {
					listener.enemyDamaged(enemy, attacker, damage, dir, weaponDef, paralyzer);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#enemyDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public int enemyDestroyed(Unit enemy, Unit attacker) {
		log(Level.FINEST, "Id: " + enemy.getUnitId() + ", Def: " + enemy.getDef().getName());
		try {
			for (IListener eventListener : mEventListeners) {
				if (eventListener instanceof IEnemyDestroyed) {
					IEnemyDestroyed listener = (IEnemyDestroyed) eventListener;
					if (listener != null) {
						listener.enemyDestroyed(enemy, attacker);
					}
				}
			}

			// Iterate through the attacker's listeners (unit event listener)
			if (attacker != null) {
				LinkedList<IUnitEvents> unitListeners = mUnitEventListeners.get(attacker.getUnitId());
				if (unitListeners != null) {
					for (IUnitEvents listener : unitListeners) {
						listener.enemyDestroyed(enemy, attacker);
					}
				}
			}

			// Iterate through the enemy (enemy event listeners)
			LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(enemy.getUnitId());
			if (enemyListeners != null) {
				for (IEnemyEvents listener : enemyListeners) {
					listener.enemyDestroyed(enemy, attacker);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#enemyEnterLOS(com.springrts.ai.oo.Unit)
	 */
	@Override
	public int enemyEnterLOS(Unit enemy) {
		try {
			for (IListener eventListener : mEventListeners) {
				if (eventListener instanceof IEnemyEnterLOS) {
					IEnemyEnterLOS listener = (IEnemyEnterLOS) eventListener;
					if (listener != null) {
						listener.enemyEnterLOS(enemy);
					}
				}
			}

			// Iterate through the enemy (enemy event listeners)
			LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(enemy.getUnitId());
			if (enemyListeners != null) {
				for (IEnemyEvents listener : enemyListeners) {
					listener.enemyEnterLOS(enemy);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#enemyEnterRadar(com.springrts.ai.oo.
	 * Unit)
	 */
	@Override
	public int enemyEnterRadar(Unit enemy) {
		try {
			// Iterate through the enemy (enemy event listeners)
			LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(enemy.getUnitId());
			if (enemyListeners != null) {
				for (IEnemyEvents listener : enemyListeners) {
					listener.enemyEnterRadar(enemy);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#enemyLeaveLOS(com.springrts.ai.oo.Unit)
	 */
	@Override
	public int enemyLeaveLOS(Unit enemy) {
		try {
			// Iterate through the enemy (enemy event listeners)
			LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(enemy.getUnitId());
			if (enemyListeners != null) {
				for (IEnemyEvents listener : enemyListeners) {
					listener.enemyLeaveLOS(enemy);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#enemyLeaveRadar(com.springrts.ai.oo.
	 * Unit)
	 */
	@Override
	public int enemyLeaveRadar(Unit enemy) {
		try {
			// Iterate through the enemy (enemy event listeners)
			LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(enemy.getUnitId());
			if (enemyListeners != null) {
				for (IEnemyEvents listener : enemyListeners) {
					listener.enemyLeaveRadar(enemy);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#init(int,
	 * com.springrts.ai.oo.OOAICallback)
	 */
	@Override
	public int init(int teamId, OOAICallback callback) {
		int returnValue = -1;
		mCallback.getLog().log("Init()");

		try {
			mCallback = callback;
			mCallback.getLog().log("new callback");

			// Initialize logger
			try {
				mCallback.getLog().log("Try logger");
				FileHandler fileLogger = new FileHandler("log-AlIce-" + teamId + ".txt", false);
				fileLogger.setFormatter(new LogFormatter());
				fileLogger.setLevel(Level.ALL);
				mLog = Logger.getLogger("AlIce-" + mTeamId);
				mLog.addHandler(fileLogger);

				if (msDebugging) {
					mLog.setLevel(Level.ALL);
				} else {
					mLog.setLevel(Level.INFO);
				}
			} catch (Exception e) {
				mCallback.getLog().log("Al Ice: Unable to create log!" + e.getStackTrace());
				returnValue = -2;
			}

			log(Level.FINE, "Starting AI bot for team " + mTeamId);
			// -----------------------------
			// INITIALIZATION
			// -----------------------------

			// Initialize armor, and weapon-types
			List<UnitDef> unitDefs = mCallback.getUnitDefs();
			for (UnitDef unitDef : unitDefs) {
				UnitGroup unitGroup = Defs.getUnitGroup(unitDef.getName());
				if (unitGroup != null) {
					unitGroup.unitDef = unitDef;

					unitGroup.armorType = unitDef.getCustomParams().get("armortype");
					List<WeaponMount> weaponMounts = unitDef.getWeaponMounts();
					WeaponDef weaponDef = null;
					if (!weaponMounts.isEmpty()) {
						weaponDef = weaponMounts.get(0).getWeaponDef();
						unitGroup.damageType = weaponDef.getCustomParams().get("damagetype");
					}

					if (unitGroup.armorType == null) {
						unitGroup.armorType = "";
					}
					if (unitGroup.damageType == null) {
						unitGroup.damageType = "";
					}
				}

			}

			// Initialize counters
			mGameTime = new GameTimeContainer();

			// Initialize resources
			mResources = new HashMap<String, Resource>();
			List<Resource> resources = mCallback.getResources();
			for (Resource resource : resources) {
				mResources.put(resource.getName(), resource);
			}

			// Initialize listener handlers
			mEventListeners = new LinkedList<IListener>();
			mUnitEventListeners = new HashMap<Integer, LinkedList<IUnitEvents>>();
			mRemovedUnitEventListeners = new HashMap<Integer, LinkedList<IUnitEvents>>();
			mAddedUnitEventListeners = new HashMap<Integer, LinkedList<IUnitEvents>>();
			mEnemyEventListeners = new HashMap<Integer, LinkedList<IEnemyEvents>>();
			mAddedEnemyEventListeners = new HashMap<Integer, LinkedList<IEnemyEvents>>();
			mRemovedEnemyEventListeners = new HashMap<Integer, LinkedList<IEnemyEvents>>();

			// Initialize objects
			mExtractionPointMap = new ExtractionPointMap(this);
			mSightedEnemies = new SightedEnemies(this);
			mTaskHandler = new TaskHandler(this);
			mTaskUnitHandler = new TaskUnitHandler(this);
			mGeneral = new General(this);

			log(Level.FINE, "Done initializing");
			returnValue = 0;
			// -----------------------------
			// END OF - INITIALIZATION
			// -----------------------------
		} catch (Exception e) {
			returnValue = -1;
			log(Level.SEVERE, "ERROR" + e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return returnValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#load(java.lang.String)
	 */
	@Override
	public int load(String file) {
		try {
			// Don't implement
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#message(int, java.lang.String)
	 */
	@Override
	public int message(int player, String message) {
		try {
			for (IListener eventListener : mEventListeners) {
				if (eventListener instanceof IMessage) {
					IMessage listener = (IMessage) eventListener;
					if (listener != null) {
						listener.message(player, message);
					}
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#playerCommand(java.util.List,
	 * com.springrts.ai.AICommand, int)
	 */
	@Override
	public int playerCommand(List<Unit> units, AICommand command, int playerId) {
		try {
			// What is this?
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#release(int)
	 */
	@Override
	public int release(int reason) {
		try {
			// Release?
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#save(java.lang.String)
	 */
	@Override
	public int save(String file) {
		try {
			// Don't implement
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#seismicPing(com.springrts.ai.AIFloat3,
	 * float)
	 */
	@Override
	public int seismicPing(AIFloat3 pos, float strength) {
		try {
			// Implement this when ISeismicPing is implemented
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#unitCaptured(com.springrts.ai.oo.Unit,
	 * int, int)
	 */
	@Override
	public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
		try {
			// Don't implement this
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#unitCreated(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public int unitCreated(Unit unit, Unit builder) {
		try {

			if (unit.getTeam() == mTeamId && unit != null) {
				log(Level.FINEST, "Id: " + unit.getUnitId() + ", Def: " + unit.getDef().getName());
				for (IListener eventListener : mEventListeners) {
					if (eventListener instanceof IUnitCreated) {
						IUnitCreated listener = (IUnitCreated) eventListener;
						if (listener != null) {
							listener.unitCreated(unit, builder);
						}
					}
				}

				if (builder != null) {
					// Iterate through the builder's listeners
					LinkedList<IUnitEvents> listeners = mUnitEventListeners.get(builder.getUnitId());
					if (listeners != null) {
						for (IUnitEvents listener : listeners) {
							listener.unitCreated(unit, builder);
						}
					}
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#unitDamaged(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit, float, com.springrts.ai.AIFloat3,
	 * com.springrts.ai.oo.WeaponDef, boolean)
	 */
	@Override
	public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
		try {
			// Iterate through the unit (unit event listeners)
			LinkedList<IUnitEvents> unitListeners = mUnitEventListeners.get(unit.getUnitId());
			if (unitListeners != null) {
				for (IUnitEvents listener : unitListeners) {
					listener.unitDamaged(unit, attacker, damage, dir, weaponDef, paralyzer);
				}
			}

			// Iterate through the attacker (enemy event listeners)
			if (attacker != null) {
				LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(attacker.getUnitId());
				if (enemyListeners != null) {
					for (IEnemyEvents listener : enemyListeners) {
						listener.unitDamaged(unit, attacker, damage, dir, weaponDef, paralyzer);
					}
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#unitDestroyed(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.Unit)
	 */
	@Override
	public int unitDestroyed(Unit unit, Unit attacker) {
		try {
			if (unit.getTeam() == mTeamId && unit != null) {
				log(Level.FINEST, "Id: " + unit.getUnitId() + ", Def: " + unit.getDef().getName());

				for (IListener eventListener : mEventListeners) {
					if (eventListener instanceof IUnitDestroyed) {
						IUnitDestroyed listener = (IUnitDestroyed) eventListener;
						if (listener != null) {
							listener.unitDestroyed(unit, attacker);
						}
					}
				}

				// Iterate through the unit (unit event listener)
				LinkedList<IUnitEvents> listeners = mUnitEventListeners.get(unit.getUnitId());
				if (listeners != null) {
					for (IUnitEvents listener : listeners) {
						listener.unitDestroyed(unit, attacker);
					}
				}

				// Iterate through the attakcer (enemy event listeners)
				if (attacker != null) {
					LinkedList<IEnemyEvents> enemyListeners = mEnemyEventListeners.get(attacker.getUnitId());
					if (enemyListeners != null) {
						for (IEnemyEvents listener : enemyListeners) {
							listener.unitDestroyed(unit, attacker);
						}
					}
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString() + " Id: " + unit.getDef().getName() + "was killed by: " + attacker.getDef().getName());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#unitFinished(com.springrts.ai.oo.Unit)
	 */
	@Override
	public int unitFinished(Unit unit) {
		try {
			if (unit.getTeam() == mTeamId && unit != null) {
				log(Level.FINEST, "Id: " + unit.getUnitId() + ", Def: " + unit.getDef().getName());

				for (IListener eventListener : mEventListeners) {
					if (eventListener instanceof IUnitFinished) {
						IUnitFinished listener = (IUnitFinished) eventListener;
						if (listener != null) {
							listener.unitFinished(unit);
						}
					}
				}

				// Iterate through the unit's listeners
				LinkedList<IUnitEvents> listeners = mUnitEventListeners.get(unit.getUnitId());
				if (listeners != null) {
					for (IUnitEvents listener : listeners) {
						listener.unitFinished(unit);
					}
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#unitGiven(com.springrts.ai.oo.Unit,
	 * int, int)
	 */
	@Override
	public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {
		try {
			// Don't implement this
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#unitIdle(com.springrts.ai.oo.Unit)
	 */
	@Override
	public int unitIdle(Unit unit) {
		try {
			if (unit.getTeam() == mTeamId) {
				// Iterate through the unit's listeners
				LinkedList<IUnitEvents> listeners = mUnitEventListeners.get(unit.getUnitId());
				if (listeners != null) {
					for (IUnitEvents listener : listeners) {
						listener.unitIdle(unit);
					}
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#unitMoveFailed(com.springrts.ai.oo.Unit)
	 */
	@Override
	public int unitMoveFailed(Unit unit) {
		try {
			// Iterate through the unit's listeners
			LinkedList<IUnitEvents> listeners = mUnitEventListeners.get(unit.getUnitId());
			if (listeners != null) {
				for (IUnitEvents listener : listeners) {
					listener.unitMoveFailed(unit);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springrts.ai.oo.AbstractOOAI#update(int)
	 */
	@Override
	public int update(int frame) {
		try {
			mGameTime.update(frame);
			addRemoveUnitEventListeners();

			mGeneral.update();
			mTaskHandler.update();

			addRemoveUnitEventListeners();
			addRemoveEnemyEventListeners();
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.springrts.ai.oo.AbstractOOAI#weaponFired(com.springrts.ai.oo.Unit,
	 * com.springrts.ai.oo.WeaponDef)
	 */
	@Override
	public int weaponFired(Unit unit, WeaponDef weaponDef) {
		try {
			// Iterate through the unit's listeners
			LinkedList<IUnitEvents> listeners = mUnitEventListeners.get(unit.getUnitId());
			if (listeners != null) {
				for (IUnitEvents listener : listeners) {
					listener.weaponFired(unit, weaponDef);
				}
			}
		} catch (Exception e) {
			log(Level.SEVERE, e.toString());
			for (StackTraceElement element : e.getStackTrace()) {
				log(Level.SEVERE, element.toString());
			}
		}
		return 0;
	}

	/**
	 * If we are debugging or not.
	 */
	public static boolean msDebugging = true;

	// ------------------------------------------------------------------------
	// PROTECTED
	// ------------------------------------------------------------------------

	// ------------------------------------------------------------------------
	// PRIVATE
	// ------------------------------------------------------------------------

	/**
	 * Add and remove unit event listeners
	 */
	private void addRemoveUnitEventListeners() {
		// Remove the unit event listeners
		Iterator<Entry<Integer, LinkedList<IUnitEvents>>> it = mRemovedUnitEventListeners.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, LinkedList<IUnitEvents>> currentEntry = it.next();
			Integer unitId = currentEntry.getKey();
			LinkedList<IUnitEvents> removeListeners = currentEntry.getValue();

			// Get LinkedList with the unit's event listeners
			LinkedList<IUnitEvents> activeListeners = mUnitEventListeners.get(unitId);
			if (activeListeners != null) {
				for (IUnitEvents listener : removeListeners) {
					activeListeners.remove(listener);
				}
			} else {
				log(Level.WARNING, "Can't remove listener for unit: " + unitId);
			}

			it.remove();
		}

		// Add the unit event listeners
		it = mAddedUnitEventListeners.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, LinkedList<IUnitEvents>> currentEntry = it.next();
			Integer unitId = currentEntry.getKey();
			LinkedList<IUnitEvents> addListeners = currentEntry.getValue();

			// Get LinkedList with the unit's event listeners
			LinkedList<IUnitEvents> activeListeners = mUnitEventListeners.get(unitId);
			if (activeListeners == null) {
				activeListeners = new LinkedList<IUnitEvents>();
				mUnitEventListeners.put(unitId, activeListeners);
			}

			for (IUnitEvents listener : addListeners) {
				activeListeners.add(listener);
			}

			it.remove();
		}
	}

	/**
	 * Add and remove enemy event listeners
	 */
	private void addRemoveEnemyEventListeners() {
		// Remove the enemy event listeners
		Iterator<Entry<Integer, LinkedList<IEnemyEvents>>> it = mRemovedEnemyEventListeners.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, LinkedList<IEnemyEvents>> currentEntry = it.next();
			Integer unitId = currentEntry.getKey();
			LinkedList<IEnemyEvents> removeListeners = currentEntry.getValue();

			// Get LinkedList with the enemy's event listeners
			LinkedList<IEnemyEvents> activeListeners = mEnemyEventListeners.get(unitId);
			if (activeListeners != null) {
				for (IEnemyEvents listener : removeListeners) {
					activeListeners.remove(listener);
				}
			} else {
				log(Level.WARNING, "Can't remove listener for unit: " + unitId);
			}

			it.remove();
		}

		// Add the enemy event listeners
		it = mAddedEnemyEventListeners.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, LinkedList<IEnemyEvents>> currentEntry = it.next();
			Integer unitId = currentEntry.getKey();
			LinkedList<IEnemyEvents> addListeners = currentEntry.getValue();

			// Get LinkedList with the enemy's event listeners
			LinkedList<IEnemyEvents> activeListeners = mEnemyEventListeners.get(unitId);
			if (activeListeners == null) {
				activeListeners = new LinkedList<IEnemyEvents>();
				mEnemyEventListeners.put(unitId, activeListeners);
			}

			for (IEnemyEvents listener : addListeners) {
				activeListeners.add(listener);
			}

			it.remove();
		}
	}

	/**
	 * The log formatter for the log messages.
	 * 
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	private static class LogFormatter extends java.util.logging.Formatter {
		/**
		 * The date format of the logger
		 */
		private DateFormat mDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SSS");

		public String format(LogRecord record) {
			StringBuffer buffer = new StringBuffer();

			// Append date
			Date date = new Date(record.getMillis());
			buffer.append("[");
			buffer.append(mDateFormat.format(date));
			buffer.append(" ");

			// Append level name
			buffer.append(record.getLevel().getName());
			buffer.append("]: ");

			// Append formated message
			buffer.append(formatMessage(record));
			buffer.append("\n");

			return buffer.toString();
		}
	}

	/**
	 * Container for the game time
	 * 
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	private class GameTimeContainer {
		/**
		 * delta time in seconds
		 */
		public double mDeltaTimeSeconds;
		/**
		 * delta time in minutes
		 */
		public double mDeltaTimeMinutes;
		/**
		 * elapsed time in seconds since the game started
		 */
		public double mElapsedGameTimeSeconds;
		/**
		 * elapsed time in minutes since the game started
		 */
		public double mElapsedGameTimeMinutes;
		/**
		 * elapsed time in seconds since the initial build was completed
		 */
		public double mElapsedInitialBuildSeconds;
		/**
		 * elapsed time in minutes since the initial build was completed
		 */
		public double mElapsedInitialBuildMinutes;
		/**
		 * the current frame
		 */
		public int mcCurrentFrame;
		/**
		 * The previous frame
		 */
		public int mcPreviousFrame;
		/**
		 * The frame when the initial build was completed
		 */
		public int mInitialBuildFrame;

		/**
		 * Updates the time of the game. Should be called every update
		 * 
		 * @param currentFrame
		 *            the current frame of the game
		 */
		public void update(int currentFrame) {
			mcPreviousFrame = mcCurrentFrame;
			mcCurrentFrame = currentFrame;

			int elapsedInitialFrames = mcCurrentFrame - mInitialBuildFrame;
			mElapsedInitialBuildSeconds = elapsedInitialFrames * SpringHelper.SECONDS_PER_FRAME;
			mElapsedInitialBuildMinutes = elapsedInitialFrames * SpringHelper.MINUTES_PER_FRAME;
			mElapsedGameTimeSeconds = mcCurrentFrame * SpringHelper.SECONDS_PER_FRAME;
			mElapsedGameTimeMinutes = mcCurrentFrame * SpringHelper.MINUTES_PER_FRAME;

			int deltaFrames = mcCurrentFrame - mcPreviousFrame;
			mDeltaTimeSeconds = deltaFrames * SpringHelper.SECONDS_PER_FRAME;
			mDeltaTimeMinutes = deltaFrames * SpringHelper.MINUTES_PER_FRAME;
		}
	}

	/**
	 * The logger which the whole system uses. Cannot be directly accessed, but
	 * can be accessed through the log() function.
	 * 
	 * @see log
	 */
	private Logger mLog = null;

	/**
	 * The team id of the AI
	 */
	private int mTeamId;

	/**
	 * Spring Engine callback method
	 */
	private OOAICallback mCallback;

	/**
	 * Info properties of the AI. I.e. those that can be found in
	 * data/AIInfo.lua
	 */
	private Properties mInfo;
	/**
	 * The TaskHandler
	 */
	private TaskHandler mTaskHandler;
	/**
	 * All the listeners that listens to some events. The listener will listen
	 * to all the event interfaces that it implements that derive from
	 * IListener.
	 */
	private LinkedList<IListener> mEventListeners;
	/**
	 * HashMap of unit event listeners
	 */
	private HashMap<Integer, LinkedList<IUnitEvents>> mUnitEventListeners;
	/**
	 * Listeners to be removed from the unit listeners
	 */
	private HashMap<Integer, LinkedList<IUnitEvents>> mRemovedUnitEventListeners;
	/**
	 * Listeners to be added to the unit listeners
	 */
	private HashMap<Integer, LinkedList<IUnitEvents>> mAddedUnitEventListeners;
	/**
	 * Hashmap of enemy event listeners
	 */
	private HashMap<Integer, LinkedList<IEnemyEvents>> mEnemyEventListeners;
	/**
	 * Listeners to be removed from the enemy listeners
	 */
	private HashMap<Integer, LinkedList<IEnemyEvents>> mRemovedEnemyEventListeners;
	/**
	 * Listeners to be added to the enemy listeners
	 */
	private HashMap<Integer, LinkedList<IEnemyEvents>> mAddedEnemyEventListeners;
	/**
	 * The task unit handler
	 */
	private TaskUnitHandler mTaskUnitHandler;
	/**
	 * The General
	 */
	private General mGeneral;
	/**
	 * Stores all the time information about the game
	 */
	GameTimeContainer mGameTime;
	/**
	 * The Energy resource
	 */
	private HashMap<String, Resource> mResources;
	/**
	 * The Extraction point map
	 */
	private ExtractionPointMap mExtractionPointMap;
	/**
	 * All sighted enemies
	 */
	private SightedEnemies mSightedEnemies;

}
