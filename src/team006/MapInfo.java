package team006;

import battlecode.common.*;

import java.util.*;

/**
 * Created by andrewalbers on 9/25/16.
 */

public class MapInfo {
    public Map<Integer, MapLocation> archonLocations = new HashMap<>();
    public Map<MapLocation, Boolean> denLocations = new HashMap<>();
    public Map<MapLocation, Integer> hasBeenLocations = new HashMap<>();
    public Map<MapLocation, Integer> partLocations = new HashMap<>();
    public Map<MapLocation, Integer> neutralLocations = new HashMap<>();
    public MapLocation assistLoc = null;
    public MapLocation lastKnownOpponentLocation = null;
    public int roundNum = 0;
    public Map<Integer, Integer> scoutSignals = new HashMap<>(); // <scoutId : roundLastSignaled>
    public int selfScoutsCreated = 0;
    public RobotType selfType = null;
    public Team selfTeam = null;
    public Team opponentTeam = null;
    public int selfId;
    public double selfHealth;
    public double selfMaxHealth;
    public boolean selfBeingHealed = false;
    public RobotInfo[] hostileRobots;
    public RobotInfo[] friendlyRobots;
    public RobotInfo[] opponentRobots;
    public int selfSenseRadiusSq = 0;
    public int selfAttackRadiusSq = 0;
    public double selfAttackPower = 0;
    public double selfWeaponDelay = 0;
    public boolean selfWeaponReady = false;
    public int selfLastSignaled = 0;
    public MapLocation selfLoc = null;
    public Signal urgentSignal = null;
    public int[] spawnSchedule = null;
    public int lastRoundZombieSeen = 0;
    public int lastRoundScoutMessageSeen = 0;
    public int selfCreatedRound;                // round # when unit was created
    public int scoutsCreated = 0;
    public int vipersCreated = 0;               // number of vipers this unit has created
    public int teamAttackSignalRound = -1;      // round when team attack signal was given or received

    public int unitsCreated = 0;

    public MapLocation closestAssistLoc = null;

    public int scoutRoundsTraveled = 0;

    public Random rand;

    public MapInfo(RobotController rc) {
        spawnSchedule = rc.getZombieSpawnSchedule().getRounds();
        selfType = rc.getType();
        selfId = rc.getID();
        selfTeam = rc.getTeam();
        selfMaxHealth = selfType.maxHealth;
        opponentTeam = selfTeam.equals(Team.A) ? Team.B : Team.A;
        selfCreatedRound = rc.getRoundNum();
        selfAttackPower = selfType.attackPower;
        selfSenseRadiusSq = selfType.sensorRadiusSquared;
        selfAttackRadiusSq = selfType.attackRadiusSquared;
        rand = new Random(selfId);
    }

    public void updateAll(RobotController rc) {
        // Read and update signals
        Signal[] signals = rc.emptySignalQueue();
        selfLoc = rc.getLocation();
        selfWeaponDelay = rc.getWeaponDelay();
        roundNum = rc.getRoundNum();
        selfHealth = rc.getHealth();
        selfWeaponReady = rc.isWeaponReady();
        urgentSignal = null;
        hostileRobots = rc.senseHostileRobots(selfLoc,selfSenseRadiusSq);
        friendlyRobots = rc.senseNearbyRobots(selfLoc, selfSenseRadiusSq, selfTeam);
        opponentRobots = rc.senseNearbyRobots(selfLoc, selfSenseRadiusSq, opponentTeam);

        // clear assistLoc if nearby
        if (assistLoc != null && rc.canSense(assistLoc)){
            assistLoc = null;
        }

        // Update Zombie Spawn Date
        if (spawnSchedule.length > 0) {
            if (spawnSchedule[0] < roundNum) {
                for (int i = 0; i < spawnSchedule.length; i++){
                    if (spawnSchedule[i] >= roundNum) {
                        spawnSchedule = Arrays.copyOfRange(spawnSchedule, i, spawnSchedule.length);
                        break;
                    }
                }
            }
        }

        // Process Signals
        MapLocation thisLocation;
        for (Signal signal : signals){
            thisLocation = signal.getLocation();
            int[] message = signal.getMessage();
            if (message != null) {
                if (message[0] == SignalManager.SIG_ASSIST) {
                    // set urgent signal to this if it's the closest
                    setAssistLoc(thisLocation, signal);
                } else if (message[0] == SignalManager.SIG_UPDATE_ARCHON_LOC) {
                    archonLocations.put(signal.getID(),signal.getLocation());
                } else if (message[0] == SignalManager.SIG_SCOUT_DENS) {
                    updateZombieDenSignal(signal, thisLocation, message);
                    lastRoundScoutMessageSeen = roundNum;
                } else if (message[0] == SignalManager.SIG_SCOUT_OPPONENT) {
                    updateLastKnownOpponentLocation(signal, thisLocation, message);
                } else if (message[0] == SignalManager.SIG_SCOUT_ZOMBIE) {
                    lastRoundZombieSeen = roundNum;
                } else if (message[0] == SignalManager.SIG_SCOUT_NEUTRALS) {
                    updateNeutrals(thisLocation, message);
                    lastRoundScoutMessageSeen = roundNum;
                } else if (message[0] == SignalManager.SIG_SCOUT_PARTS) {
                    updatePartLocations(thisLocation, message);
                    lastRoundScoutMessageSeen = roundNum;
                } else if (message[0] == SignalManager.SIG_TEAM_ATTACK) {
                    // a special signal that outweighs other signals. If received, break out and follow it
                    urgentSignal = signal;
                    teamAttackSignalRound = roundNum;
                    break;
                }
            } else {
                // set urgent signal to this if it's the closest
                setAssistLoc(thisLocation, signal);
            }
        }
        try {
            // set each zombie den location to false if it's been destroyed
            for (MapLocation denLoc : denLocations.keySet()) {
                if (denLocations.get(denLoc) == true) {
                    if (rc.canSense(denLoc)) {
                        RobotInfo denBot = rc.senseRobotAtLocation(denLoc);
                        if (denBot == null || denBot.type != RobotType.ZOMBIEDEN) {
                            updateZombieDens(denLoc, false);
                        }
                    }
                }
            }

            // clear last known opponent location if no opponents here
            if (lastKnownOpponentLocation != null && rc.canSense(lastKnownOpponentLocation)){
                if (hostileRobots.length == 0){
                    lastKnownOpponentLocation = null;
                }
            }

        } catch (GameActionException gae){
            System.out.println(gae.getMessage());
            rc.setIndicatorString(2,gae.getMessage());
            gae.printStackTrace();
        }

        if (selfType == RobotType.ARCHON) {
            // stop recording last signals from scouts that are probably dead
            for (Iterator<Map.Entry<Integer, Integer>> it = scoutSignals.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, Integer> entry = it.next();
                if (roundNum - entry.getValue() > 50) {
                    it.remove();
                }
            }
        }
    }

    public MapLocation getNearestFriendlyArchonLoc(RobotController rc) {
        int nearestDist = 999999;
        MapLocation nearestLocation = null;
        int rcId = rc.getID();
        MapLocation rcLoc = rc.getLocation();
        for (Map.Entry<Integer, MapLocation> archonLocation : archonLocations.entrySet()){
            if (archonLocation.getKey() != rcId) {
                if (rcLoc.distanceSquaredTo(archonLocation.getValue()) < nearestDist) {
                    nearestLocation = archonLocation.getValue();
                }
            }
        }
        return nearestLocation;
    }

    public void setAssistLoc(MapLocation location, Signal signal) {
        if (assistLoc == null || selfLoc.distanceSquaredTo(assistLoc) > selfLoc.distanceSquaredTo(location)) {
            assistLoc = location;
            urgentSignal = signal;
        }
    }

    public void updateZombieDenSignal(Signal signal, MapLocation sigLoc, int[] message) {
        MapLocation denLocation = SignalManager.decodeLocation(sigLoc, message[1]);
        if (denLocations.containsKey(denLocation) == false || denLocations.get(denLocation)) {
            urgentSignal = signal;
            updateZombieDens(denLocation, true);
        }
    }

    public void updateZombieDens(MapLocation denLoc, boolean isHere) {
        denLocations.put(denLoc, isHere);
        if (isHere) {
            lastRoundZombieSeen = roundNum;
        }
    }

    public void updateLastKnownOpponentLocation(Signal signal, MapLocation sigLoc, int[] message){
        lastKnownOpponentLocation = SignalManager.decodeLocation(sigLoc, message[1]);
        // treat this as an urgent signal if viper
        if (selfType == RobotType.VIPER){
            urgentSignal = signal;
        }
    }

    public void updateNeutrals(MapLocation neutralLoc, int[] message){
        neutralLocations.put(neutralLoc, message[1]);
    }

    public void updatePartLocations(MapLocation partLoc, int[] message){
        partLocations.put(partLoc, message[1]);
    }

    // return the actual shortest MOVE distance between two locations
//    public static int moveDist(MapLocation fromLoc, MapLocation toLoc){
//        int xDist = Math.abs(toLoc.x - fromLoc.x);
//        int yDist = Math.abs(toLoc.y - fromLoc.y);
//        return Math.max(xDist, yDist);
//    }

    public void clearHasBeenLocations() {
        hasBeenLocations = new HashMap<>();
    }

    public void incrementHasBeenOnCurrent() {
       if (hasBeenLocations.containsKey(selfLoc)) {
           hasBeenLocations.put(selfLoc, hasBeenLocations.get(selfLoc) + 1);
       } else {
           hasBeenLocations.put(selfLoc, 1);
       }
    }

    public boolean needAnotherScout() {
        for (int archonId : archonLocations.keySet()) {
            if (archonId < selfId) {
                return false; // don't make scouts if not the lowest id archon
            }
        }
        return roundNum > 25 && (double)scoutsCreated / (double)roundNum < 0.002;
    }

    public boolean isTimeForVipers() {
        // if it's been > 1000 rounds since last zombie sighting, and team attack signal hasn't been given, start creating vipers
        if (roundNum - Math.max(selfCreatedRound, lastRoundZombieSeen) > 1000 && teamAttackSignalRound == -1){
            return true;
        }
        return false;
    }

    public boolean isTimeToSignalTeamAttack() {
        if (teamAttackSignalRound == -1 && vipersCreated >= 10 && lastKnownOpponentLocation != null){
            return true;
        }
        return false;
    }
}
