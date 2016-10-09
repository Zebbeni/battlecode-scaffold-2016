package team006;

import battlecode.common.*;


/**
 * Created by andrewalbers on 9/14/16.
 */
public class RobotTasks {

    public static int TASK_NOT_GIVEN = -1;
    public static int TASK_IN_PROGRESS = 0;
    public static int TASK_COMPLETE = 1;
    public static int TASK_ABANDONED = 2;
//    public static int TASK_SIGNALED = 3;
    public static int TASK_ATTACKING = 4;
    public static int TASK_RETREATING = 5;

    public static int pursueTask(RobotController rc, MapInfo mapInfo, Assignment assignment) {
        try {
            String targetLoc = assignment.targetLocation == null ? "null" : assignment.targetLocation.x + ", " + assignment.targetLocation.y;

            if ( assignment.assignmentType == AssignmentManager.ARCHON_ACTION ) {
                rc.setIndicatorString(0, "doing archon action");
                return archonAction(rc, mapInfo);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_MOVE_TO_LOC ) {
                rc.setIndicatorString(0, "moving to location: " + targetLoc);
                return moveToLocation(rc, mapInfo, assignment.targetLocation, TASK_IN_PROGRESS);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_ATTACK_MOVE_TO_LOC ){
                rc.setIndicatorString(0, "attack moving to location: " + targetLoc);
                return attackMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_TIMID_MOVE_TO_LOC ){
                rc.setIndicatorString(0, "timid moving to location: " + targetLoc);
                return timidMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_RUN_AWAY ){
                rc.setIndicatorString(0, "running away to: " + targetLoc);
                return retreat(rc, mapInfo);
            } else if ( assignment.assignmentType == AssignmentManager.BOT_PATROL ){
                rc.setIndicatorString(0, "patrolling to location: " + targetLoc);
                return attackMoveToLocation(rc, mapInfo, assignment.targetLocation);
            } else if (assignment.assignmentType == AssignmentManager.BOT_TURRET_DEFEND) {
                rc.setIndicatorString(0, "defending location: " + targetLoc);
                return turretDefend(rc, mapInfo);
            } else if (assignment.assignmentType == AssignmentManager.BOT_SCOUT) {
                rc.setIndicatorString(0, "scouting location: " + targetLoc);
                return scoutLocation(rc, mapInfo, assignment.targetLocation);
            } else if (assignment.assignmentType == AssignmentManager.BOT_KILL_DEN) {
                rc.setIndicatorString(0, "attacking zombie den at: " + targetLoc);
                return attackZombieDen(rc, mapInfo, assignment.targetLocation);
            } else if (assignment.assignmentType == AssignmentManager.BOT_ASSIST_LOC) {
                rc.setIndicatorString(0, "assisting at location: " + targetLoc);
                return assistLocation(rc, mapInfo, assignment.targetLocation);
            } else if (assignment.assignmentType == AssignmentManager.BOT_ASSEMBLE_TO_LOC){
                rc.setIndicatorString(0, "assembling to location: " + targetLoc);
                return assembleToLocation(rc, mapInfo, assignment.targetLocation);
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
                                thisScore -= evalLocation.distanceSquaredTo(targetLocation);
                            } else {
                                thisScore += evalLocation.distanceSquaredTo(targetLocation);
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
                        rc.setIndicatorString(1, "moving to location: " + targetLocation.x + ", " + targetLocation.y);
                        rc.move(dirToMove);
                        return task;
                    } else {
                        rc.setIndicatorString(1, "clearing rubble");
                        rc.clearRubble(dirToMove);
                        return task;
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

            mapInfo.scoutRoundsTraveled++;

            if (mapInfo.hostileRobots.length > 0 && mapInfo.roundNum - mapInfo.selfLastSignaled > 5) {
                SignalManager.scoutEnemies(rc, mapInfo, mapInfo.hostileRobots);
                return TASK_IN_PROGRESS;
            } else if (mapInfo.selfLoc.equals(targetLocation) || mapInfo.scoutRoundsTraveled > 20) {
                SignalManager.scoutResources(rc, mapInfo, rc.sensePartLocations(mapInfo.selfSenseRadiusSq), rc.senseNearbyRobots(mapInfo.selfSenseRadiusSq, Team.NEUTRAL));
                return TASK_COMPLETE;
            } else {
                return moveToLocation(rc, mapInfo, targetLocation, TASK_IN_PROGRESS);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            rc.setIndicatorString(2,e.getMessage());
            e.printStackTrace();
        }
        return TASK_ABANDONED;
    }

    // just use attackMoveToLocation, since
    public static int turretDefend(RobotController rc, MapInfo mapInfo){
        try {
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
            if (rc.canSenseLocation(targetLocation)) {
                RobotInfo denLocBot = rc.senseRobotAtLocation(targetLocation);
                if (denLocBot == null || denLocBot.type != RobotType.ZOMBIEDEN) {
                    mapInfo.updateZombieDens(targetLocation, false);
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
            MapLocation moveTarget = targetLocation;

            if (moveTarget == null) {
                moveTarget = new MapLocation(mapInfo.selfLoc.x + mapInfo.rand.nextInt(13) - 6, mapInfo.selfLoc.y + mapInfo.rand.nextInt(13) - 6);
            }

            if (mapInfo.hostileRobots.length > 0) {
                if (mapInfo.roundNum - mapInfo.selfLastSignaled > 50) {
                    SignalManager.requestHelp(rc, mapInfo, mapInfo.selfLoc);
                    rc.setIndicatorString(2, "selfLastSignaled: " + mapInfo.selfLastSignaled);
                    return TASK_IN_PROGRESS;
                } else {
                    return doAttackLogic(rc, mapInfo);
                }
            } else if (mapInfo.selfType == RobotType.TURRET) {
                // no enemies found, keep on sitting there
                rc.setIndicatorString(1, "watching for enemies");
                return TASK_IN_PROGRESS;
            } else if (mapInfo.selfLoc.distanceSquaredTo(moveTarget) > 8) {
                rc.setIndicatorString(1, "moving toward target location");
                return moveToLocation(rc, mapInfo, moveTarget, TASK_IN_PROGRESS);
            } else {
                rc.setIndicatorString(1, "attack move task complete!");
                return TASK_COMPLETE;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
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
                            return TASK_IN_PROGRESS;
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

            if ( mapInfo.isTimeToSignalTeamAttack()) {
                rc.setIndicatorString(2, "Group and Attack!!!!");
                SignalManager.signalAssemble(rc, mapInfo);
                return TASK_IN_PROGRESS;
            } else {
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

                if (closestNeutralLoc != null) {
                    return timidMoveToLocation(rc, mapInfo, closestNeutralLoc);
                }

                // if team parts under threshold, move toward best part location in sight (if valuable enough)
                double teamParts = rc.getTeamParts();
                MapLocation[] partLocations = rc.sensePartLocations(mapInfo.selfSenseRadiusSq);
                double bestScore = 50.0; // only pursue adjacent parts of 50+, parts 2 away of 200+
                MapLocation bestPartLocation = null;
                for (MapLocation partLoc : partLocations) {
                    int partDist = mapInfo.selfLoc.distanceSquaredTo(partLoc);
                    if (partDist != 0) {
                        double score = (200 * rc.senseParts(partLoc)) / (Math.pow(partDist, 2) * (teamParts + 1)); // add 1 so we don't divide by 0
                        if (score > bestScore) {
                            bestPartLocation = partLoc;
                            bestScore = score;
                        }
                    }
                }
                if (bestPartLocation != null) {
                    return timidMoveToLocation(rc, mapInfo, bestPartLocation);
                }

                // If we did none of the above stuff, try to make a robot
                int taskStatus = archonBuildRobot(rc, mapInfo);

                // If we couldn't make a new robot this round, check for adjacent friendly robots to repair
                if (taskStatus != TASK_COMPLETE){
                    RobotInfo[] adjacentFriends = rc.senseNearbyRobots(2, mapInfo.selfTeam);
                    for (RobotInfo adjacentFriend : adjacentFriends){
                        if (adjacentFriend.type != RobotType.ARCHON && adjacentFriend.health < adjacentFriend.type.maxHealth){
                            rc.repair(adjacentFriend.location);
                            return TASK_IN_PROGRESS;
                        }
                    }
                }

                return TASK_IN_PROGRESS;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            rc.setIndicatorString(2, e.getMessage());
            e.printStackTrace();
        }
        return TASK_ABANDONED;
    }

    public static int archonBuildRobot(RobotController rc, MapInfo mapInfo) {
        try{
            RobotType typeToBuild = Constants.ROBOT_TYPES[1]; // default SOLDIER

            if (mapInfo.needAnotherScout()) {
                typeToBuild = Constants.ROBOT_TYPES[0]; // build SCOUT every so many rounds
            } else if (mapInfo.isTimeForVipers()) {
                typeToBuild = Constants.ROBOT_TYPES[3]; // Build VIPER
            } else if (mapInfo.rand.nextInt(100) < 30) {
                typeToBuild = Constants.ROBOT_TYPES[2]; // GUARDS are 30% of regular troops
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
                        if (typeToBuild == Constants.ROBOT_TYPES[3]){
                            mapInfo.vipersCreated += 1;
                        } else if (typeToBuild == Constants.ROBOT_TYPES[0]) {
                            mapInfo.scoutsCreated += 1;
                        }
                        rc.setIndicatorString(1, "scouts: " + mapInfo.scoutsCreated + " vipers: " + mapInfo.vipersCreated);
                        rc.setIndicatorString(2, "Build task Complete!");
                        return TASK_COMPLETE;
                    }
                }
                return TASK_ABANDONED;
            } else {
                return TASK_IN_PROGRESS;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            rc.setIndicatorString(2, e.getMessage());
            e.printStackTrace();
        }
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
                    int dist = mapInfo.selfLoc.distanceSquaredTo(info.location);
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

    public static int assembleToLocation(RobotController rc, MapInfo mapInfo, MapLocation targetLocation){
        try {
            if (mapInfo.selfLoc.distanceSquaredTo(targetLocation) < 5 || mapInfo.roundNum > mapInfo.teamAttackSignalRound + 50) {
                return TASK_COMPLETE;
            } else {
                return moveToLocation(rc, mapInfo, targetLocation, TASK_IN_PROGRESS);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        rc.setIndicatorString(2, "Uh oh, abandoned retreating");
        return TASK_ABANDONED;
    }

    public static int doAttackLogic(RobotController rc, MapInfo mapInfo) {
        try {
            double minThreatEffort = 99999;
            double minNonThreatEffort = 99999;
            int minRange = mapInfo.selfType == RobotType.TURRET ? 5 : 0;
            MapLocation attackLoc;
            MapLocation threatLoc = null;
            MapLocation nonThreatLoc = null; // location of nearest enemy archon or den

            double thisEffort;
            // find and attack closest enemy bot

            double selfHealthPercent = mapInfo.selfHealth / mapInfo.selfType.maxHealth;

            boolean isOpponentHere = rc.senseNearbyRobots(mapInfo.selfSenseRadiusSq,mapInfo.opponentTeam).length > 0;

            for (RobotInfo info : mapInfo.hostileRobots) {
                if (!isOpponentHere || info.team.equals(mapInfo.opponentTeam)) {
                    // Find optimal attack location
                    int thisDist = mapInfo.selfLoc.distanceSquaredTo(info.location);
                    if (thisDist > minRange) {
                        if (info.type.canAttack() == false) {
                            // only consider non-threat targets if they are near the actual target location
                            // ie. don't stop to shoot dens if on the way to help another fighter
                            if (info.type == RobotType.ZOMBIEDEN) {
                                mapInfo.updateZombieDens(info.location, true);
                            }

                            thisEffort = thisDist * info.health;

                            if (thisEffort < minNonThreatEffort) {
                                minNonThreatEffort = thisEffort;
                                nonThreatLoc = info.location;
                            }
                        } else {

                            // only consider distance if rc is low health (target closest)
                            // thisEffort = selfHealthPercent > 0.33 ? thisDist * info.health / info.attackPower : thisDist;

                            thisEffort = thisDist * info.health / info.attackPower;

                            if (thisEffort < minThreatEffort) {
                                minThreatEffort = thisEffort;
                                threatLoc = info.location;
                            }
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
                        rc.setIndicatorString(1, "watching. no enemies in range");
                        return TASK_IN_PROGRESS;
                    } else if (mapInfo.selfAttackRadiusSq > 1) {
                        // see if we can move to a more optimal firing location
                        int currDist = mapInfo.selfLoc.distanceSquaredTo(attackLoc);
                        int maxDist = mapInfo.selfAttackRadiusSq;
                        Direction bestDir = null;
                        for (Direction dirToMove : Constants.DIRECTIONS) {
                            if (rc.canMove(dirToMove)) {
                                int newDist = mapInfo.selfLoc.add(dirToMove).distanceSquaredTo(attackLoc);
                                if (newDist <= maxDist && newDist > currDist) {
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
            return TASK_IN_PROGRESS;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            rc.setIndicatorString(2, e.getMessage());
            e.printStackTrace();
        }
        return TASK_ABANDONED;
    }
}
