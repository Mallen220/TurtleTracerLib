package com.turtletracerlib;

import com.pedropathing.geometry.Pose;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
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
        }
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
