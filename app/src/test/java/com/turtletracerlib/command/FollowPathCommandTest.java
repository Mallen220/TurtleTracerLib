package com.turtletracerlib.command;

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

public class FollowPathCommandTest {

    private Follower follower;
    private boolean busy = false;

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
            @Override public double completion() { return 0.0; }
            @Override public Pose closestPose() { return new Pose(0, 0, 0); }
            @Override public Vector2D closestTangent() { return Vector2D.iHat(); }
            @Override public Vector2D closestNormal() { return Vector2D.jHat(); }
            @Override public double curvature() { return 0; }
            @Override public double remainingDistance() { return 0; }
            @Override public double parametricCompletion() { return 0; }
            @Override public boolean atParametricEnd() { return false; }
            @Override public void reset() { busy = true; }
            @Override public boolean isBusy() { return busy; }
            @Override public Map<String, Object> debug() { return Collections.emptyMap(); }
        };

        follower = new Follower(localizer, drivetrain, algorithm);
    }

    @Test
    public void testPreBuiltPathCommand() {
        Path path = Paths.line(new Pose(0, 0, 0), new Pose(10, 10, 0)).constant(0);
        FollowPathCommand cmd = new FollowPathCommand(follower, path, true);

        cmd.initialize();
        assertTrue(follower.following());
        assertTrue(follower.holdEnd.get());

        assertFalse(cmd.isFinished());
        busy = false;
        assertTrue(cmd.isFinished());

        cmd.end(true);
        assertTrue(follower.idle());
    }

    @Test
    public void testFluentBuilderPathCommand() {
        FollowPathCommand cmd = new FollowPathCommand(follower)
                .addPath(Paths.line(new Pose(0, 0, 0), new Pose(10, 0, 0)))
                .setConstantHeadingInterpolation(Math.PI / 2);

        cmd.initialize();
        assertTrue(follower.following());

        cmd.end(false);
    }

    @Test
    public void testCurveThroughFluentBuilder() {
        FollowPathCommand cmd = new FollowPathCommand(follower)
                .curveThrough(new Pose(0, 0, 0), new Pose(5, 5, 0), new Pose(10, 10, 0))
                .setTangentHeadingInterpolation();

        cmd.initialize();
        assertTrue(follower.following());
        cmd.end(false);
    }
}
