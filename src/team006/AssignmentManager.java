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
    public static int BOT_ASSIST_LOC = 12;
    public static int BOT_ASSEMBLE_TO_LOC = 13;

    public static Assignment getAssignment(RobotController rc, Random rand, MapInfo mapInfo, Assignment assignment) {

        int assignmentType = 0;
        int targetInt = 0;
        MapLocation targetLocation = null;

        if (rc.getType() == RobotType.ARCHON) {

            assignmentType = ARCHON_ACTION;

        } else if ( rc.getType() == RobotType.SCOUT ) {

            mapInfo.scoutRoundsTraveled = 0;
            assignmentType = BOT_SCOUT;
            targetLocation = mapInfo.selfLoc.add(Constants.DIRECTIONS[mapInfo.rand.nextInt(8)], 20);

        } else {

            assignmentType = BOT_KILL_DEN;
            targetLocation = getNearestZombieDen(mapInfo);
            if (targetLocation == null) {
                if (mapInfo.teamAttackSignalRound != -1 && mapInfo.roundNum > mapInfo.teamAttackSignalRound + 50) {
                    assignmentType = BOT_ATTACK_MOVE_TO_LOC;
                    targetLocation = mapInfo.lastKnownOpponentLocation;
                } else if (mapInfo.assistLoc != null){
                    targetLocation = mapInfo.assistLoc;
                    assignmentType = BOT_ASSIST_LOC;
                } else {
                    assignmentType = BOT_PATROL;
                }
            }
        }
        return new Assignment(targetInt, assignmentType, targetLocation);
    }

    public static Assignment getSignalAssignment(RobotController rc, MapInfo mapInfo, Assignment assignment) {

        if (mapInfo.selfType == RobotType.SCOUT) {
            return null;
        }

        int[] message = mapInfo.urgentSignal.getMessage();
        int assignmentType = 0;
        int targetInt = 0;
        MapLocation targetLocation = mapInfo.urgentSignal.getLocation();

        if (message != null) {
            targetLocation = SignalManager.decodeLocation(mapInfo.urgentSignal.getLocation(), message[1]);
        }

        // If team attack signal sent
        if (mapInfo.teamAttackSignalRound == -1 && message != null && message[0] == SignalManager.SIG_TEAM_ATTACK) {
            mapInfo.teamAttackSignalRound = mapInfo.roundNum;
            if (mapInfo.selfType.equals(RobotType.GUARD) || mapInfo.selfType.equals(RobotType.SOLDIER)) {
                assignmentType = BOT_ASSEMBLE_TO_LOC;
                return new Assignment(targetInt, assignmentType, targetLocation);
            } else if (mapInfo.selfType.equals(RobotType.VIPER)){
                assignmentType = BOT_ATTACK_MOVE_TO_LOC;
                targetLocation = mapInfo.lastKnownOpponentLocation;
                return new Assignment(targetInt, assignmentType, targetLocation);
            }
        }

        if (assignment.assignmentType != AssignmentManager.BOT_ASSEMBLE_TO_LOC) {
            // if new zombie den signal

            if (mapInfo.selfType.canAttack()) {

                if (message != null && message[0] == SignalManager.SIG_SCOUT_DENS) {
                    assignmentType = BOT_KILL_DEN;
                    targetLocation = SignalManager.decodeLocation(targetLocation, message[1]);
                    return new Assignment(targetInt, assignmentType, targetLocation);
                }

                // if request for assistance
                if (message == null || message[0] == SignalManager.SIG_ASSIST) {

                    targetLocation = mapInfo.assistLoc;

                    // if not already assisting, or if the new assist location is closer than the original one, set assignment to assisting location
                    if (assignment.assignmentType != AssignmentManager.BOT_ASSIST_LOC
                            || mapInfo.selfLoc.distanceSquaredTo(targetLocation) < mapInfo.selfLoc.distanceSquaredTo(assignment.targetLocation)) {
                        assignmentType = BOT_ASSIST_LOC;
                        return new Assignment(targetInt, assignmentType, targetLocation);
                    }
                }
            }
        }
        return null;
    }

    public static MapLocation getNearestZombieDen(MapInfo mapInfo) {
        MapLocation targetLocation = null;
        int minDist = 2500; // don't go further than 50 units to kill a zombie den
        for (MapLocation denLoc : mapInfo.denLocations.keySet()) {
            if (mapInfo.denLocations.get(denLoc)) {
                int thisDist = mapInfo.selfLoc.distanceSquaredTo(denLoc);
                if (thisDist < minDist) {
                    targetLocation = denLoc;
                    minDist = thisDist;
                }
            }
        }
        return targetLocation;
    }
}
