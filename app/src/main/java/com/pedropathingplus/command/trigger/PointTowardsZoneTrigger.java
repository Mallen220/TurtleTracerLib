package com.pedropathingplus.command.trigger;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;

/**
 * A trigger that is active when the robot's heading points towards a specific target point on the field,
 * within a given tolerance.
 */
public class PointTowardsZoneTrigger extends Trigger {

    /**
     * Constructs a PointTowardsZoneTrigger.
     *
     * @param follower          The follower providing the current robot pose.
     * @param targetPoint       The target coordinate to point towards.
     * @param toleranceRadians  The acceptable angle difference in radians.
     */
    public PointTowardsZoneTrigger(Follower follower, Pose targetPoint, double toleranceRadians) {
        super(() -> {
            Pose currentPose = follower.getPose();
            double targetAngle = Math.atan2(targetPoint.getY() - currentPose.getY(), targetPoint.getX() - currentPose.getX());
            double currentAngle = currentPose.getHeading();

            // Normalize angles to be between -PI and PI to find the shortest distance
            double angleDifference = Math.abs(normalizeAngle(targetAngle - currentAngle));

            return angleDifference <= toleranceRadians;
        });
    }

    private static double normalizeAngle(double angle) {
        double result = angle;
        while (result > Math.PI) result -= 2 * Math.PI;
        while (result < -Math.PI) result += 2 * Math.PI;
        return result;
    }
}
