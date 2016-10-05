package team006;

import battlecode.common.*;

import java.awt.*;

/**
 * Created by andrewalbers on 9/14/16.
 */
public class SignalManager {
    public static int SIG_ASSIST = 1;
    public static int SIG_UPDATE_ARCHON_LOC = 2;
    public static int SIG_SCOUT_DENS = 3;
    public static int SIG_SCOUT_NEUTRALS = 4;
    public static int SIG_SCOUT_PARTS = 5;
    public static int SIG_TEAM_ATTACK = 6;
    public static int SIG_SCOUT_OPPONENT = 7;
    public static int SIG_SCOUT_ZOMBIE = 8;

    public static void requestHelp(RobotController rc, MapInfo mapInfo, MapLocation location) {
        try {
            if (mapInfo.selfType == RobotType.ARCHON) {
                rc.broadcastMessageSignal(SIG_ASSIST, encodeLocation(mapInfo.selfLoc, location), 1000);
            } else {
                rc.broadcastSignal(400);
            }
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
    }

    public static void signalArchonLoc(RobotController rc, MapInfo mapInfo) {
        try {
            rc.broadcastMessageSignal(SIG_UPDATE_ARCHON_LOC, encodeLocation(mapInfo.selfLoc, mapInfo.selfLoc), 1000);
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
    }

    public static void signalAssemble(RobotController rc, MapInfo mapInfo) {
        try {
            int avgX = mapInfo.selfLoc.x; // archonLocations doesn't include self, so start summing with rc's x and y
            int avgY = mapInfo.selfLoc.y;
            for (MapLocation archLoc : mapInfo.archonLocations.values()){
                avgX += archLoc.x;
                avgY += archLoc.y;
            }
            mapInfo.teamAttackSignalRound = mapInfo.roundNum;
            MapLocation avgArchonLoc = new MapLocation(avgX / (mapInfo.archonLocations.size() + 1), avgY / (mapInfo.archonLocations.size() + 1));
            rc.broadcastMessageSignal(SIG_TEAM_ATTACK, encodeLocation(mapInfo.selfLoc, avgArchonLoc), 10000);
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
    }

    public static void scoutEnemies(RobotController rc, MapInfo mapInfo, RobotInfo[] enemies) {
        try {
            MapLocation zombieLoc = null;
            MapLocation opponentLoc = null;
            for (RobotInfo info : enemies) {
                if (info.type == RobotType.ZOMBIEDEN) {
                    rc.broadcastMessageSignal(SIG_SCOUT_DENS, encodeLocation(mapInfo.selfLoc, info.location), 1000);
                    rc.setIndicatorString(2, "Sent Zombie Den message");
                    return;
                } else if (info.team.equals(Team.ZOMBIE) == false){
                    opponentLoc = info.location;
                } else {
                    zombieLoc = info.location;
                }
            }
            if (zombieLoc != null){
                rc.broadcastMessageSignal(SIG_SCOUT_ZOMBIE, encodeLocation(mapInfo.selfLoc, zombieLoc), 1000);
                rc.setIndicatorString(2, "Sent Zombie Location message");
            } else if (opponentLoc != null){
                rc.broadcastMessageSignal(SIG_SCOUT_OPPONENT, encodeLocation(mapInfo.selfLoc, opponentLoc), 1000);
                rc.setIndicatorString(2, "Sent Opponent Location message");
            }
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
    }

    public static void scoutResources(RobotController rc, MapInfo mapInfo, MapLocation[] partLocations, RobotInfo[] neutrals) {
        try {
            rc.broadcastMessageSignal(SIG_SCOUT_NEUTRALS,neutrals.length,1000);
            int parts = 0;
            for (MapLocation partLoc : partLocations) {
                parts += rc.senseParts(partLoc);
            }
            rc.broadcastMessageSignal(SIG_SCOUT_PARTS,parts,1000);
        } catch (GameActionException gae) {
            System.out.println(gae.getMessage());
            gae.printStackTrace();
        }
    }

    // encodes a map location relative to the broadcaster's location in a single int
    // 0206 : location is +2 x and +6 y from broadcaster's location
    // 1206 : location is -2 x and +6 y from broadcaster's location
    public static int encodeLocation(MapLocation rcLoc, MapLocation targetLoc) {
        int diffX = targetLoc.x - rcLoc.x;
        int diffY = targetLoc.y - rcLoc.y;
        int locSignal = Math.abs(diffX) + (100 * Math.abs(diffY));
        locSignal += diffX < 0 ? 10 : 0;
        locSignal += diffY < 0 ? 1000 : 0;
        return locSignal;
    }

    public static MapLocation decodeLocation(MapLocation rcLoc, int locSignal) {
        int signal = locSignal;
        int diffX = locSignal / 1000 == 1 ? -1 : 1;
        signal = signal % 1000;
        diffX = diffX * ( signal / 100 );
        signal = signal % 100;
        int diffY = signal / 10 == 1 ? -1 : 1;
        signal = signal % 10;
        diffY = diffY * signal;
        return new MapLocation(rcLoc.x + diffX, rcLoc.y + diffY);
    }
}
