package com.turtletracerlib;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Localizer;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Twist;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.PathTracker;
import com.turtletracerlib.pathing.ProgressTracker;
import com.turtletracerlib.pathing.event.PathEvent;
import com.turtletracerlib.pathing.event.SpatialEvent;
import com.turtletracerlib.pathing.event.TemporalEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class TurtleTracerReaderTest {

    private static final String SAMPLE_JSON = "{\n" +
            "  \"version\": \"2.2.1\",\n" +
            "  \"header\": {\n" +
            "    \"info\": \"Created with Turtle Tracer\",\n" +
            "    \"copyright\": \"Copyright 2026 Matthew Allen.\",\n" +
            "    \"link\": \"https://github.com/Mallen220/TurtleTracer\"\n" +
            "  },\n" +
            "  \"startPoint\": {\n" +
            "    \"x\": 9,\n" +
            "    \"y\": 25,\n" +
            "    \"heading\": \"linear\",\n" +
            "    \"locked\": false,\n" +
            "    \"startDeg\": 0,\n" +
            "    \"endDeg\": -180\n" +
            "  },\n" +
            "  \"lines\": [\n" +
            "    {\n" +
            "      \"id\": \"line-6tw9ehn50rd\",\n" +
            "      \"name\": \"DriveToShoot\",\n" +
            "      \"endPoint\": {\n" +
            "        \"x\": 60,\n" +
            "        \"y\": 25,\n" +
            "        \"heading\": \"linear\",\n" +
            "        \"startDeg\": 0,\n" +
            "        \"endDeg\": 90\n" +
            "      },\n" +
            "      \"controlPoints\": [],\n" +
            "      \"eventMarkers\": []\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"line-fsumxs48om\",\n" +
            "      \"name\": \"dave\",\n" +
            "      \"endPoint\": {\n" +
            "        \"x\": 85,\n" +
            "        \"y\": 107,\n" +
            "        \"heading\": \"tangential\",\n" +
            "        \"reverse\": false\n" +
            "      },\n" +
            "      \"controlPoints\": [],\n" +
            "      \"eventMarkers\": [\n" +
            "        {\n" +
            "          \"id\": \"event-1789514363543-9jwtsxel6\",\n" +
            "          \"name\": \"fdsag\",\n" +
            "          \"type\": \"parametric\",\n" +
            "          \"position\": 0.75,\n" +
            "          \"time\": 500,\n" +
            "          \"endTime\": 500,\n" +
            "          \"poseX\": 72,\n" +
            "          \"poseY\": 72,\n" +
            "          \"poseHeading\": 0,\n" +
            "          \"lineIndex\": 1\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"mu30bx3g-w1khiz\",\n" +
            "      \"name\": \"greg\",\n" +
            "      \"endPoint\": {\n" +
            "        \"x\": 68,\n" +
            "        \"y\": 39,\n" +
            "        \"heading\": \"linear\"\n" +
            "      },\n" +
            "      \"controlPoints\": [\n" +
            "        {\n" +
            "          \"x\": 86,\n" +
            "          \"y\": 50\n" +
            "        }\n" +
            "      ],\n" +
            "      \"eventMarkers\": []\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"mu30bmfb-bpsqv6\",\n" +
            "      \"name\": \"riley\",\n" +
            "      \"endPoint\": {\n" +
            "        \"x\": 64,\n" +
            "        \"y\": 89,\n" +
            "        \"heading\": \"linear\"\n" +
            "      },\n" +
            "      \"controlPoints\": [],\n" +
            "      \"eventMarkers\": [\n" +
            "        {\n" +
            "          \"id\": \"event-1789514364734-k2yxfc7fm\",\n" +
            "          \"name\": \"Shoot\",\n" +
            "          \"type\": \"temporal\",\n" +
            "          \"position\": 1,\n" +
            "          \"time\": 21838,\n" +
            "          \"endTime\": 21838,\n" +
            "          \"poseX\": 72,\n" +
            "          \"poseY\": 72,\n" +
            "          \"poseHeading\": 0,\n" +
            "          \"lineIndex\": 8\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"mu30cowh-upd1rd\",\n" +
            "      \"name\": \"josiah\",\n" +
            "      \"endPoint\": {\n" +
            "        \"x\": 99,\n" +
            "        \"y\": 44,\n" +
            "        \"heading\": \"constant\"\n" +
            "      },\n" +
            "      \"controlPoints\": [],\n" +
            "      \"eventMarkers\": [\n" +
            "        {\n" +
            "          \"id\": \"event-1789514365809-v4bft9sil\",\n" +
            "          \"name\": \"Intake\",\n" +
            "          \"type\": \"pose\",\n" +
            "          \"position\": 0.5,\n" +
            "          \"time\": 500,\n" +
            "          \"endTime\": 500,\n" +
            "          \"poseX\": 72,\n" +
            "          \"poseY\": 72,\n" +
            "          \"poseHeading\": 0,\n" +
            "          \"lineIndex\": 14\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"sequence\": [\n" +
            "    {\n" +
            "      \"kind\": \"path\",\n" +
            "      \"lineId\": \"line-6tw9ehn50rd\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"kind\": \"wait\",\n" +
            "      \"id\": \"mu30bq3y-otkqwf\",\n" +
            "      \"name\": \"waitathon\",\n" +
            "      \"durationMs\": 1000\n" +
            "    }\n" +
            "  ],\n" +
            "  \"shapes\": [\n" +
            "    {\n" +
            "      \"id\": \"biobuzz-hive-red\",\n" +
            "      \"name\": \"HiveRedSide\",\n" +
            "      \"vertices\": [\n" +
            "        { \"x\": 46.5, \"y\": 92 },\n" +
            "        { \"x\": 46.5, \"y\": 52 }\n" +
            "      ],\n" +
            "      \"type\": \"obstacle\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private Follower follower;

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
            @Override public double parametricCompletion() { return 0.0; }
            @Override public boolean atParametricEnd() { return false; }
            @Override public void reset() {}
            @Override public boolean isBusy() { return false; }
            @Override public Map<String, Object> debug() { return Collections.emptyMap(); }
        };

        follower = new Follower(localizer, drivetrain, algorithm);
    }

    @Test
    public void testVisualizerJsonParsing() {
        TurtleTracerReader reader = new TurtleTracerReader(SAMPLE_JSON);

        assertEquals("2.2.1", reader.getVersion());
        assertNotNull(reader.getHeader());
        assertEquals("Created with Turtle Tracer", reader.getHeader().info);

        assertNotNull(reader.getStartPoint());
        assertEquals(9.0, reader.getStartPoint().x, 1e-4);

        assertNotNull(reader.get("startPoint"));
        assertNotNull(reader.get("DriveToShoot"));
        assertNotNull(reader.get("dave"));
        assertNotNull(reader.get("greg"));

        // Verify control point parsing
        assertNotNull(reader.get("greg_control1"));
        assertEquals(50.0, reader.get("greg_control1").x(), 1e-4);
        assertEquals(144.0 - 86.0, reader.get("greg_control1").y(), 1e-4);

        // Verify sequence and shapes
        assertEquals(2, reader.getSequence().size());
        assertEquals("path", reader.getSequence().get(0).kind);
        assertEquals("wait", reader.getSequence().get(1).kind);
        assertEquals(1000, reader.getSequence().get(1).durationMs);

        assertEquals(1, reader.getShapes().size());
        assertEquals("HiveRedSide", reader.getShapes().get(0).name);
    }

    @Test
    public void testEventMarkersParsingAndRegistration() {
        TurtleTracerReader reader = new TurtleTracerReader(SAMPLE_JSON);

        AtomicBoolean fdsagRan = new AtomicBoolean(false);
        AtomicBoolean shootRan = new AtomicBoolean(false);
        AtomicBoolean intakeRan = new AtomicBoolean(false);

        reader.onEvent("fdsag", () -> fdsagRan.set(true))
              .onEvent("Shoot", () -> shootRan.set(true))
              .onEvent("Intake", () -> intakeRan.set(true));

        ProgressTracker tracker = new ProgressTracker(follower, null);
        reader.registerEvents(tracker);

        List<PathEvent> events = tracker.getPathEvents();
        assertEquals(3, events.size());

        // Verify event types
        boolean hasParametric = false;
        boolean hasTemporal = false;
        boolean hasSpatial = false;

        for (PathEvent e : events) {
            if (e instanceof com.turtletracerlib.pathing.event.ParametricEvent) hasParametric = true;
            if (e instanceof TemporalEvent) {
                hasTemporal = true;
                assertEquals(21838, ((TemporalEvent) e).getDurationMs());
            }
            if (e instanceof SpatialEvent) {
                hasSpatial = true;
                assertEquals(2.0, ((SpatialEvent) e).getRadius(), 1e-4);
            }
        }

        assertTrue("Should have registered parametric event", hasParametric);
        assertTrue("Should have registered temporal event", hasTemporal);
        assertTrue("Should have registered spatial event", hasSpatial);
    }
}
