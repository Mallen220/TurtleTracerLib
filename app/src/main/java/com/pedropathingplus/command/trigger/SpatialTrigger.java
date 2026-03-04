package com.pedropathingplus.command.trigger;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;

/**
 * Spatial triggers that activate based on the robot's physical location on the field.
 */
public class SpatialTrigger extends Trigger {

    /**
     * Creates a trigger that activates when the robot is within a given radius of a target pose.
     *
     * @param follower   The follower providing the current robot pose.
     * @param targetPose The target coordinates.
     * @param radius     The acceptable distance radius.
     * @return A new trigger that is active when near the target position.
     */
    public static SpatialTrigger nearFieldPosition(Follower follower, Pose targetPose, double radius) {
        return new SpatialTrigger(() -> {
            Pose currentPose = follower.getPose();
            double distance = Math.hypot(currentPose.getX() - targetPose.getX(), currentPose.getY() - targetPose.getY());
            return distance <= radius;
        });
    }

    /**
     * Creates a trigger that activates when the robot is within a defined rectangular area on the field.
     *
     * @param follower The follower providing the current robot pose.
     * @param minPose  The bottom-left (minimum X, minimum Y) corner of the area.
     * @param maxPose  The top-right (maximum X, maximum Y) corner of the area.
     * @return A new trigger that is active when inside the defined area.
     */
    public static SpatialTrigger inFieldArea(Follower follower, Pose minPose, Pose maxPose) {
        return new SpatialTrigger(() -> {
            Pose currentPose = follower.getPose();
            double x = currentPose.getX();
            double y = currentPose.getY();

            return x >= minPose.getX() && x <= maxPose.getX() &&
                   y >= minPose.getY() && y <= maxPose.getY();
        });
    }

    private SpatialTrigger(java.util.function.BooleanSupplier condition) {
        super(condition);
    }
}
