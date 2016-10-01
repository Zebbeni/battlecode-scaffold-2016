package team006;

import battlecode.common.*;

import java.awt.*;

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
            if ( assignment.assignmentType == AssignmentManager.ARCH_COLLECT_PARTS ) {
                return collectParts(rc, mapInfo, assignment.targetLocation, assignment.targetInt);
            } else if ( assignment.assignmentType == AssignmentManager.ARCH_BUILD_ROBOTS ){
                return buildRobot(rc, mapInfo, assignment.targetInt);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_MOVE_TO_LOC ) {
                return moveToLocation(rc, mapInfo, assignment.targetLocation, TASK_IN_PROGRESS);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_ATTACK_MOVE_TO_LOC ){
                return attackMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_TIMID_MOVE_TO_LOC ){
                return timidMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_RUN_AWAY ){
                return retreatToLocation(rc, mapInfo, assignment.targetLocation);
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
            if (rc.getLocation().equals(targetLocation)) {
                // If goal reached
                rc.setIndicatorString(1, "task complete");
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
                            thisScore += MapInfo.moveDist(evalLocation, targetLocation);
                            if (mapInfo.hasBeenLocations.containsKey(evalLocation)) {
                                thisScore += Math.pow(1.5, (double)mapInfo.hasBeenLocations.get(evalLocation));
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
                RobotInfo[] hostileBots = rc.senseHostileRobots(mapInfo.selfLoc, mapInfo.selfSenseRadiusSq);
                SignalManager.scoutEnemies(rc, mapInfo, hostileBots);
                return TASK_SIGNALED;
            } else if (mapInfo.selfLoc.equals(targetLocation)) {
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
            RobotInfo[] hostileBots = rc.senseHostileRobots(mapInfo.selfLoc, mapInfo.selfSenseRadiusSq);
            double minThreatEffort = 99999;
            double minNonThreatEffort = 99999;
            int minRange = mapInfo.selfType == RobotType.TURRET ? 5 : 0;
            MapLocation attackLoc = null;
            MapLocation threatLoc = null;
            MapLocation nonThreatLoc = null; // location of nearest enemy archon or den

            if (hostileBots.length > 0) {
                if (mapInfo.roundNum - mapInfo.selfLastSignaled > 50) {
                    SignalManager.requestHelp(rc, mapInfo, mapInfo.selfLoc);
                    rc.setIndicatorString(2, "selfLastSignaled: " + mapInfo.selfLastSignaled);
                    return TASK_SIGNALED;
                } else {
                    double thisEffort;
                    // find and attack closest enemy bot
                    for (RobotInfo info : hostileBots) {
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
                        if (mapInfo.selfWeaponDelay >= 1) {
                            rc.setIndicatorString(1, "recharging");
                            if (mapInfo.selfType == RobotType.TURRET) {
                                return TASK_ATTACKING;
                            } else if (rc.canAttackLocation(attackLoc) == false){
                                // if mobile, pursue target while recharging
                                return moveToLocation(rc, mapInfo, attackLoc, TASK_ATTACKING);
                            }
                        } else if (rc.canAttackLocation(attackLoc)) {
                            rc.setIndicatorString(1, "attacking location");
                            rc.attackLocation(attackLoc);
                            return TASK_IN_PROGRESS;
                        } else {
                            if (mapInfo.selfType == RobotType.TURRET) {
                                rc.setIndicatorString(1, "no enemies in range");
                                return TASK_IN_PROGRESS;
                            } else {
                                // if mobile, pursue target
                                return moveToLocation(rc, mapInfo, attackLoc, TASK_ATTACKING);
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
            rc.setIndicatorString(1, "timid moving to location");
            RobotInfo[] hostileRobots = rc.senseHostileRobots(mapInfo.selfLoc, mapInfo.selfSenseRadiusSq);
            for (RobotInfo info : hostileRobots) {
                if (mapInfo.selfType == RobotType.ARCHON && mapInfo.roundNum - mapInfo.selfLastSignaled > 50){
                    // request assistance
                    SignalManager.requestHelp(rc, mapInfo, info.location);
                    return TASK_SIGNALED;
                } else {
                    return TASK_ABANDONED;
                }
            }
            if (mapInfo.selfType == RobotType.ARCHON) {
                RobotInfo[] neutralRobots = rc.senseNearbyRobots(mapInfo.selfLoc, mapInfo.selfSenseRadiusSq, Team.NEUTRAL);
                for (RobotInfo neutralInfo : neutralRobots) {
                    if (mapInfo.selfLoc.distanceSquaredTo(neutralInfo.location) == 1) {
                        rc.activate(neutralInfo.location);
                        rc.setIndicatorString(1, "activating a neutral");
                        return TASK_IN_PROGRESS;
                    }
                }
            }
            if (mapInfo.selfLoc != targetLocation) {
                return moveToLocation(rc, mapInfo, targetLocation, TASK_IN_PROGRESS);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return TASK_COMPLETE;
    }

    // Move toward a target location and collect parts in that area
    // Complete task when rc is on target location and cannot detect any parts
    public static int collectParts(RobotController rc, MapInfo mapInfo, MapLocation targetLocation, int radius) {
        try {
            rc.setIndicatorString(0, "collecting parts");
            if (mapInfo.selfLoc.distanceSquaredTo(targetLocation) > radius * radius) {
                return timidMoveToLocation(rc, mapInfo, targetLocation);
            }

            MapLocation targetPartLocation = null;
            MapLocation[] partLocations = rc.sensePartLocations(mapInfo.selfSenseRadiusSq);
            int minPartDist = 999999;

            for (MapLocation partLocation : partLocations ) {
                int partDist = mapInfo.selfLoc.distanceSquaredTo(partLocation);
                if (partDist < minPartDist && partLocation.distanceSquaredTo(targetLocation) <= radius * radius) {
                    minPartDist = partDist;
                    targetPartLocation = partLocation;
                }
            }
            if (targetPartLocation == null) {
                if (mapInfo.selfLoc.equals(targetLocation)) {
                    // if no parts found and archon in center of collecting region, task complete
                    return TASK_COMPLETE;
                } else {
                    // if no parts seen, move toward center of target region
                    return timidMoveToLocation(rc, mapInfo, targetLocation);
                }
            } else {
                return timidMoveToLocation(rc, mapInfo, targetPartLocation);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return timidMoveToLocation(rc, mapInfo, targetLocation);
    }

    public static int buildRobot(RobotController rc, MapInfo mapInfo, int typeIndex) {
        try {
            rc.setIndicatorString(0, "building Robots");
            // Choose a random unit to build
            RobotType typeToBuild = team006.Constants.ROBOT_TYPES[typeIndex];
            // Check for sufficient parts
            if (rc.hasBuildRequirements(typeToBuild)) {
                // Choose a random direction to try to build in
                Direction dirToBuild = Direction.NORTH;
                for (int i = 0; i < 8; i++) {
                    // If possible, build in this direction
                    if (rc.canBuild(dirToBuild, typeToBuild)) {
                        rc.build(dirToBuild, typeToBuild);
                        return TASK_COMPLETE;
                    } else {
                        // Rotate the direction to try
                        dirToBuild = dirToBuild.rotateLeft();
                    }
                }
            }
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
        return TASK_IN_PROGRESS;
    }

    // Retreats to a given target location until no enemies in sight
    // If target location is null, moves in the opposite direction from the closest enemy
    public static int retreatToLocation(RobotController rc, MapInfo mapInfo, MapLocation targetLocation) {
        try {
            rc.setIndicatorString(0, "retreating");
            RobotInfo[] hostileBots = rc.senseHostileRobots(mapInfo.selfLoc, mapInfo.selfSenseRadiusSq);
            if (hostileBots.length == 0) {
                return TASK_COMPLETE;
            } else if (targetLocation != null) {
                return moveToLocation(rc, mapInfo, targetLocation, TASK_RETREATING);
            } else {
                for (RobotInfo info : hostileBots) {
                    Direction oppositeDir = mapInfo.selfLoc.directionTo(info.location).opposite();
                    if (rc.canMove(oppositeDir)) {
                        rc.move(oppositeDir);
                        return TASK_IN_PROGRESS;
                    } else if (rc.canMove(oppositeDir.rotateRight())) {
                        rc.move(oppositeDir.rotateRight());
                        return TASK_IN_PROGRESS;
                    } else if (rc.canMove(oppositeDir.rotateLeft())) {
                        rc.move(oppositeDir.rotateLeft());
                        return TASK_IN_PROGRESS;
                    }
                }
            }
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
        return TASK_IN_PROGRESS;
    }
}
