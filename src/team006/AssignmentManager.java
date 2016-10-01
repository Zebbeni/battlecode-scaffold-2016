package team006;

import battlecode.common.*;

import java.util.Random;

/**
 * Created by andrewalbers on 9/14/16.
 */
public class AssignmentManager {
    public static int ARCHON_ACTION = 1;
    public static int BOT_MOVE_TO_LOC = 4;
    public static int BOT_ATTACK_MOVE_TO_LOC = 5;
    public static int BOT_TIMID_MOVE_TO_LOC = 6;
    public static int BOT_RUN_AWAY = 7;
    public static int BOT_PATROL = 8;
    public static int BOT_TURRET_DEFEND = 9;
    public static int BOT_SCOUT = 10;
    public static int BOT_KILL_DEN = 11;
    public static int BOT_ASSIST_LOC;

    public static Assignment getAssignment(RobotController rc, Random rand, MapInfo mapInfo) {

        int assignmentType = 0;
        int targetInt = 0;
        MapLocation targetLocation = null;

        if ( rc.getType() == RobotType.ARCHON ) {

            assignmentType = ARCHON_ACTION;

        } else if ( mapInfo.selfType == RobotType.SOLDIER || mapInfo.selfType == RobotType.GUARD){

            assignmentType = BOT_PATROL;
            targetLocation = getNearestZombieDen(mapInfo);
            if (targetLocation == null) {
                targetLocation = new MapLocation(mapInfo.selfLoc.x + rand.nextInt(21) - 10, mapInfo.selfLoc.y + rand.nextInt(21) - 10);
            } else {
                assignmentType = BOT_KILL_DEN;
            }

        } else if ( rc.getType() == RobotType.SCOUT ){
            assignmentType = BOT_SCOUT;
            targetLocation = new MapLocation(7 * (Math.round(mapInfo.selfLoc.x + (rand.nextInt(3) - 1) * 7)/7), 7 * (Math.round(mapInfo.selfLoc.y + (rand.nextInt(3) - 1) * 7))/7);

        } else if ( rc.getType() == RobotType.VIPER ){

            assignmentType = BOT_PATROL;
            targetLocation = new MapLocation(mapInfo.selfLoc.x + rand.nextInt(21) - 10, mapInfo.selfLoc.y + rand.nextInt(21) - 10);

        } else if ( rc.getType() == RobotType.TURRET ){

            assignmentType = BOT_TURRET_DEFEND;

        } else if ( rc.getType() == RobotType.TTM ) {

        }
        return new Assignment(targetInt, assignmentType, targetLocation);
    }

    public static Assignment getSignalAssignment(RobotController rc, MapInfo mapInfo, Signal signal, Assignment assignment) {
        int[] message = signal.getMessage();

        int assignmentType = 0;
        int targetInt = 0;
        MapLocation targetLocation = signal.getLocation();
        if (message != null) {
            targetLocation = SignalManager.decodeLocation(signal.getLocation(), message[1]);
        }

        if (assignment.assignmentType != AssignmentManager.BOT_KILL_DEN && (message == null || message[0] == SignalManager.SIG_ASSIST)) {

            if (mapInfo.selfType == RobotType.SOLDIER || mapInfo.selfType == RobotType.GUARD) {
                // don't interrupt current assignment if this one is farther away
                if (assignment.assignmentType == AssignmentManager.BOT_ASSIST_LOC){
                    if (MapInfo.moveDist(mapInfo.selfLoc,targetLocation) > MapInfo.moveDist(mapInfo.selfLoc,assignment.targetLocation)){
                        return null;
                    }
                }
                assignmentType = BOT_ASSIST_LOC;
                return new Assignment(targetInt, assignmentType, targetLocation);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static MapLocation getNearestZombieDen(MapInfo mapInfo) {
        MapLocation targetLocation = null;
        MapLocation rcLoc = mapInfo.selfLoc;
        int minDist = 625; // don't go further than 25 units to kill a zombie den
        for (MapLocation denLoc : mapInfo.denLocations.keySet()) {
            if (mapInfo.denLocations.get(denLoc) == true) { // TODO: make sure we remove these when found to no longer exist
                int thisDist = rcLoc.distanceSquaredTo(denLoc);
                if (thisDist < minDist) {
                    targetLocation = denLoc;
                    minDist = thisDist;
                }
            }
        }
        return targetLocation;
    }
}
