package team006;

import java.util.Random;
import battlecode.common.*;

/**
 * Created by andrewalbers on 9/15/16.
 */
public class Decision {

    public static Random rand = new Random(1);

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
}
