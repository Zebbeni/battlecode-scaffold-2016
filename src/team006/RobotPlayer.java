package team006;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {

        Random rand = new Random(rc.getID());

        int taskStatus = RobotTasks.TASK_NOT_GIVEN;
        Assignment assignment = null;

        MapInfo mapInfo = new MapInfo(rc);

        while (true) {
            // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
            // at the end of it, the loop will iterate once per game round.
            try {
                if (rc.isCoreReady()) {

                    mapInfo.updateAll(rc);

                    rc.setIndicatorString(0,"dens: " + mapInfo.denLocations.size());

                    if (mapInfo.urgentSignal != null){
                        Assignment newAssignment = AssignmentManager.getSignalAssignment(rc, mapInfo, mapInfo.urgentSignal, assignment);
                        if (newAssignment != null) {
                            assignment = newAssignment;
                            mapInfo.clearHasBeenLocations();
                            taskStatus = RobotTasks.TASK_IN_PROGRESS;
                        }
                    } else if ( taskStatus != RobotTasks.TASK_IN_PROGRESS && taskStatus != RobotTasks.TASK_ATTACKING && taskStatus != RobotTasks.TASK_RETREATING) {
                        assignment = AssignmentManager.getAssignment(rc, rand, mapInfo);
                        mapInfo.clearHasBeenLocations();
                        taskStatus = RobotTasks.TASK_IN_PROGRESS;
                    }

                    if (taskStatus != RobotTasks.TASK_ATTACKING || taskStatus != RobotTasks.TASK_RETREATING) {
                        mapInfo.incrementHasBeenOnCurrent();
                    }

                    taskStatus = RobotTasks.pursueTask(rc, mapInfo, assignment);

                    if (mapInfo.selfType == RobotType.ARCHON && mapInfo.roundNum - mapInfo.selfLastSignaled > 20) {
                        SignalManager.signalArchonLoc(rc, mapInfo);
                        mapInfo.selfLastSignaled = mapInfo.roundNum;
                    }
                    if (taskStatus == RobotTasks.TASK_SIGNALED) {
                        mapInfo.selfLastSignaled = mapInfo.roundNum;
                    } else if (taskStatus == RobotTasks.TASK_COMPLETE) {
                        mapInfo.handleTaskComplete(assignment);
                    }
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
