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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.springrts.ai.oo.UnitDef;

/**
 * All the definitions used in the project.
 * 
 * @author Tobias Hall <kazzoa@gmail.com>
 * @author Matteus Magnusson <senth.wallace@gmail.com>
 */
public class Defs {
	/**
	 * Returns all the unit definitions of those used in this AI
	 * 
	 * @return all the unit definitions of those used in this AI
	 */
	public static final Map<String, UnitGroup> getAllUnits() {
		return mUnits;
	}

	/**
	 * Returns the UnitGroup of the specified unit name.
	 * 
	 * @param unitDef
	 *            the definition of the unit, i.e. the name.
	 * @return the UnitGroup of the specified unit. Null if not found
	 */
	public static final UnitGroup getUnitGroup(String unitDef) {
		return mUnits.get(unitDef);
	}

	/**
	 * Method to get the damage multiplier for a specific armor and damage type
	 * 
	 * @param armorType
	 *            The armor type
	 * @param damageType
	 *            The damage type
	 * @return The damage multiplier
	 */
	public static double getDamageMultiplier(String armorType, String damageType) {
		HashMap<String, Double> tempArmor = mDamageMultiplier.get(armorType);
		Double multiplier = null;
		if (tempArmor != null) {
			multiplier = tempArmor.get(damageType);
		}
		if (multiplier == null) {
			multiplier = 0.0;
		}
		return multiplier;
	}

	// -----------------------
	// Resources
	// -----------------------
	/**
	 * The energy resource
	 */
	public final static String Energy = "Energy";
	/**
	 * The metal resource
	 */
	public final static String Metal = "Metal";

	// -----------------------
	// Units
	// -----------------------
	/**
	 * None
	 */
	public final static UnitGroup NONE = new UnitGroup("none", "none", false, false);
	/**
	 * Commander - Armortype: Heavy :: Press D to activate EMP Defense ::
	 * Groups: builder,mobile_builder
	 */
	public final static UnitGroup TheOverseer = new UnitGroup("ecommander", UnitGroup.BUILDER + "," + UnitGroup.MOBILE_BUILDER,
			false, false);
	/**
	 * Airborne Engineer :: Armortype: Light :: Groups: builder,mobile_builder
	 */
	public final static UnitGroup TheArchitectAir = new UnitGroup("eairengineer", UnitGroup.BUILDER + "," +
			UnitGroup.MOBILE_BUILDER, true, false);
	/**
	 * Produces Units :: Groups: builder,factory
	 */
	public final static UnitGroup AircraftPlant = new UnitGroup("eairplant", UnitGroup.BUILDER + "," + UnitGroup.FACTORY, true,
			false);
	/**
	 * Stealth Bomber - Damagetype: Explosive :: Armortype: Medium :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Cardinal = new UnitGroup("ebomber", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Fighter - Damagetype: Light :: Armortype: Light :: Groups: attack_force
	 */
	public final static UnitGroup Coyote = new UnitGroup("efighter", UnitGroup.ATTACK_FORCE, true, true);
	/**
	 * MidKnight - HERP DERP
	 */
	public final static UnitGroup MidKnight = new UnitGroup("edrone", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Gunship Skirmisher - Damagetype: Medium :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Wildcat = new UnitGroup("egunship2", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Air Scout - Armortype: Light :: Groups: attack_force,scout
	 */
	public final static UnitGroup AirScout = new UnitGroup("escout", UnitGroup.SCOUT, true, false);
	/**
	 * Fast Armored Transport - Armortype: Heavy :: Groups: attack_force
	 */
	public final static UnitGroup Charter = new UnitGroup("etransport", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Engineer :: Armortype: Light :: Groups: builder,mobile_builder
	 */
	public final static UnitGroup TheArchitectGround = new UnitGroup("eallterrengineer", UnitGroup.BUILDER + "," +
			UnitGroup.MOBILE_BUILDER, true, false);
	/**
	 * Armored Skirmish Tank - Damagetype: Medium :: Armortype: Medium ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Sledge = new UnitGroup("eallterrheavy", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Raider - Damagetype: Piercing :: Armortype: Light :: Groups: attack_force
	 */
	public final static UnitGroup Recluse = new UnitGroup("eallterrlight", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Light Skirmish Tank - Damagetype: Light :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Basher = new UnitGroup("eallterrmed", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Anti-Swarm Tank - Damagetype: Light :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Mossberg = new UnitGroup("eallterrriot", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Produces Units :: Groups: builder,factory
	 */
	public final static UnitGroup AllTerrainFactory = new UnitGroup("eminifac", UnitGroup.BUILDER + "," + UnitGroup.FACTORY,
			true, false);
	/**
	 * Laser Support Artillery - Damagetype: Piercing :: Armortype: Light ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Splitter = new UnitGroup("eamphibrock", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Scout/Raider - Damagetype: Piercing :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Snake = new UnitGroup("eamphibbuggy", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Engineer - Armortype: Light :: Groups: builder,mobile_builder
	 */
	public final static UnitGroup TheDeveloper = new UnitGroup("eamphibengineer", UnitGroup.BUILDER + "," +
			UnitGroup.MOBILE_BUILDER, false, false);
	/**
	 * Produces Units :: Groups: builder,factory
	 */
	public final static UnitGroup AmphibiousTankFactory = new UnitGroup("eamphibfac",
			UnitGroup.BUILDER + "," + UnitGroup.FACTORY, false, false);
	/**
	 * Skirmish Tank - Damagetype: Medium :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Razor = new UnitGroup("eamphibmedtank", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Med tank hurr durr
	 * 
	 */
	public final static UnitGroup RazorHealer = new UnitGroup("eamphibdrone", UnitGroup.HEALER, false, false);
	/**
	 * Anti-Swarm Tank - Damagetype: Light :: Armortype: Medium :: Groups:
	 * attack_force
	 */

	public final static UnitGroup Dicer = new UnitGroup("eamphibriot", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Missile support tank - Damagetype: Medium :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Spitter = new UnitGroup("eamphibrock", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Anti-Amphibious Submarine :: Groups: attack_force
	 */
	public final static UnitGroup Lurker = new UnitGroup("esubmarine", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Dedicated Anti-Air Flak Turret - Damagetype: Explosive :: Armortype:
	 * Armored Building :: Groups: armored_building
	 */
	public final static UnitGroup Copperhead = new UnitGroup("eaaturret", UnitGroup.ARMORED_BUILDING, true, true);
	/**
	 * Organic Barricade - Regenerates Health :: Armortype: Armored Building ::
	 * Groups: building
	 */
	public final static UnitGroup Thorn = new UnitGroup("ebarricade", UnitGroup.BUILDING, false, false);
	/**
	 * Produces 10 Energy :: Groups: economic
	 */
	public final static UnitGroup FusionReactor = new UnitGroup("efusion2", UnitGroup.ECONOMIC, true, false);
	/**
	 * Produces 7 Energy :: Groups: economic
	 */
	public final static UnitGroup GeothermalPowerplant = new UnitGroup("egeothermal", UnitGroup.ECONOMIC, false, false);
	/**
	 * Heavy Plasma Battery - Damagetype: Medium :: Armored Building :: Groups:
	 * armored_building
	 */
	public final static UnitGroup Immolator = new UnitGroup("eheavyturret2", UnitGroup.ARMORED_BUILDING, true, false);
	/**
	 * Radar Jamming Tower :: Groups: building
	 */
	public final static UnitGroup RadarJammerTower = new UnitGroup("ejammer2", UnitGroup.ARMORED_BUILDING, true, false);
	/**
	 * Anti-Raid Defense Platform - Damagetype: Light :: Armortype: Armored
	 * Building :: Groups: armored_building
	 */
	public final static UnitGroup LightningRod = new UnitGroup("elightturret2", UnitGroup.ARMORED_BUILDING, true, true);
	/**
	 * Long Range Plasma Cannon - 50e Per Shot :: Groups: armored_building
	 */
	public final static UnitGroup Executioner = new UnitGroup("elrpc", UnitGroup.ARMORED_BUILDING, true, false);
	/**
	 * Converts Energy into Metal :: Groups: economic
	 */
	public final static UnitGroup MetalMaker = new UnitGroup("emaker", UnitGroup.ECONOMIC, true, false);
	/**
	 * Extracts Metal :: Groups: economic
	 */
	public final static UnitGroup MetalExtractor = new UnitGroup("emetalextractor", UnitGroup.ECONOMIC, true, false);
	/**
	 * Radar Tower - High LOS:: Groups: building
	 */
	public final static UnitGroup RadarTower = new UnitGroup("eradar2", UnitGroup.BUILDING, true, false);
	/**
	 * Shield Generator - Anti-Nuke/Anti-LRPC Facility :: Groups: building
	 */
	public final static UnitGroup Protector = new UnitGroup("eshieldgen", UnitGroup.BUILDING, true, false);
	/**
	 * Nuclear Missile Silo - Missile takes 4 minutes to build - Drains 50e
	 * while building :: Groups: building
	 */
	public final static UnitGroup Eradicator = new UnitGroup("esilo", UnitGroup.BUILDING, true, false);
	/**
	 * Produces 1 Energy :: Groups: economic
	 */
	public final static UnitGroup SolarCollector = new UnitGroup("esolar2", UnitGroup.ECONOMIC, false, false);
	/**
	 * Resource Storage - Stores 30e/30m :: Groups: economic
	 */
	public final static UnitGroup Storage = new UnitGroup("estorage", UnitGroup.ECONOMIC, true, false);
	/**
	 * Anti-Air Support Tank - Damagetype: Explosive :: Armortype: Medium ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Spewer = new UnitGroup("eaatank", UnitGroup.ATTACK_FORCE, true, true);
	/**
	 * Artillery Support Tank - Damagetype: Explosive :: Armortype: Light ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Lobster = new UnitGroup("eartytank", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Produces Units :: Groups: builder,factory
	 */
	public final static UnitGroup HovertankFactory = new UnitGroup("ebasefactory", UnitGroup.BUILDER + "," + UnitGroup.FACTORY,
			true, false);
	/**
	 * Hovering Bomb - Damagetype: Explosive :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Shellshock = new UnitGroup("ebomb", UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Produces Units :: Groups: builder,factory
	 */
	public final static UnitGroup CommandingFactory = new UnitGroup("ecommandfactory", UnitGroup.BUILDER + "," +
			UnitGroup.FACTORY, true, false);
	/**
	 * Engineer - Armortype: Light :: Groups: builder,mobile_unit
	 */
	public final static UnitGroup TheErector = new UnitGroup("eengineer5", UnitGroup.BUILDER + "," + UnitGroup.MOBILE_BUILDER,
			true, false);
	/**
	 * Armored Anti-Base Tank - Damagetype: Explosive :: Armortype: Heavy ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Fatso = new UnitGroup("efatso2", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Flamethrower Raider - Damagetype: Explosive :: Armortype: Light ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Pyromaniac = new UnitGroup("eflametank", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Armored Skirmish Tank - Damagetype: Medium :: Armortype: Medium ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Crusher = new UnitGroup("eheavytank3", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Raider - Damagetype: Piercing :: Armortype: Light :: Groups: attack_force
	 */
	public final static UnitGroup Kite = new UnitGroup("elighttank3", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Light Skirmish Tank - Damagetype: Medium :: Armortype: Light :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Bruiser = new UnitGroup("emediumtank3", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Missile Support Tank - Damagetype: Piercing :: Armortype: Light ::
	 * Groups: attack_force
	 */
	public final static UnitGroup Droplet = new UnitGroup("emissiletank", UnitGroup.ATTACK_FORCE, true, false);
	/**
	 * Field Medic :: Armortype: Light :: Groups: healer,attack_force
	 */
	public final static UnitGroup ORB = new UnitGroup("eorb", UnitGroup.HEALER + "," + UnitGroup.ATTACK_FORCE, false, false);
	/**
	 * Anti-Swarm Tank - Damagetype: Light :: Armortype: Heavy :: Groups:
	 * attack_force
	 */
	public final static UnitGroup Spas = new UnitGroup("eriottank2", UnitGroup.ATTACK_FORCE, true, false);

	// -----------------------------------------
	// Update times. How often we should update methods
	// and the likes
	// -----------------------------------------
	/**
	 * How often should we check if we should to attack. In seconds
	 */
	public final static double ATTACK_UPDATE_TIME = 0.2;
	/**
	 * How often we should iterate through free builders. In seconds
	 */
	public final static double FREE_BUILDER_UPDATE_TIME = 0.5;
	/**
	 * The number of seconds to split the load
	 */
	public final static double TASK_SPLIT_LOAD_TIME = 0.1;
	/**
	 * How often we should check if the enemy still exists. In seconds
	 */
	public final static double ENEMY_CHECK_TIME = 1.0;
	/**
	 * How often we should check for close attack goals to merge them
	 */
	public final static double MERGE_ATTACK_GOALS_UPDATE_TIME = 1.0;

	// -----------------------------------------
	// Attack force numbers
	// -----------------------------------------
	/**
	 * The minimum number of free ground units to start a ground attack force
	 */
	public final static int ATTACK_FORCE_GROUND_MIN = 10;
	/**
	 * The minimum number of free air units to start a air attack force (for
	 * ground)
	 */
	public final static int ATTACK_FORCE_AIR_MIN = 3;
	/**
	 * The minimum number of free anti-air ground units to start an anti-air
	 * attack
	 */
	public final static int ATTACK_FORCE_GROUND_ANTI_AIR_MIN = 3;
	/**
	 * The minimum number of free anti-air air units to start an anti-air attack
	 */
	public final static int ATTACK_FORCE_AIR_ANTI_AIR_MIN = 3;

	// -----------------------------------------
	// Various constants used in the project
	// -----------------------------------------
	/**
	 * Timelimit for last seen enemy in seconds
	 */
	public final static double LAST_SEEN_TIMELIMIT = 30.0;
	/**
	 * The distance we consider close enough to a way point
	 */
	public final static int CLOSE_TO_WP = 200;
	/**
	 * The radius we will look for enemies
	 */
	public final static int ATTACK_SEARCH_RADIUS = 1400;
	/**
	 * The width distance the different roaming positions should have between
	 * each other
	 */
	public final static float ROAMING_DISTANCE_WIDTH = 1200.0f;
	/**
	 * The height distance the different roaming positions should have between
	 * each other
	 */
	public final static float ROAMING_DISTANCE_HEIGHT = 1200.0f;
	/**
	 * The minimum distance between buildings in the default BuildUnitByUnit
	 * Task
	 */
	public final static int MIN_BUILD_SPACE = 30;
	/**
	 * Timeout for the AICommands
	 */
	public static final int TIME_OUT = 10000;
	/**
	 * The radius to search for close extraction points in the beginning of the
	 * game
	 */
	public static final float CLOSE_INITAL_EXTRACTION_POINTS = 500.0f;
	/**
	 * The time in seconds it takes until we treat an attack target as timed out
	 */
	public static final double ATTACK_TARGET_TIMEOUT = 30.0;
	/**
	 * How often we should check if we have stopped in TMoveCloseTo task. In
	 * seconds
	 */
	public static final double MOVE_CLOSE_TO_STOP_CHECK_TIME = 10.0;
	/**
	 * The close radius when merging attack tasks
	 */
	public static final float MERGE_CLOSE_RADIUS = 2000.0f;

	/**
	 * The number of max stops a unit can do until the task is treated as
	 * UNEXPECTED_ERROR
	 */
	public static final int MOVE_CLOSE_TO_MAX_RETRIES = 3;

	// -----------------------------------------
	// Various other things
	// -----------------------------------------

	/**
	 * Group information of a unit
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public static class UnitGroup {
		/**
		 * Constructor, adds unitDef and groups the unit belongs to
		 * 
		 * @param unitName
		 *            The unit definition (name)
		 * @param groupsString
		 *            The groups it belongs to, use "," to separate groups
		 * @param build
		 *            True if we should should build it
		 * @param canAttackAir
		 *            if the unit can attack air. TODO: Set it dynamically as we
		 *            do with the unitDef.
		 */
		UnitGroup(String unitName, String groupsString, boolean build, boolean canAttackAir) {
			this.unitName = unitName;
			this.build = build;
			this.groups = new LinkedList<String>();
			this.canAttackAir = canAttackAir;
			do {
				int lastIndex = groupsString.indexOf(",");

				// If we didn't find a comma, break
				if (lastIndex == -1) {
					break;
				}

				String subString = groupsString.substring(0, lastIndex);
				groups.add(subString);

				// Remove the first group and the comma.
				groupsString = groupsString.substring(lastIndex + 1);

			} while (true);
			// add the last group, i.e. the remaining groupString
			groups.add(groupsString);
		}

		/**
		 * True if the unit can attack air
		 */
		public boolean canAttackAir;
		/**
		 * The armor type of the unit
		 */
		public String armorType;
		/**
		 * The damage type of the unit
		 */
		public String damageType;
		/**
		 * Definition (name) of the unit
		 */
		public String unitName;
		/**
		 * The definition of the unit
		 */
		public UnitDef unitDef;
		/**
		 * Groups the unit belongs to
		 */
		public LinkedList<String> groups;
		/**
		 * If we shall take this unit into our calculation when building units
		 */
		public boolean build;

		/**
		 * The attacking units
		 */
		public static final String ATTACK_FORCE = "attack_force";
		/**
		 * Armored buildings
		 */
		public static final String ARMORED_BUILDING = "armored_buildings";
		/**
		 * The Builder group. If the unit belongs to this group it should also
		 * belong to either FACTORY or MOBILE_BUILDER.
		 */
		public static final String BUILDER = "builder";
		/**
		 * The Factory group, builds mobile units. If the unit belongs to this
		 * group it should also belong to the builder group
		 */
		public static final String FACTORY = "factory";
		/**
		 * The Mobile Builder group, builds buildings, armored buildings, and
		 * factories. If the unit belongs to this group it should also belong to
		 * the builder group.
		 */
		public static final String MOBILE_BUILDER = "mobile_builder";
		/**
		 * The Economic group
		 */
		public static final String ECONOMIC = "economic";
		/**
		 * The Scout group
		 */
		public static final String SCOUT = "scout";
		/**
		 * The Helper group
		 */
		public static final String BUILDING = "building";
		/**
		 * The Healers group
		 */
		public static final String HEALER = "healer";

		/**
		 * List with all the group types
		 */
		public static final LinkedList<String> TYPES;

		static {
			LinkedList<String> groupTypes = new LinkedList<String>();
			groupTypes.add(ATTACK_FORCE);
			groupTypes.add(ARMORED_BUILDING);
			groupTypes.add(BUILDER);
			groupTypes.add(FACTORY);
			groupTypes.add(MOBILE_BUILDER);
			groupTypes.add(ECONOMIC);
			groupTypes.add(SCOUT);
			groupTypes.add(BUILDING);
			groupTypes.add(HEALER);

			TYPES = groupTypes;
		}
	}

	/**
	 * Contains all the units
	 */
	private final static Map<String, UnitGroup> mUnits;

	static {
		// Initialize the unit vectors, only add those we want to use
		Map<String, UnitGroup> units = new HashMap<String, UnitGroup>();

		// ----- ATTACK FORCE -----
		units.put(Basher.unitName, Basher);
		units.put(Bruiser.unitName, Bruiser);
		units.put(Cardinal.unitName, Cardinal);
		units.put(Coyote.unitName, Coyote);
		units.put(Crusher.unitName, Crusher);
		units.put(Dicer.unitName, Dicer);
		units.put(Droplet.unitName, Droplet);
		units.put(Fatso.unitName, Fatso);
		units.put(Kite.unitName, Kite);
		units.put(Lobster.unitName, Lobster);
		units.put(Lurker.unitName, Lurker);
		units.put(MidKnight.unitName, MidKnight);
		units.put(Mossberg.unitName, Mossberg);
		units.put(Pyromaniac.unitName, Pyromaniac);
		units.put(Razor.unitName, Razor);
		units.put(Recluse.unitName, Recluse);
		units.put(Shellshock.unitName, Shellshock);
		units.put(Sledge.unitName, Sledge);
		units.put(Snake.unitName, Snake);
		units.put(Spas.unitName, Spas);
		units.put(Spewer.unitName, Spewer);
		units.put(Splitter.unitName, Splitter);
		units.put(Wildcat.unitName, Wildcat);

		// ----- SCOUT (also belongs to force) -----
		units.put(AirScout.unitName, AirScout);

		// ----- FACTORY (also belongs to builders) -----
		units.put(AircraftPlant.unitName, AircraftPlant);
		units.put(AllTerrainFactory.unitName, AllTerrainFactory);
		units.put(AmphibiousTankFactory.unitName, AmphibiousTankFactory);
		units.put(HovertankFactory.unitName, HovertankFactory);

		// ----- MOBILE BUILDERS (also belongs to builders) -----
		units.put(TheArchitectAir.unitName, TheArchitectAir);
		units.put(TheArchitectGround.unitName, TheArchitectGround);
		units.put(TheErector.unitName, TheErector);
		units.put(TheDeveloper.unitName, TheDeveloper);
		units.put(TheOverseer.unitName, TheOverseer);

		// ----- ARMORED BUILDING -----
		units.put(Copperhead.unitName, Copperhead);
		units.put(Executioner.unitName, Executioner);
		units.put(Immolator.unitName, Immolator);
		units.put(LightningRod.unitName, LightningRod);

		// ----- ECONOMIC -----
		units.put(FusionReactor.unitName, FusionReactor);
		units.put(GeothermalPowerplant.unitName, GeothermalPowerplant);
		units.put(MetalExtractor.unitName, MetalExtractor);
		units.put(MetalMaker.unitName, MetalMaker);
		units.put(SolarCollector.unitName, SolarCollector);
		units.put(Storage.unitName, Storage);

		// ----- BUILDING -----
		units.put(Eradicator.unitName, Eradicator);
		units.put(Protector.unitName, Protector);
		units.put(RadarJammerTower.unitName, RadarJammerTower);
		units.put(RadarTower.unitName, RadarTower);
		units.put(Thorn.unitName, Thorn); // organic barricade

		// ----- HEALER -----
		units.put(ORB.unitName, ORB); // Also force
		units.put(RazorHealer.unitName, RazorHealer);

		mUnits = units;
	}
	/**
	 * Damage multiplier
	 */
	private static HashMap<String, HashMap<String, Double>> mDamageMultiplier;

	static {
		mDamageMultiplier = new HashMap<String, HashMap<String, Double>>();

		// Heavy Armor
		HashMap<String, Double> tempArmor = new HashMap<String, Double>();
		tempArmor.put(Defs.DamageType.PIERCING, 1.0);
		tempArmor.put(Defs.DamageType.LIGHT, 0.5);
		tempArmor.put(Defs.DamageType.MEDIUM, 0.75);
		tempArmor.put(Defs.DamageType.EXPLOSIVE, 0.5);
		mDamageMultiplier.put(Defs.ArmorType.HEAVY, tempArmor);

		// Medium Armor
		tempArmor = new HashMap<String, Double>();
		tempArmor.put(Defs.DamageType.PIERCING, 1.0);
		tempArmor.put(Defs.DamageType.LIGHT, 0.5);
		tempArmor.put(Defs.DamageType.MEDIUM, 1.0);
		tempArmor.put(Defs.DamageType.EXPLOSIVE, 0.75);
		mDamageMultiplier.put(Defs.ArmorType.MEDIUM, tempArmor);

		// Light Armor
		tempArmor = new HashMap<String, Double>();
		tempArmor.put(Defs.DamageType.PIERCING, 1.0);
		tempArmor.put(Defs.DamageType.LIGHT, 2.0);
		tempArmor.put(Defs.DamageType.MEDIUM, 1.0);
		tempArmor.put(Defs.DamageType.EXPLOSIVE, 1.0);
		mDamageMultiplier.put(Defs.ArmorType.LIGHT, tempArmor);

		// Ecological
		tempArmor = new HashMap<String, Double>();
		tempArmor.put(Defs.DamageType.PIERCING, 2.0);
		tempArmor.put(Defs.DamageType.LIGHT, 0.1);
		tempArmor.put(Defs.DamageType.MEDIUM, 0.5);
		tempArmor.put(Defs.DamageType.EXPLOSIVE, 2.0);
		mDamageMultiplier.put(Defs.ArmorType.ECOLOGICAL, tempArmor);

		// Buildings
		tempArmor = new HashMap<String, Double>();
		tempArmor.put(Defs.DamageType.PIERCING, 1.5);
		tempArmor.put(Defs.DamageType.LIGHT, 0.1);
		tempArmor.put(Defs.DamageType.MEDIUM, 0.5);
		tempArmor.put(Defs.DamageType.EXPLOSIVE, 2.0);
		mDamageMultiplier.put(Defs.ArmorType.BUILDING, tempArmor);

		// Armored Buildings
		tempArmor = new HashMap<String, Double>();
		tempArmor.put(Defs.DamageType.PIERCING, 1.5);
		tempArmor.put(Defs.DamageType.LIGHT, 0.1);
		tempArmor.put(Defs.DamageType.MEDIUM, 0.5);
		tempArmor.put(Defs.DamageType.EXPLOSIVE, 0.75);
		mDamageMultiplier.put(Defs.ArmorType.ARMORED_BUILDING, tempArmor);
	}

	/**
	 * Contains all the different armor types of the units.
	 * 
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 * @author Tobias Hall <kazzoa@gmail.com>
	 */
	public static class ArmorType {
		/**
		 * Heavy armor. Piercing: 1.0, Light: 0.5, Medium: 0.75, Explosive: 0.5
		 */
		public static final String HEAVY = "heavyarmor";
		/**
		 * Medium armor. Piercing: 1.0, Light: 0.5, Medium: 1.0, Explosive: 0.75
		 */
		public static final String MEDIUM = "mediumarmor";
		/**
		 * Light armor. Piercing: 1.0, Light: 2.0, Medium: 1.0, Explosive: 1.0
		 */
		public static final String LIGHT = "lightarmor";
		/**
		 * Ecological. Piercing: 2.0, Light: 0.1, Medium: 0.5, Explosive: 2.0
		 */
		public static final String ECOLOGICAL = "eco";
		/**
		 * Buildings. Piercing: 1.5, Light: 0.1, Medium: 0.2, Explosive: 2.0
		 */
		public static final String BUILDING = "building";
		/**
		 * Armored Buildings. Piercing: 1.5, Light: 0.1, Medium: 0.2, Explosive:
		 * 0.75
		 */
		public static final String ARMORED_BUILDING = "armoredbuilding";

		/**
		 * All the different group types
		 */
		public static final List<String> TYPES;

		static {
			LinkedList<String> tempArmor = new LinkedList<String>();
			tempArmor.add(HEAVY);
			tempArmor.add(MEDIUM);
			tempArmor.add(LIGHT);
			tempArmor.add(ECOLOGICAL);
			tempArmor.add(BUILDING);
			tempArmor.add(ARMORED_BUILDING);
			TYPES = tempArmor;
		}
	}

	/**
	 * Contains all the different damage types of the units' weapons
	 * 
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 * @author Tobias Hall <kazzoa@gmail.com>
	 */
	public static class DamageType {
		/**
		 * Piercing. Heavy Armor: 1.0, Medium Armor: 1.0, Light Armor: 1.0,
		 * Ecological: 2.0, Building: 1.5, Armored Building: 1.5
		 */
		public static final String PIERCING = "beam";
		/**
		 * Light. Heavy Armor: 0.5, Medium Armor: 0.5, Light Armor: 2.0,
		 * Ecological: 0.1, Building: 0.1, Armored Building: 0.1
		 */
		public static final String LIGHT = "light";
		/**
		 * Medium. Heavy Armor: 0.75, Medium Armor: 1.0, Light Armor: 1.0,
		 * Ecological: 0.5, Building: 0.5, Armored Building: 0.5
		 */
		public static final String MEDIUM = "medium";
		/**
		 * Explosive. Heavy Armor: 0.5, Medium Armor: 0.75, Light Armor: 1.0,
		 * Ecological: 2.0, Building: 2.0, Armored Building: 0.75
		 */
		public static final String EXPLOSIVE = "explosive";
		/**
		 * All the different damage types.
		 */
		public static final List<String> TYPES;

		static {
			LinkedList<String> tempDamage = new LinkedList<String>();
			tempDamage.add(PIERCING);
			tempDamage.add(MEDIUM);
			tempDamage.add(LIGHT);
			tempDamage.add(EXPLOSIVE);
			TYPES = tempDamage;
		}
	}

	/**
	 * Various information of Priority calculating for building units. All
	 * values should be double to keep a consistency.
	 * 
	 * @author Tobias Hall <kazzoa@gmail.com>
	 * @author Matteus Magnusson <senth.wallace@gmail.com>
	 */
	public static class Priority {
		// -------------------------------------------------------
		// RESOURCE Specific - Used for balancing
		// -------------------------------------------------------
		/**
		 * The multiplier for the Economy Resource group
		 */
		public static final double ECONOMY_MULTIPLIER = 1.0;
		/**
		 * Minimum value for Economy. This value always gets added to the value.
		 */
		public static final double ECONOMY_MIN = 150.0;
		/**
		 * Max value for Economy
		 */
		public static final double ECONOMY_MAX = 800.0;
		/**
		 * The minimum priority to include it in the priority list
		 */
		public static final double PRIORITY_MUST_HAVE = 50.0;

		// -------------------------------------------------------
		// ECONOMIC - Priorities concerning the Economic group
		// -------------------------------------------------------
		/**
		 * Energy increment each second. I.e. our income should increase with
		 * this each second. Only affects the building priority after the
		 * initial build
		 */
		public static final double ENERGY_INCREMENT_START = 2.0;
		/**
		 * Linear Energy increment. I.e. the value 1 then and
		 * ENERGY_START_INCREMENT is 10 then we need 11 more after the first
		 * minute, 12 after the second.
		 */
		public static final double ENERGY_INCREMENT_EXP = 0.75;
		/**
		 * The maximum increment of energy. It will never go over this
		 */
		public static final double ENERGY_INCREMENT_MAX = 15.0;
		/**
		 * Our initial Energy income. This value gets set after our initial
		 * build. Although it's not final it should be treated as that since it
		 * will only be set once.
		 */
		public static double ENERGY_INCOME_START = 0.0;
		/**
		 * Energy income max, the maximum number of energy
		 */
		public static double ENERGY_INCOME_MAX = 300.0;
		/**
		 * Our initial Energy storage. This value gets set after our initial
		 * build. Although it's not final it should be treated as that since it
		 * will only be set once.
		 */
		public static double ENERGY_STORAGE_START = 0.0;
		/**
		 * Metal increment each minute. I.e. our income should increase with
		 * this each minute
		 */
		public static final double METAL_INCREMENT = 1.5;
		/**
		 * Our initial Metal income. This value get's set after our initial
		 * build. Although it's not final it should be treated as that since it
		 * will only be set once.
		 */
		public static double METAL_INCOME_START = 0.0;
		/**
		 * Our initial Metal storage. This value gets set after our initial
		 * build. Although it's not final it should be treated as that since it
		 * will only be set once.
		 */
		public static double METAL_STORAGE_START = 0.0;
		/**
		 * Fusion reactor default priority
		 */
		public static final double FUSION_PRIORITY = 10.0;
		/**
		 * Fusion generation power. The income a single fusion reactor
		 * generates.
		 */
		public static final double FUSION_INCOME = 10.0;
		/**
		 * The number of metal extraction points we need to own before not using
		 * the metal extractor multiplier. [0.0-1.0]
		 */
		public static final double EXTRACTION_POINT_SHOULD_OWN = 0.4;
		/**
		 * The priority multiplier for Metal Extractor when we own less then
		 * EXTRACTION_POINT_SHOULD_OWN.
		 */
		public static final double EXTRACTION_POINT_MULTIPLIER = 4.0;
		/**
		 * The minimum part of the extraction points that should be free. If
		 * it's less than this and less than EXTRACTION_MIN_FOR_BUILD we will
		 * not build any metal extractors
		 */
		public static final double EXTRACTION_MIN_FREE_PART = 0.1;
		/**
		 * The minimum number of extraction points that should be free. If it's
		 * less than this and less than EXTRACTION_POINT_FREE_PART we will not
		 * build any metal extractors
		 */
		public static final double EXTRACTION_MIN_FREE_NUMBER = 5.0;
		/**
		 * Metal extractor multiplier
		 */
		public static final double METAL_EXTRACTOR_PRIORITY = 10.0;
		/**
		 * Adds this priority to metal extractor when we own less than the part
		 * we should own
		 */
		public static final double METAL_EXTRACTOR_LESS_OWN_ADDITION = 100.0;
		/**
		 * Decrement of the metal maker prioirity
		 */
		public static final double METAL_MAKER_DECREMENT = 40.0;
		/**
		 * How much energy per storage we need to build it.
		 */
		public static final double ENERGY_PER_STORAGE = 30.0f;
		/**
		 * The storage priority (when we have ENERGY_PER_STORAGE).
		 */
		public static final double STORAGE_PRIORITY = 5.0;
		/**
		 * The time (in game time minutes, i.e. after initial build) that we
		 * only will build economic units.
		 */
		public static final double ECONOMICS_ONLY_TIME = 0.75;

		// -------------------------------------------------------
		// ATTACK FORCE - Priorities concerning the ATTACK FORCE
		// -------------------------------------------------------
		/**
		 * The minimum damage priority
		 */
		public static final double DAMAGE_TYPE_PRIO_MIN = 15.0;
		/**
		 * THe maximum damage priority
		 */
		public static final double DAMAGE_TYPE_PRIO_MAX = 25.0;
		/**
		 * The minimum armor priority
		 */
		public static final double ARMOR_TYPE_PRIO_MIN = 10.0;
		/**
		 * THe maximum armor priority
		 */
		public static final double ARMOR_TYPE_PRIO_MAX = 20.0;
		/**
		 * The decrement of unit priority for each unit that we already have
		 */
		public static final double ATTACK_FORCE_UNIT_DECREMENT = 4.0;
		/**
		 * Number per anti-air units per health. Also used for armored buildings
		 */
		public static final double ANTI_AIR_PER_FLYING_HEALTH = 50.0;
		/**
		 * Anti-air multiplier priority, only used for attack force
		 */
		public static final double ANTI_AIR_MULTIPLIER = 10.0;

		// -------------------------------------------------------
		// ARMORED BUILDINGS
		// -------------------------------------------------------
		/**
		 * The minimum priority for armored buildings
		 */
		public static final double ARMORED_BUILDING_PRIO_MIN = 100.0;
		/**
		 * The maximum priority for armored buildings
		 */
		public static final double ARMORED_BUILDING_PRIO_MAX = 400.0;
		/**
		 * How many defending buildings we should have per health they have in
		 * the group
		 */
		public static final double ARMORED_BUILDING_HEALTH_PER = 2000.0;
		/**
		 * The multiplying priority for each armored building.
		 */
		public static final double ARMORED_BUILDING_PRIO = 50.0;
		/**
		 * The decreasing amount of priority for a building type
		 */
		public static final double ARMORED_BUILDING_UNIT_DECREMENT = 10.0;
		/**
		 * Extra priority for super tower, Executioner. This priority is added
		 * to the normal priority
		 */
		public static final double EXECUTIONER_EXTRA_PRIORITY = 10.0;
		/**
		 * The multiplier for the anti-air turret
		 */
		public static final double COPPERHEAD_PRIORITY = 2.0;

		// -------------------------------------------------------
		// BUILDER - Priorities regarding builders
		// -------------------------------------------------------
		/**
		 * The metal we need to have before building a new builder
		 */
		public static final double BUILDER_NEW_MIN_METAL = 50.0;
		/**
		 * Builder minimum priority
		 */
		public static final double BUILDER_PRIORITY_MIN = 100.0;
		/**
		 * The minimum time we must wait until we build a new builder again. In
		 * seconds. Only used when forcing build
		 */
		public static final double BUILDER_WAIT_TIME = 10.0;

		// -------------------------------------------------------
		// MOBILE BUILDERS
		// -------------------------------------------------------
		/**
		 * Number of flying builder we should have after a specific time
		 */
		public static final double BUILDER_MINUTES_PER_FLYING = 3.0;
		/**
		 * The priority for flying builders
		 */
		public static final double BUILDER_FLYING_PRIORITY = 200.0;

		// -------------------------------------------------------
		// FACTORIES
		// -------------------------------------------------------
		/**
		 * Number of factories we should have after a specified time
		 */
		public static final double FACTORY_MINUTES_PER = 5.0;
		/**
		 * The priority for flying builders
		 */
		public static final double FACTORY_PRIORITY = 300.0;

		// -------------------------------------------------------
		// SCOUT - Priorities regarding scouts
		// -------------------------------------------------------
		/**
		 * The mimimum scout to have alive
		 */
		public static final double SCOUTS_MIN = 1.0;

		// -------------------------------------------------------
		// Overall - Some priorities regarding all groups/units
		// -------------------------------------------------------
		/**
		 * Apply this value to force a unit to being built
		 */
		public static final double FORCE_BUILD = 10000.0;
	}
}
