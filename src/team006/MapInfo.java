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
    public int roundNum = 0;
    public Map<Integer, Integer> scoutSignals = new HashMap<>(); // <scoutId : roundLastSignaled>
    public int selfScoutsCreated = 0;
    public RobotType selfType = null;
    public Team selfTeam = null;
    public int selfId;
    public boolean selfCanClearRubble;
    public int selfSenseRadiusSq = 0;
    public int selfAttackRadiusSq = 0;
    public double selfWeaponDelay = 0;
    public int selfLastSignaled = 0;
    public MapLocation selfLoc = null;
    public Signal urgentSignal = null;
    public int[] spawnSchedule = null;
    public int timeTillSpawn = 999999;

    public MapInfo(RobotController rc) {
        spawnSchedule = rc.getZombieSpawnSchedule().getRounds();
        selfType = rc.getType();
        selfId = rc.getID();
        selfTeam = rc.getTeam();
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
        urgentSignal = null;

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
            if (signal.getTeam() == selfTeam) {
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
                    }
                } else {
                    // set urgent signal to this if it's the closest
                    minUrgentDist = setUrgentSignal(minUrgentDist, thisLocation, signal);
                }
            }
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

    // updates the map if anything special needs to happen on task complete
    public void handleTaskComplete(Assignment assignment) {
        if (assignment.assignmentType == AssignmentManager.BOT_KILL_DEN) {
            denLocations.put(assignment.targetLocation, false);
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
}
