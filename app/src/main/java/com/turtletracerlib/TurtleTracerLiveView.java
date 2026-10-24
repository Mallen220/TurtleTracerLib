package com.turtletracerlib;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.math.Velocity;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A utility class to send real-time robot pose telemetry to the Turtle Tracer
 * Visualizer.
 * It runs a TCP server on port 8888 and broadcasts the robot's pose as JSON.
 * Each message also carries the robot clock ({@code t}, in ms) and, when a
 * {@link Follower} is linked, its velocity and busy state, and while it
 * follows a path the closest point on it, so the visualizer can compare the
 * real robot's motion with its simulation settings.
 * <p>
 * This class uses the Singleton pattern to ensure the server persists across
 * OpModes.
 * It allows the robot to communicate its position and heading to an external
 * visualizer tool
 * for debugging and path verification.
 * </p>
 *
 * <h2>Usage Instructions:</h2>
 * <ol>
 * <li><strong>Start:</strong> Call {@link #start()} in your OpMode's
 * {@code init()} (it is safe to call multiple times).</li>
 * <li><strong>Set Follower:</strong> Call {@link #setFollower(Follower)} in
 * {@code init()} to link the current OpMode's follower.</li>
 * <li><strong>Cleanup:</strong> Call {@link #disable()} in your OpMode's
 * {@code stop()} to clear the reference and prevent memory leaks.</li>
 * </ol>
 *
 * <pre>{@code
 * public class MyAuto extends LinearOpMode {
 *     private Follower follower;
 *
 *     &#64;Override
 *     public void runOpMode() {
 *         follower = new Follower(...);
 *
 *         // Start server (if not running) and link follower
 *         TurtleTracerLiveView.getInstance().start();
 *         TurtleTracerLiveView.getInstance().setFollower(follower);
 *
 *         waitForStart();
 *         // ... run pathing ...
 *
 *         // Clean up reference when done
 *         TurtleTracerLiveView.getInstance().disable();
 *     }
 * }
 * }</pre>
 */
public class TurtleTracerLiveView {

    /**
     * The single instance of this class (Singleton pattern).
     */
    private static final TurtleTracerLiveView INSTANCE = new TurtleTracerLiveView();

    /**
     * A thread-safe reference to the supplier that provides the current robot pose.
     * This allows hot-swapping the provider between OpModes without restarting the
     * server.
     */
    private final AtomicReference<Supplier<Pose>> poseProvider = new AtomicReference<>();

    /**
     * The follower linked with {@link #setFollower(Follower)}, used for velocity
     * and busy state. {@code null} when a custom pose provider is used.
     */
    private final AtomicReference<Follower> follower = new AtomicReference<>();

    /**
     * The thread running the server loop to accept client connections.
     */
    private Thread serverThread;

    /**
     * The server socket listening for incoming connections.
     */
    private ServerSocket serverSocket;

    /**
     * A flag indicating whether the server is currently running.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * The port on which the server listens for connections (8888).
     */
    private static final int PORT = 8888;

    /**
     * The default interval in milliseconds between telemetry updates (20ms, 50 Hz).
     */
    private static final int DEFAULT_UPDATE_INTERVAL_MS = 20;

    /**
     * The shortest interval accepted by {@link #setUpdateIntervalMs(int)}.
     */
    private static final int MIN_UPDATE_INTERVAL_MS = 5;

    /**
     * The interval in milliseconds between telemetry updates sent to connected
     * clients.
     */
    private volatile int updateIntervalMs = DEFAULT_UPDATE_INTERVAL_MS;

    /**
     * Private constructor to enforce the Singleton pattern.
     */
    private TurtleTracerLiveView() {
    }

    /**
     * Retrieves the singleton instance of {@code TurtleTracerLiveView}.
     *
     * @return The singleton instance.
     */
    public static TurtleTracerLiveView getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the {@link Follower} instance to track.
     * <p>
     * This convenience method extracts the pose supplier from the provided
     * {@code Follower}, and also sends its velocity and busy state.
     * Call this in your OpMode's {@code init()} method.
     * </p>
     *
     * @param follower The {@link Follower} instance to track, or {@code null} to
     *                 disable tracking.
     */
    public void setFollower(Follower follower) {
        this.follower.set(follower);
        if (follower != null) {
            this.poseProvider.set(follower::pose);
        } else {
            this.poseProvider.set(null);
        }
    }

    /**
     * Sets a custom pose provider.
     * <p>
     * Use this method if you are not using a standard {@code Follower} or need to
     * provide
     * pose data from a different source (e.g., simulation or raw odometry).
     * </p>
     *
     * @param poseProvider A {@link Supplier} that returns the current robot
     *                     {@link Pose}.
     */
    public void setPoseProvider(Supplier<Pose> poseProvider) {
        this.follower.set(null);
        this.poseProvider.set(poseProvider);
    }

    /**
     * Sets how often telemetry is sent to each connected client.
     *
     * @param intervalMs Milliseconds between messages (at least 5). The default
     *                   is 20 (50 Hz).
     */
    public void setUpdateIntervalMs(int intervalMs) {
        this.updateIntervalMs = Math.max(MIN_UPDATE_INTERVAL_MS, intervalMs);
    }

    /**
     * Returns the interval in milliseconds between telemetry messages.
     *
     * @return The current update interval.
     */
    public int getUpdateIntervalMs() {
        return updateIntervalMs;
    }

    /**
     * Disables telemetry by clearing the pose provider reference.
     * <p>
     * It is crucial to call this in your OpMode's {@code stop()} method to release
     * references
     * to the {@code Follower} (and thus hardware objects), preventing potential
     * memory leaks
     * or access to closed hardware devices.
     * </p>
     */
    public void disable() {
        this.follower.set(null);
        this.poseProvider.set(null);
    }

    /**
     * Where the follower is on the path it follows: the closest point, the
     * path's direction there, and which path of the chain it is on.
     */
    static final class PathPoint {
        final double x;
        final double y;
        final double tangentX;
        final double tangentY;
        final int pathIndex;

        PathPoint(double x, double y, double tangentX, double tangentY, int pathIndex) {
            this.x = x;
            this.y = y;
            this.tangentX = tangentX;
            this.tangentY = tangentY;
            this.pathIndex = pathIndex;
        }

        /** The follower's point on its path, or {@code null} when it isn't following one. */
        static PathPoint of(Follower follower) {
            if (!follower.isBusy()) {
                return null;
            }
            try {
                Pose closest = follower.closestPose();
                Vector2D tangent = follower.closestTangent();
                if (closest == null || tangent == null) {
                    return null;
                }
                return new PathPoint(closest.x(), closest.y(), tangent.x(), tangent.y(),
                        follower.pathIndex());
            } catch (RuntimeException e) {
                return null;
            }
        }
    }

    /**
     * Builds one telemetry message.
     *
     * @param pose     The robot pose.
     * @param velocity The follower's velocity (in/s and rad/s), or {@code null}.
     * @param busy     Whether the follower is following a path, or {@code null}.
     * @param timeMs   The robot clock in milliseconds.
     * @return A single-line JSON object.
     */
    static String formatMessage(Pose pose, Velocity velocity, Boolean busy, double timeMs) {
        return formatMessage(pose, velocity, busy, null, timeMs);
    }

    /**
     * Builds one telemetry message.
     *
     * @param pose      The robot pose.
     * @param velocity  The follower's velocity (in/s and rad/s), or {@code null}.
     * @param busy      Whether the follower is following a path, or {@code null}.
     * @param pathPoint Where the follower is on its path, or {@code null}.
     * @param timeMs    The robot clock in milliseconds.
     * @return A single-line JSON object.
     */
    static String formatMessage(Pose pose, Velocity velocity, Boolean busy, PathPoint pathPoint,
            double timeMs) {
        StringBuilder json = new StringBuilder(String.format(Locale.US,
                "{\"x\":%.4f, \"y\":%.4f, \"heading\":%.4f, \"t\":%.1f",
                pose.x(), pose.y(), pose.heading(), timeMs));
        if (velocity != null) {
            json.append(String.format(Locale.US,
                    ", \"vx\":%.4f, \"vy\":%.4f, \"omega\":%.4f",
                    velocity.vx, velocity.vy, velocity.omega));
        }
        if (busy != null) {
            json.append(", \"busy\":").append(busy);
        }
        if (pathPoint != null) {
            json.append(String.format(Locale.US,
                    ", \"cx\":%.4f, \"cy\":%.4f, \"tx\":%.4f, \"ty\":%.4f, \"pathIndex\":%d",
                    pathPoint.x, pathPoint.y, pathPoint.tangentX, pathPoint.tangentY,
                    pathPoint.pathIndex));
        }
        return json.append('}').toString();
    }

    /**
     * Starts the telemetry server in a background thread if it is not already
     * running.
     * <p>
     * This method is idempotent; calling it multiple times has no effect if the
     * server is already active.
     * The server thread is set as a daemon thread, so it will not prevent the JVM
     * (or app) from shutting down.
     * </p>
     */
    public synchronized void start() {
        if (running.get())
            return;
        running.set(true);
        serverThread = new Thread(this::serverLoop);
        serverThread.setDaemon(true); // Ensure it doesn't prevent JVM shutdown
        serverThread.start();
    }

    /**
     * Stops the telemetry server and closes all active connections.
     * <p>
     * This is typically not needed if you want the server to persist across
     * multiple OpModes.
     * However, it can be used to completely shut down the service if required.
     * </p>
     */
    public synchronized void stop() {
        if (!running.get())
            return;
        running.set(false);

        // Close server socket to interrupt accept()
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    /**
     * The main loop for the server thread.
     * <p>
     * Listens for incoming client connections on the specified port. When a client
     * connects,
     * a new thread is spawned to handle that specific client's data stream.
     * </p>
     */
    private void serverLoop() {
        try {
            serverSocket = new ServerSocket(PORT);
            while (running.get()) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    if (running.get()) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles the communication with a single connected client.
     * <p>
     * Continuously sends the current robot pose (as JSON) to the client at the
     * current update interval.
     * </p>
     *
     * @param client The {@link Socket} representing the connected client.
     */
    private void handleClient(Socket client) {
        try (OutputStream out = client.getOutputStream();
                PrintWriter writer = new PrintWriter(out, true)) {

            while (running.get() && !client.isClosed() && client.isConnected()) {
                Supplier<Pose> provider = poseProvider.get();
                Follower linked = follower.get();
                if (provider != null) {
                    try {
                        Pose pose = provider.get();
                        if (pose != null) {
                            Velocity velocity = linked != null ? linked.velocity() : null;
                            Boolean busy = linked != null ? linked.isBusy() : null;
                            PathPoint pathPoint = linked != null ? PathPoint.of(linked) : null;
                            writer.println(formatMessage(pose, velocity, busy, pathPoint,
                                    System.nanoTime() / 1e6));
                        }
                    } catch (Exception e) {
                        // Handle potential exceptions from accessing closed hardware in provider
                        // e.g. if user forgot to call disable()
                        writer.println("{\"error\": \"provider_error\"}");
                    }
                }

                if (writer.checkError()) {
                    break;
                }

                try {
                    Thread.sleep(updateIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            // Client connection error
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }
}
