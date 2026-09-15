package com.turtletracerlib.pathing.event;

import com.pedropathing.math.Pose;
import java.util.function.Supplier;

/**
 * An event that triggers when the robot's physical location reaches a target area or comes within
 * a specified radius of a target {@link Pose}.
 */
public class SpatialEvent extends PathEvent {

    private final Supplier<Pose> poseSupplier;
    private final Pose targetPose;
    private final double radius;

    /**
     * Constructs a SpatialEvent that triggers when within a radius of the target pose.
     *
     * @param poseSupplier Supplier providing current robot pose.
     * @param targetPose   Target pose on the field.
     * @param radius       Proximity tolerance distance (in inches).
     * @param action       The action to execute.
     */
    public SpatialEvent(Supplier<Pose> poseSupplier, Pose targetPose, double radius, Runnable action) {
        super(action);
        this.poseSupplier = poseSupplier;
        this.targetPose = targetPose;
        this.radius = radius;
    }

    @Override
    public boolean isConditionMet() {
        if (poseSupplier == null || targetPose == null) {
            return false;
        }
        Pose current = poseSupplier.get();
        if (current == null) {
            return false;
        }
        double distance = Math.hypot(current.x() - targetPose.x(), current.y() - targetPose.y());
        return distance <= radius;
    }

    /**
     * Gets the target pose for this event.
     *
     * @return Target pose.
     */
    public Pose getTargetPose() {
        return targetPose;
    }

    /**
     * Gets the radius tolerance for this event.
     *
     * @return Radius in inches.
     */
    public double getRadius() {
        return radius;
    }
}
