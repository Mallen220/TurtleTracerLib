package com.turtletracerlib.pathing;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.api.Paths;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Localizer;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Twist;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathTracker;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class ProgressTrackerTest {

    private Follower follower;
    private ProgressTracker tracker;
    private boolean isBusy = false;
    private double currentCompletion = 0.0;
    private double currentParametric = 0.0;

    @Before
    public void setUp() {
        Localizer localizer = new Localizer() {
            private Pose pose = new Pose(0, 0, 0);
            @Override public void setPose(Pose p) { this.pose = p; }
            @Override public MotionState state() { return MotionState.ofTwist(pose, Twist.zero()); }
            @Override public void update() {}
            @Override public void reset() {}
        };

        Drivetrain drivetrain = new Drivetrain() {
            @Override public void drive(DrivePowers powers, boolean manual) {}
            @Override public double maxScaling(DrivePowers current, DrivePowers delta) { return 1.0; }
            @Override public void stop() {}
            @Override public void stop(boolean brake) {}
            @Override public Map<String, Object> debug() { return Collections.emptyMap(); }
            @Override public double interpolateVelocity(double xRadius, double yRadius, double theta) { return 1.0; }
        };

        Algorithm algorithm = new Algorithm() {
            @Override public DrivePowers calculatePath(Drivetrain d, PathTracker p, MotionState s, double dt) { return DrivePowers.zero(); }
            @Override public DrivePowers calculateHold(Drivetrain d, Pose target, MotionState s, boolean useScaling, double dt) { return DrivePowers.zero(); }
            @Override public double completion() { return currentCompletion; }
            @Override public Pose closestPose() { return new Pose(0, 0, 0); }
            @Override public Vector2D closestTangent() { return Vector2D.iHat(); }
            @Override public Vector2D closestNormal() { return Vector2D.jHat(); }
            @Override public double curvature() { return 0; }
            @Override public double remainingDistance() { return 0; }
            @Override public double parametricCompletion() { return currentParametric; }
            @Override public boolean atParametricEnd() { return false; }
            @Override public void reset() { isBusy = true; }
            @Override public boolean isBusy() { return isBusy; }
            @Override public Map<String, Object> debug() { return Collections.emptyMap(); }
        };

        follower = new Follower(localizer, drivetrain, algorithm);
        tracker = new ProgressTracker(follower, null);
    }

    @Test
    public void testEventRegistrationAndTrigger() {
        NamedCommands.clearAllCommands();
        final boolean[] triggered = {false};
        NamedCommands.registerCommand("action", () -> { triggered[0] = true; });

        Path path = Paths.line(new Pose(0, 0, 0), new Pose(10, 10, 0)).constant(0);
        follower.follow(path);
        tracker.setCurrentPath(path);

        tracker.registerEvent("action", 0.5);
        assertFalse(tracker.isEventTriggered("action"));

        currentParametric = 0.3;
        currentCompletion = 0.3;
        assertFalse(tracker.shouldTriggerEvent("action"));

        currentParametric = 0.6;
        currentCompletion = 0.6;
        assertTrue(tracker.shouldTriggerEvent("action"));

        tracker.executeEvent("action");
        assertTrue(triggered[0]);
        assertTrue(tracker.isEventTriggered("action"));
        assertFalse(tracker.shouldTriggerEvent("action"));
    }

    @Test
    public void testTurnAndStop() {
        follower.setPose(new Pose(0, 0, 0));
        tracker.turn(Math.PI / 2, "turnEvent", 0.5);

        isBusy = true;
        assertTrue(tracker.isBusy());

        tracker.stop();
        assertTrue(follower.idle());
    }

    @Test
    public void testTotalProgressAndSetCurrentPath() {
        Path path = Paths.line(new Pose(0, 0, 0), new Pose(10, 0, 0)).constant(0);
        follower.follow(path);
        tracker.setCurrentPath(path);
        tracker.registerEvent("evt", 0.8);
        assertFalse(tracker.isEventTriggered("evt"));

        assertEquals(0.0, tracker.getTotalProgress(), 1e-4);
        assertEquals(0.0, tracker.getCompletion(), 1e-4);
        assertEquals(0.0, tracker.getChainProgress(), 1e-4);
    }
}
