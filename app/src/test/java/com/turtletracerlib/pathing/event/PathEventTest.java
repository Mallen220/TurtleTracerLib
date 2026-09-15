package com.turtletracerlib.pathing.event;

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
import com.turtletracerlib.command.FollowPathCommand;
import com.turtletracerlib.pathing.ProgressTracker;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PathEventTest {

    private Follower follower;
    private Pose currentPose;
    private double parametricT = 0.0;
    private double overallCompletion = 0.0;
    private boolean busy = false;

    @Before
    public void setUp() {
        currentPose = new Pose(0, 0, 0);

        Localizer localizer = new Localizer() {
            @Override public void setPose(Pose p) { currentPose = p; }
            @Override public MotionState state() { return MotionState.ofTwist(currentPose, Twist.zero()); }
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
            @Override public double completion() { return overallCompletion; }
            @Override public Pose closestPose() { return currentPose; }
            @Override public Vector2D closestTangent() { return Vector2D.iHat(); }
            @Override public Vector2D closestNormal() { return Vector2D.jHat(); }
            @Override public double curvature() { return 0; }
            @Override public double remainingDistance() { return 0; }
            @Override public double parametricCompletion() { return parametricT; }
            @Override public boolean atParametricEnd() { return parametricT >= 1.0; }
            @Override public void reset() { busy = true; }
            @Override public boolean isBusy() { return busy; }
            @Override public Map<String, Object> debug() { return Collections.emptyMap(); }
        };

        follower = new Follower(localizer, drivetrain, algorithm);
    }

    @Test
    public void testParametricEventThreshold() {
        AtomicBoolean ran = new AtomicBoolean(false);
        ParametricEvent event = new ParametricEvent(() -> parametricT, 0.7, () -> ran.set(true));

        parametricT = 0.5;
        assertFalse(event.isConditionMet());
        event.update();
        assertFalse(ran.get());

        parametricT = 0.75;
        assertTrue(event.isConditionMet());
        event.update();
        assertTrue(ran.get());
        assertTrue(event.hasTriggered());

        // Second update does not re-run
        ran.set(false);
        event.update();
        assertFalse(ran.get());
    }

    @Test
    public void testParametricEventRange() {
        AtomicBoolean ran = new AtomicBoolean(false);
        ParametricEvent event = new ParametricEvent(() -> parametricT, 0.3, 0.6, () -> ran.set(true));

        parametricT = 0.2;
        assertFalse(event.isConditionMet());

        parametricT = 0.4;
        assertTrue(event.isConditionMet());
        event.update();
        assertTrue(ran.get());
    }

    @Test
    public void testTemporalEvent() {
        AtomicBoolean ran = new AtomicBoolean(false);
        TemporalEvent event = new TemporalEvent(50, () -> ran.set(true));

        assertFalse(event.isConditionMet());
        event.start();
        assertTrue(event.isStarted());

        assertFalse(event.isConditionMet());
        event.update();
        assertFalse(ran.get());

        try {
            Thread.sleep(60);
        } catch (InterruptedException ignored) {}

        assertTrue(event.isConditionMet());
        event.update();
        assertTrue(ran.get());
    }

    @Test
    public void testSpatialEventProximity() {
        AtomicBoolean ran = new AtomicBoolean(false);
        Pose target = new Pose(10, 10, 0);
        SpatialEvent event = new SpatialEvent(() -> currentPose, target, 2.0, () -> ran.set(true));

        currentPose = new Pose(5, 5, 0);
        assertFalse(event.isConditionMet());
        event.update();
        assertFalse(ran.get());

        currentPose = new Pose(9, 10, 0);
        assertTrue(event.isConditionMet());
        event.update();
        assertTrue(ran.get());
    }

    @Test
    public void testProgressTrackerEventsIntegration() {
        ProgressTracker tracker = new ProgressTracker(follower, null);
        Path path = Paths.line(new Pose(0, 0, 0), new Pose(20, 20, 0)).constant(0);
        follower.follow(path);
        tracker.setCurrentPath(path);

        AtomicInteger parametricCount = new AtomicInteger(0);
        AtomicInteger namedCount = new AtomicInteger(0);
        AtomicInteger spatialCount = new AtomicInteger(0);

        tracker.onParametric(0.5, parametricCount::incrementAndGet);
        tracker.onEvent("IntakeMarker", namedCount::incrementAndGet);
        tracker.registerEvent("IntakeMarker", 0.6);
        tracker.onSpatial(new Pose(15, 15, 0), 2.0, spatialCount::incrementAndGet);

        parametricT = 0.2;
        tracker.update();
        assertEquals(0, parametricCount.get());
        assertEquals(0, namedCount.get());
        assertEquals(0, spatialCount.get());

        parametricT = 0.55;
        tracker.update();
        assertEquals(1, parametricCount.get());
        assertEquals(0, namedCount.get());

        parametricT = 0.65;
        tracker.update();
        assertEquals(1, namedCount.get());

        currentPose = new Pose(14.5, 15.0, 0);
        tracker.update();
        assertEquals(1, spatialCount.get());
    }

    @Test
    public void testFollowPathCommandEvents() {
        Path path = Paths.line(new Pose(0, 0, 0), new Pose(10, 10, 0)).constant(0);
        AtomicBoolean parametricTriggered = new AtomicBoolean(false);
        AtomicBoolean spatialTriggered = new AtomicBoolean(false);

        FollowPathCommand cmd = new FollowPathCommand(follower, path)
                .onParametric(0.8, () -> parametricTriggered.set(true))
                .onSpatial(new Pose(10, 10, 0), 1.0, () -> spatialTriggered.set(true));

        cmd.initialize();

        parametricT = 0.4;
        cmd.execute();
        assertFalse(parametricTriggered.get());

        parametricT = 0.85;
        cmd.execute();
        assertTrue(parametricTriggered.get());

        currentPose = new Pose(10, 10.5, 0);
        cmd.execute();
        assertTrue(spatialTriggered.get());

        cmd.end(false);
    }
}
