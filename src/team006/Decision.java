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
}
