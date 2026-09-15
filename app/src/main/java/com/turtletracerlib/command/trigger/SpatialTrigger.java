package com.turtletracerlib.command.trigger;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;

/**
 * Spatial triggers that activate based on the robot's physical location on the field.
 * @deprecated Marked for removal.
 */
@Deprecated
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
            Pose currentPose = follower.pose();
            double distance = Math.hypot(currentPose.x() - targetPose.x(), currentPose.y() - targetPose.y());
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
            Pose currentPose = follower.pose();
            double x = currentPose.x();
            double y = currentPose.y();

            return x >= minPose.x() && x <= maxPose.x() &&
                   y >= minPose.y() && y <= maxPose.y();
        });
    }

    private SpatialTrigger(java.util.function.BooleanSupplier condition) {
        super(condition);
    }
}
