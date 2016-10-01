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
    public int roundNum = 0;
    public Map<Integer, Integer> scoutSignals = new HashMap<>(); // <scoutId : roundLastSignaled>
    public int selfScoutsCreated = 0;
    public RobotType selfType = null;
    public Team selfTeam = null;
    public int selfId;
    public double selfHealth;
    public RobotInfo[] hostileRobots;
    public RobotInfo[] friendlyRobots;
    public int selfSenseRadiusSq = 0;
    public int selfAttackRadiusSq = 0;
    public double selfAttackPower = 0;
    public double selfWeaponDelay = 0;
    public boolean selfWeaponReady = false;
    public int selfLastSignaled = 0;
    public MapLocation selfLoc = null;
    public Signal urgentSignal = null;
    public int[] spawnSchedule = null;
    public int timeTillSpawn = 999999;
    public int lastRoundScoutMessageSeen = 0;

    public MapInfo(RobotController rc) {
        spawnSchedule = rc.getZombieSpawnSchedule().getRounds();
        selfType = rc.getType();
        selfId = rc.getID();
        selfTeam = rc.getTeam();
        selfAttackPower = selfType.attackPower;
        selfSenseRadiusSq = selfType.sensorRadiusSquared;
        selfAttackRadiusSq = selfType.attackRadiusSquared;
    }

    public void updateAll(RobotController rc) {
        // Read and update signals
        Map<Integer, MapLocation> newArchonPositions = new HashMap<Integer, MapLocation>();
        Signal[] signals = rc.emptySignalQueue();
        selfLoc = rc.getLocation();
        selfWeaponDelay = rc.getWeaponDelay();
        roundNum = rc.getRoundNum();
        selfHealth = rc.getHealth();
        selfWeaponReady = rc.isWeaponReady();
        urgentSignal = null;
        hostileRobots = rc.senseHostileRobots(selfLoc,selfSenseRadiusSq);
        friendlyRobots = rc.senseNearbyRobots(selfLoc, selfSenseRadiusSq, selfTeam);

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
            timeTillSpawn = spawnSchedule[0] - roundNum;
        } else {
            timeTillSpawn = 999999;
        }

        // Process Signals
        MapLocation thisLocation;
        int minUrgentDist = 9999999;
        for (Signal signal : signals){
            thisLocation = signal.getLocation();
            int[] message = signal.getMessage();
            if (message != null) {
                if (message[0] == SignalManager.SIG_ASSIST) {
                    // set urgent signal to this if it's the closest
                    minUrgentDist = setUrgentSignal(minUrgentDist, thisLocation, signal);
                } else if (message[0] == SignalManager.SIG_UPDATE_ARCHON_LOC) {
                    newArchonPositions.put(signal.getID(),signal.getLocation());
                } else if (message[0] == SignalManager.SIG_SCOUT_DENS) {
                    updateZombieDens(thisLocation, message);
                    lastRoundScoutMessageSeen = roundNum;
                } else if (message[0] == SignalManager.SIG_SCOUT_NEUTRALS) {
                    updateNeutrals(thisLocation, message);
                    lastRoundScoutMessageSeen = roundNum;
                } else if (message[0] == SignalManager.SIG_SCOUT_PARTS) {
                    updatePartLocations(thisLocation, message);
                    lastRoundScoutMessageSeen = roundNum;
                }
            } else {
                // set urgent signal to this if it's the closest
                minUrgentDist = setUrgentSignal(minUrgentDist, thisLocation, signal);
            }
        }

        rc.setIndicatorString(2, "received " + signals.length + " signals. Round: " + roundNum + " hasBeen: " + hasBeenLocations.size());

        if (selfType == RobotType.ARCHON) {
            // stop recording last signals from scouts that are probably dead
            for (Iterator<Map.Entry<Integer, Integer>> it = scoutSignals.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, Integer> entry = it.next();
                if (roundNum - entry.getValue() > 50) {
                    it.remove();
                }
            }
        }

        if (newArchonPositions.size() > 0) {
            archonLocations = newArchonPositions;
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

    public int setUrgentSignal(int minDist, MapLocation location, Signal signal) {
        int distToSignal = selfLoc.distanceSquaredTo(location);
        if (distToSignal < minDist) {
            urgentSignal = signal;
            return distToSignal;
        } else {
            return minDist;
        }
    }

    public void updateZombieDens(MapLocation sigLoc, int[] message) {
        denLocations.put(SignalManager.decodeLocation(sigLoc, message[1]),true);
    }

    public void updateZombieDens(MapLocation denLoc, boolean isHere) {
        denLocations.put(denLoc, isHere);
    }

    public void updateNeutrals(MapLocation neutralLoc, int[] message){
        neutralLocations.put(neutralLoc, message[1]);
    }

    public void updatePartLocations(MapLocation partLoc, int[] message){
        partLocations.put(partLoc, message[1]);
    }

    // updates the map if anything special needs to happen on task complete
    public void handleTaskComplete(Assignment assignment) {
        if (assignment.assignmentType == AssignmentManager.BOT_KILL_DEN) {
            denLocations.put(assignment.targetLocation, false);
        } else if (assignment.assignmentType == AssignmentManager.ARCH_ACTIVATE_NEUTRALS) {
            neutralLocations.remove(assignment.targetLocation);
        } else if (assignment.assignmentType == AssignmentManager.ARCH_COLLECT_PARTS) {
            partLocations.remove(assignment.targetLocation);
        }
    }

    // return the actual shortest MOVE distance between two locations
    public static int moveDist(MapLocation fromLoc, MapLocation toLoc){
        int xDist = Math.abs(toLoc.x - fromLoc.x);
        int yDist = Math.abs(toLoc.y - fromLoc.y);
        return Math.max(xDist, yDist);
    }

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

    public boolean isOverPowered() {
        double enemyPower = 0;
        double friendlyPower = selfAttackPower * selfHealth;
        for (RobotInfo enemy : hostileRobots) {
            if (enemy.type.canAttack()) {
                enemyPower += enemy.attackPower * enemy.health;
            }
        }
        for (RobotInfo friend : friendlyRobots) {
            if (friend.type.canAttack()) {
                friendlyPower += friend.attackPower * friend.health;
            }
        }
        return enemyPower > friendlyPower;
    }
}
