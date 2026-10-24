package com.turtletracerlib;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class TurtleTracerLiveViewTest {

    private TurtleTracerLiveView liveView;
    private AtomicReference<Pose> currentPose;

    @Before
    public void setUp() {
        currentPose = new AtomicReference<>(new Pose(0, 0, 0));
        liveView = TurtleTracerLiveView.getInstance();
        liveView.setPoseProvider(currentPose::get);
    }

    @After
    public void tearDown() {
        if (liveView != null) {
            liveView.stop();
            liveView.setUpdateIntervalMs(20);
        }
    }

    @Test
    public void testMessageWithoutFollowerHasPoseAndTime() {
        String json = TurtleTracerLiveView.formatMessage(new Pose(1, 2, 0.5), null, null, 1234.56);
        assertEquals("{\"x\":1.0000, \"y\":2.0000, \"heading\":0.5000, \"t\":1234.6}", json);
    }

    @Test
    public void testMessageWithFollowerStateHasVelocityAndBusy() {
        String json = TurtleTracerLiveView.formatMessage(
                new Pose(1, 2, 0.5), new Velocity(10.25, -3.5, 1.2), true, 50);
        assertTrue(json, json.contains("\"vx\":10.2500, \"vy\":-3.5000, \"omega\":1.2000"));
        assertTrue(json, json.endsWith(", \"busy\":true}"));
    }

    @Test
    public void testMessageWithPathPointHasClosestPointTangentAndIndex() {
        String json = TurtleTracerLiveView.formatMessage(new Pose(1, 2, 0), null, true,
                new TurtleTracerLiveView.PathPoint(1.5, 2.25, 0.6, -0.8, 2), 10);
        assertTrue(json, json.endsWith(
                ", \"cx\":1.5000, \"cy\":2.2500, \"tx\":0.6000, \"ty\":-0.8000, \"pathIndex\":2}"));
    }

    @Test
    public void testMessageUsesDotDecimalsInAnyLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String json = TurtleTracerLiveView.formatMessage(
                    new Pose(1.5, 2, 0), new Velocity(0.5, 0, 0), false, 1.5);
            assertFalse(json, json.contains("1,5"));
            assertTrue(json, json.contains("\"x\":1.5000") && json.contains("\"t\":1.5"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void testUpdateIntervalHasAFloor() {
        liveView.setUpdateIntervalMs(1);
        assertEquals(5, liveView.getUpdateIntervalMs());
        liveView.setUpdateIntervalMs(40);
        assertEquals(40, liveView.getUpdateIntervalMs());
    }

    @Test
    public void testRobotClockIncreasesBetweenMessages() throws Exception {
        liveView.start();
        Thread.sleep(500);

        try (Socket socket = new Socket("localhost", 8888);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            double first = timeOf(reader.readLine());
            double second = timeOf(reader.readLine());
            assertTrue("Robot clock should move forward", second > first);
        }
    }

    private static double timeOf(String line) {
        assertNotNull("Should receive data", line);
        int start = line.indexOf("\"t\":") + 4;
        int end = line.indexOf('}', start);
        int comma = line.indexOf(',', start);
        if (comma != -1 && comma < end) {
            end = comma;
        }
        return Double.parseDouble(line.substring(start, end));
    }

    @Test
    public void testTelemetryData() throws Exception {
        liveView.start();

        // Wait a bit for server to start
        Thread.sleep(500);

        try (Socket socket = new Socket("localhost", 8888);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Read first line
            String line = reader.readLine();
            assertNotNull("Should receive data", line);
            assertTrue("Should contain x", line.contains("\"x\":0.0000"));

            // Update pose
            currentPose.set(new Pose(10.5, 20.123, 1.57));

            // Read subsequent lines until we see the new pose
            boolean found = false;
            for (int i = 0; i < 20; i++) {
                line = reader.readLine();
                if (line != null && line.contains("\"x\":10.5000")) {
                    found = true;
                    break;
                }
            }
            assertTrue("Should receive updated pose", found);

            // Disable provider
            liveView.disable();

            // Should stop receiving updates or receive something else?
            // In current implementation it loops but sends nothing if provider is null.
            // The client readLine() will block if no data is sent.
            // Let's verify we can re-enable it.

            currentPose.set(new Pose(5, 5, 0));
            liveView.setPoseProvider(currentPose::get);

            found = false;
            for (int i = 0; i < 20; i++) {
                line = reader.readLine();
                if (line != null && line.contains("\"x\":5.0000")) {
                    found = true;
                    break;
                }
            }
            assertTrue("Should receive updated pose after re-enabling", found);
        }
    }
}
