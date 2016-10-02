package team006;

import battlecode.common.*;

import java.util.Random;

/**
 * Created by andrewalbers on 9/14/16.
 */
public class RobotTasks {

    public static int TASK_NOT_GIVEN = -1;
    public static int TASK_IN_PROGRESS = 0;
    public static int TASK_COMPLETE = 1;
    public static int TASK_ABANDONED = 2;
    public static int TASK_SIGNALED = 3;
    public static int TASK_ATTACKING = 4;
    public static int TASK_RETREATING = 5;

    public static int pursueTask(RobotController rc, MapInfo mapInfo, Assignment assignment) {
        try {
            if ( assignment.assignmentType == AssignmentManager.ARCHON_ACTION ) {
                return archonAction(rc, mapInfo);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_MOVE_TO_LOC ) {
                return moveToLocation(rc, mapInfo, assignment.targetLocation, TASK_IN_PROGRESS);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_ATTACK_MOVE_TO_LOC ){
                return attackMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_TIMID_MOVE_TO_LOC ){
                return timidMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_RUN_AWAY ){
                return retreat(rc, mapInfo);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_PATROL ){
                return attackMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if (assignment.assignmentType == AssignmentManager.BOT_TURRET_DEFEND) {
                return turretDefend(rc, mapInfo);
            } else if (assignment.assignmentType == AssignmentManager.BOT_SCOUT) {
                return scoutLocation(rc, mapInfo, assignment.targetLocation);
            } else if (assignment.assignmentType == AssignmentManager.BOT_KILL_DEN) {
                return attackZombieDen(rc, mapInfo, assignment.targetLocation);
            } else if (assignment.assignmentType == AssignmentManager.BOT_ASSIST_LOC) {
                return assistLocation(rc, mapInfo, assignment.targetLocation);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return TASK_NOT_GIVEN;
    }

    // Move toward a location
    // task variable is the task type to return if no issues completing task
    // (set task = TASK_IN_PROGRESS normally)
    public static int moveToLocation(RobotController rc, MapInfo mapInfo, MapLocation targetLocation, int task) {
        try {
            if (mapInfo.selfLoc.equals(targetLocation)) {
                // If goal reached
                rc.setIndicatorString(1, "move task complete");
                return TASK_COMPLETE;
            } else {

                // evaluate best spot to go
                int minScore = 9999999;
                Direction evalDirection;
                MapLocation evalLocation;

                Direction dirToMove = mapInfo.selfLoc.directionTo(targetLocation);
                MapLocation locToMove = null;
                double rubbleToClear = 0;

                for (int i = 0; i < Constants.DIRECTIONS.length; i++) {
                    int thisScore = 0;
                    double rubble = 0.0;
                    evalDirection = Constants.DIRECTIONS[i];
                    evalLocation = mapInfo.selfLoc.add(evalDirection);
                    if (rc.onTheMap(evalLocation)) {
                        rubble = rc.senseRubble(evalLocation);
                        if (rc.canMove(evalDirection) || rubble >= 100) {
                            if (task == TASK_RETREATING) {
                                thisScore -= MapInfo.moveDist(evalLocation, targetLocation);
                            } else {
                                thisScore += MapInfo.moveDist(evalLocation, targetLocation);
                            }
                            if (mapInfo.hasBeenLocations.containsKey(evalLocation)) {
                                thisScore += Math.pow(1.5, (double) mapInfo.hasBeenLocations.get(evalLocation));
                            }
                            if (mapInfo.selfType != RobotType.SCOUT) {
                                thisScore += rubble / 100;
                            }
                            if (thisScore < minScore) {
                                minScore = thisScore;
                                dirToMove = evalDirection;
                                locToMove = evalLocation;
                                rubbleToClear = rubble;
                            }
                        }
                    }
                }


                if (locToMove != null) {
                    if (rubbleToClear < 50 || mapInfo.selfType == RobotType.SCOUT) {
                        rc.setIndicatorString(1, "moving to location");
                        rc.move(dirToMove);
                    } else {
                        rc.setIndicatorString(1, "clearing rubble");
                        rc.clearRubble(dirToMove);
                    }
                } else {
                    rc.setIndicatorString(1, "abandoning task, cannot move");
                    return TASK_ABANDONED;
                }
            }
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
        return task;
    }

    public static int scoutLocation(RobotController rc, MapInfo mapInfo, MapLocation targetLocation) {
        try {
            rc.setIndicatorString(0, "scouting");
            if (mapInfo.roundNum - mapInfo.selfLastSignaled > 10) {
                SignalManager.scoutEnemies(rc, mapInfo, mapInfo.hostileRobots);
                return TASK_SIGNALED;
            } else if (mapInfo.selfLoc.equals(targetLocation)) {
                SignalManager.scoutResources(rc, mapInfo, rc.sensePartLocations(mapInfo.selfSenseRadiusSq), rc.senseNearbyRobots(mapInfo.selfSenseRadiusSq, Team.NEUTRAL));
                return TASK_COMPLETE;
            } else {
                return moveToLocation(rc, mapInfo, targetLocation, TASK_IN_PROGRESS);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return TASK_ABANDONED;
    }

    // just use attackMoveToLocation, since
    public static int turretDefend(RobotController rc, MapInfo mapInfo){
        try {
            rc.setIndicatorString(0, "defending cause I'm a turret");
            return attackMoveToLocation(rc, mapInfo, mapInfo.selfLoc);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return TASK_ABANDONED;
    }

    // Move toward den attacking any enemies along way.
    public static int attackZombieDen(RobotController rc, MapInfo mapInfo, MapLocation targetLocation) {
        try {
            rc.setIndicatorString(0, "attacking Zombie Den");
            if (rc.canSenseLocation(targetLocation)) {
                if (rc.senseRobotAtLocation(targetLocation) == null) {
                    return TASK_COMPLETE;
                }
            }
            return attackMoveToLocation(rc, mapInfo, targetLocation);
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
        return TASK_ABANDONED;
    }

    public static int assistLocation(RobotController rc, MapInfo mapInfo, MapLocation targetLocation) {
        try {
            return attackMoveToLocation(rc, mapInfo, targetLocation);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return TASK_ABANDONED;
    }

    // Move toward a location attacking any enemies along the way
    public static int attackMoveToLocation(RobotController rc, MapInfo mapInfo, MapLocation targetLocation) {
        try {
            double minThreatEffort = 99999;
            double minNonThreatEffort = 99999;
            int minRange = mapInfo.selfType == RobotType.TURRET ? 5 : 0;
            MapLocation attackLoc = null;
            MapLocation threatLoc = null;
            MapLocation nonThreatLoc = null; // location of nearest enemy archon or den

            if (mapInfo.hostileRobots.length > 0) {
                if (mapInfo.roundNum - mapInfo.selfLastSignaled > 50) {
                    SignalManager.requestHelp(rc, mapInfo, mapInfo.selfLoc);
                    rc.setIndicatorString(2, "selfLastSignaled: " + mapInfo.selfLastSignaled);
                    return TASK_SIGNALED;
                } else {
                    double thisEffort;
                    // find and attack closest enemy bot
                    for (RobotInfo info : mapInfo.hostileRobots) {
                        // Find optimal attack location
                        int thisDist = mapInfo.selfLoc.distanceSquaredTo(info.location);
                        if (thisDist > minRange) {
                            if (info.type == RobotType.ARCHON || info.type == RobotType.SCOUT || info.type == RobotType.ZOMBIEDEN) {
                                // only consider non-threat targets if they are near the actual target location
                                // ie. don't stop to shoot dens if on the way to help another fighter
                                if (info.type == RobotType.ZOMBIEDEN){
                                    mapInfo.updateZombieDens(info.location,true);
                                }
                                thisEffort = thisDist * info.health;
                                if (thisEffort < minNonThreatEffort) {
                                    minNonThreatEffort = thisEffort;
                                    nonThreatLoc = info.location;
                                }
                            } else {
                                thisEffort = thisDist * info.health / info.attackPower;
                                if (thisEffort < minThreatEffort) {
                                    minThreatEffort = thisEffort;
                                    threatLoc = info.location;
                                }
                            }
                        }
                    }

                    attackLoc = threatLoc != null ? threatLoc : nonThreatLoc;

                    if (attackLoc != null) {

                        if (mapInfo.selfWeaponReady) {
                            if (rc.canAttackLocation(attackLoc)) {
                                rc.setIndicatorString(1, "attacking location");
                                rc.attackLocation(attackLoc);
                                return TASK_ATTACKING;
                            } else {
                                return moveToLocation(rc, mapInfo, attackLoc, TASK_ATTACKING);
                            }
                        } else {
                            if (mapInfo.selfType == RobotType.TURRET) {
                                rc.setIndicatorString(1, "no enemies in range");
                                return TASK_IN_PROGRESS;
//                            } else if (mapInfo.isOverPowered()) {
//                                // if overpowered, retreat while not shooting
//                                return retreat(rc, mapInfo);
                            } else if (mapInfo.selfAttackRadiusSq > 1){
                                // see if we can move to a more optimal firing location
                                int currDist = mapInfo.selfLoc.distanceSquaredTo(attackLoc);
                                int bestDist = mapInfo.selfLoc.distanceSquaredTo(attackLoc);
                                int maxDist = mapInfo.selfAttackRadiusSq;
                                Direction bestDir = null;
                                for (Direction dirToMove : Constants.DIRECTIONS){
                                    if (rc.canMove(dirToMove)){
                                        int newDist = mapInfo.selfLoc.add(dirToMove).distanceSquaredTo(attackLoc);
                                        if (newDist <= maxDist && newDist > (double)currDist * 1.5) {
                                            bestDir = dirToMove;
                                        }
                                    }
                                }
                                if (bestDir != null) {
                                    rc.move(bestDir);
                                    return TASK_ATTACKING;
                                }
                            }
                        }
                    }
                }
            } else if (mapInfo.selfType == RobotType.TURRET) {
                // no enemies found, keep on sitting there
                rc.setIndicatorString(1, "watching for enemies");
                return TASK_IN_PROGRESS;
            } else if (MapInfo.moveDist(mapInfo.selfLoc, targetLocation) > 1) {
                rc.setIndicatorString(1, "moving toward target location");
                return moveToLocation(rc, mapInfo, targetLocation, TASK_IN_PROGRESS);
            } else {
                rc.setIndicatorString(1, "attack move task complete");
                return TASK_COMPLETE;
            }
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
        rc.setIndicatorString(1, "attack move task complete");
        return TASK_COMPLETE;
    }

    // Move toward a location but retreat and abandon if enemy sighted
    // This should typically only be used by scouts and archons
    public static int timidMoveToLocation(RobotController rc, MapInfo mapInfo, MapLocation targetLocation) {
        try {
            if (mapInfo.hostileRobots.length > 0){
                for (RobotInfo hostile : mapInfo.hostileRobots) {
                    if (hostile.type.canAttack()) {
                        if (mapInfo.roundNum - mapInfo.selfLastSignaled > 50) {
                            // request assistance
                            SignalManager.requestHelp(rc, mapInfo, mapInfo.selfLoc);
                            rc.setIndicatorString(2, "Signaled for help!");
                            return TASK_SIGNALED;
                        } else {
                            rc.setIndicatorString(2, "Abandoned timid move!");
                            return TASK_ABANDONED;
                        }
                    }
                }
            } if (mapInfo.selfLoc.equals(targetLocation)){
                rc.setIndicatorString(2, "Timid move complete!");
                return TASK_COMPLETE;
            } else {
                rc.setIndicatorString(2, "Timid moving in progress...");
                return moveToLocation(rc, mapInfo, targetLocation, TASK_IN_PROGRESS);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        rc.setIndicatorString(2, "Uh oh, timid move abandoned");
        return TASK_ABANDONED;
    }

    public static int archonAction(RobotController rc, MapInfo mapInfo) {
        // all logic for archon for each round
        try {
            // get neutrals info. If adjacent to a neutral, activate before checking for hostiles
            RobotInfo[] neutrals = rc.senseNearbyRobots(mapInfo.selfSenseRadiusSq, Team.NEUTRAL);
            MapLocation closestNeutralLoc = null;
            int minNeutralDist = 99999;
            for (RobotInfo neutral : neutrals) {
                int neutralDist = mapInfo.selfLoc.distanceSquaredTo(neutral.location);
                if (neutralDist < minNeutralDist) {
                    minNeutralDist = neutralDist;
                    closestNeutralLoc = neutral.location;
                }
                if (mapInfo.selfLoc.isAdjacentTo(closestNeutralLoc)) {
                    rc.activate(closestNeutralLoc);
                    rc.setIndicatorString(2, "Task complete!");
                    return TASK_COMPLETE;
                }
            }

            // stop doing regular action if a threatening enemy robot is near
            RobotInfo[] hostiles = rc.senseHostileRobots(mapInfo.selfLoc, 13);
            if (hostiles.length > 0) {
                for (RobotInfo hostile : hostiles) {
                    if (hostile.type.canAttack()) {
                        return retreat(rc, mapInfo);
                    }
                }
            }

            if (closestNeutralLoc != null){
                return timidMoveToLocation(rc, mapInfo, closestNeutralLoc);
            }

            // if team parts under threshold, move toward best part location in sight (if valuable enough)
            double teamParts = rc.getTeamParts();
            MapLocation[] partLocations = rc.sensePartLocations(mapInfo.selfSenseRadiusSq);
            double bestScore = 50.0; // only pursue adjacent parts of 50+, parts 2 away of 200+
            MapLocation bestPartLocation = null;
            for (MapLocation partLoc : partLocations) {
                int partDist = MapInfo.moveDist(mapInfo.selfLoc, partLoc);
                if (partDist != 0) {
                    double score = (200 * rc.senseParts(partLoc)) / (Math.pow(partDist, 2) * (teamParts +1)); // add 1 so we don't divide by 0
                    if (score > bestScore) {
                        bestPartLocation = partLoc;
                        bestScore = score;
                    }
                }
            }
            if (bestPartLocation != null) {
                return timidMoveToLocation(rc, mapInfo, bestPartLocation);
            }

            // If we did none of the above stuff, make a robot!

            RobotType typeToBuild = Constants.ROBOT_TYPES[1]; // default SOLDIER

            if (mapInfo.rand.nextInt(100) < 30) {
                typeToBuild = Constants.ROBOT_TYPES[2]; // GUARDS are 30% of regular troops
            } else if (mapInfo.rand.nextInt(50) == 1) {
                typeToBuild = Constants.ROBOT_TYPES[0]; // build SCOUT occasionally
            } else if (mapInfo.rand.nextInt(25) == 1) {
                typeToBuild = Constants.ROBOT_TYPES[4]; // build TURRET occasionally
            }
            // Check for sufficient parts
            if (rc.hasBuildRequirements(typeToBuild)) {
                // Choose a random direction to try to build in
                for (int i = 0; i < 8; i++) {
                    // If possible, build in this direction
                    if (rc.canBuild(Constants.DIRECTIONS[i], typeToBuild)) {
                        rc.build(Constants.DIRECTIONS[i], typeToBuild);
                        rc.setIndicatorString(2, "Build task Complete!");
                        return TASK_COMPLETE;
                    }
                }
            } else {
                return TASK_IN_PROGRESS;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        rc.setIndicatorString(2, "Uh oh, abandoned archon action");
        return TASK_ABANDONED;
    }

    // Retreats to a given target location until no enemies in sight
    // If target location is null, moves in the opposite direction from the closest enemy
    public static int retreat(RobotController rc, MapInfo mapInfo) {
        try {
            if (mapInfo.hostileRobots.length == 0) {
                rc.setIndicatorString(2, "Retreat task Complete!");
                return TASK_COMPLETE;
            } else {
                int minDist = 999999;
                MapLocation closestEnemyLoc = null;
                for (RobotInfo info : mapInfo.hostileRobots) {
                    int dist = MapInfo.moveDist(mapInfo.selfLoc, info.location);
                    if (dist < minDist && info.type != RobotType.ZOMBIEDEN && info.type != RobotType.ARCHON && info.type != RobotType.SCOUT) {
                        closestEnemyLoc = info.location;
                        minDist = dist;
                    }
                }
                // moveToLocation will find best way away from worstLocation if task is TASK_RETREATING
                rc.setIndicatorString(2, "Retreating in progress...");
                return moveToLocation(rc, mapInfo, closestEnemyLoc, TASK_RETREATING);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        rc.setIndicatorString(2, "Uh oh, abandoned retreating");
        return TASK_ABANDONED;
    }
}
