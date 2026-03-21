package com.turtletracerlib;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
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
     * The interval in milliseconds between telemetry updates sent to connected
     * clients (50ms).
     */
    private static final int UPDATE_INTERVAL_MS = 50;

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
     * {@code Follower}.
     * Call this in your OpMode's {@code init()} method.
     * </p>
     *
     * @param follower The {@link Follower} instance to track, or {@code null} to
     *                 disable tracking.
     */
    public void setFollower(Follower follower) {
        if (follower != null) {
            this.poseProvider.set(follower::getPose);
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
        this.poseProvider.set(poseProvider);
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
        this.poseProvider.set(null);
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
     * defined update interval.
     * </p>
     *
     * @param client The {@link Socket} representing the connected client.
     */
    private void handleClient(Socket client) {
        try (OutputStream out = client.getOutputStream();
                PrintWriter writer = new PrintWriter(out, true)) {

            while (running.get() && !client.isClosed() && client.isConnected()) {
                Supplier<Pose> provider = poseProvider.get();
                if (provider != null) {
                    try {
                        Pose pose = provider.get();
                        if (pose != null) {
                            String json = String.format(Locale.US,
                                    "{\"x\":%.4f, \"y\":%.4f, \"heading\":%.4f}",
                                    pose.getX(), pose.getY(), pose.getHeading());
                            writer.println(json);
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
                    Thread.sleep(UPDATE_INTERVAL_MS);
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
