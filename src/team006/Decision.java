package team006;

import java.util.Random;
import battlecode.common.*;

/**
 * Created by andrewalbers on 9/15/16.
 */
public class Decision {

    public static Random rand = new Random(1);

    public static boolean doCollectParts(RobotController rc) {
        if ( rc.getTeamParts() < 100 ) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean doRunAway(RobotController rc, MapInfo mapInfo) {
        return ( mapInfo.hostileRobots.length > 0);
    }

    // return index of robot type to build from Constants.ROBOT_TYPES
    public static int botToBuild(RobotController rc, MapInfo mapInfo) {
        if (mapInfo.roundNum == 0) {
            return 0;
        } else if (mapInfo.timeTillSpawn < 50) {
            return 2; // build guards if zombies to spawn soon
        }
        return 1;
    }

    public static MapLocation getBestPartLocation(RobotController rc, MapInfo mapInfo) {
        MapLocation bestPartLoc = null;
        return null;
//        int minDist = 5 + 100 / (int)(rc.getTeamParts() + 1); // allow more room to search if team is low on parts
//        for (MapLocation partLoc : mapInfo.partLocations.keySet()) {
//            int dist = MapInfo.moveDist(mapInfo.selfLoc, partLoc);
//            if (dist < minDist) {
//                bestPartLoc = partLoc;
//                minDist = dist;
//            }
//        }
//        return bestPartLoc;
    }

    public static MapLocation getBestNeutralLocation(MapInfo mapInfo) {
        MapLocation bestNeutralLoc = null;
        int minDist = 10;
        for (MapLocation neutralLoc : mapInfo.neutralLocations.keySet()) {
            int dist = MapInfo.moveDist(mapInfo.selfLoc, neutralLoc);
            if (dist < minDist) {
                bestNeutralLoc = neutralLoc;
                minDist = dist;
            }
        }
        return bestNeutralLoc;
    }
}
