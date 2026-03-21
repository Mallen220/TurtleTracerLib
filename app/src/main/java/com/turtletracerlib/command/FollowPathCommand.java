package com.turtletracerlib.command;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Curve;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.HeadingInterpolator;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathBuilder;
import com.pedropathing.paths.PathChain;
import com.pedropathing.paths.PathConstraints;
import com.pedropathing.paths.callbacks.PathCallback;

import java.util.Collections;
import java.util.Set;

/**
 * A Command that commands the Follower to follow a Path or PathChain.
 * <p>
 * This command supports two modes of operation:
 * 1. **Pre-Built:** Pass an existing {@link Path} or {@link PathChain} to the constructor.
 * 2. **Fluent Builder:** Create a command with just the {@link Follower}, and use the fluent API
 *    (e.g., {@link #curveThrough(double, Pose...)}) to build the path inline.
 * </p>
 * <p>
 * Example Usage:
 * <pre>
 * // Pre-built
 * new FollowPathCommand(follower, myPathChain);
 *
 * // Fluent Builder
 * new FollowPathCommand(follower)
 *     .curveThrough(0.5, new Pose(10, 10), new Pose(20, 20))
 *     .setConstantHeadingInterpolation(Math.toRadians(90));
 * </pre>
 * </p>
 */
public class FollowPathCommand implements Command {

    /**
     * The follower instance used to execute the path.
     */
    private final Follower follower;

    /**
     * The compiled PathChain to follow. This is either provided in the constructor or built by the PathBuilder.
     */
    private PathChain pathChain;

    /**
     * The builder used for fluent path construction. Null if a pre-built chain is provided.
     */
    private PathBuilder pathBuilder;

    /**
     * Whether to hold the position at the end of the path.
     */
    private boolean holdEnd = true;

    /**
     * The maximum power scaling for the follower (0.0 to 1.0).
     */
    private double maxPower = 1.0;

    // --- Constructors for Pre-Built PathChain ---

    /**
     * Creates a new FollowPathCommand for a pre-built PathChain.
     *
     * @param follower  The follower instance.
     * @param pathChain The PathChain to follow.
     */
    public FollowPathCommand(Follower follower, PathChain pathChain) {
        this.follower = follower;
        this.pathChain = pathChain;
    }

    /**
     * Creates a new FollowPathCommand for a pre-built PathChain with hold-end configuration.
     *
     * @param follower  The follower instance.
     * @param pathChain The PathChain to follow.
     * @param holdEnd   Whether to hold position at the end of the path.
     */
    public FollowPathCommand(Follower follower, PathChain pathChain, boolean holdEnd) {
        this(follower, pathChain);
        this.holdEnd = holdEnd;
    }

    /**
     * Creates a new FollowPathCommand for a pre-built PathChain with max power configuration.
     *
     * @param follower  The follower instance.
     * @param pathChain The PathChain to follow.
     * @param maxPower  The maximum power scaling (0.0 to 1.0).
     */
    public FollowPathCommand(Follower follower, PathChain pathChain, double maxPower) {
        this(follower, pathChain);
        this.maxPower = maxPower;
    }

    /**
     * Creates a new FollowPathCommand for a pre-built PathChain with full configuration.
     *
     * @param follower  The follower instance.
     * @param pathChain The PathChain to follow.
     * @param holdEnd   Whether to hold position at the end of the path.
     * @param maxPower  The maximum power scaling (0.0 to 1.0).
     */
    public FollowPathCommand(Follower follower, PathChain pathChain, boolean holdEnd, double maxPower) {
        this.follower = follower;
        this.pathChain = pathChain;
        this.holdEnd = holdEnd;
        this.maxPower = maxPower;
    }

    // --- Constructors for Single Path (auto-converts to PathChain) ---

    /**
     * Creates a new FollowPathCommand for a single Path.
     *
     * @param follower The follower instance.
     * @param path     The Path to follow.
     */
    public FollowPathCommand(Follower follower, Path path) {
        this(follower, new PathChain(path));
    }

    /**
     * Creates a new FollowPathCommand for a single Path with hold-end configuration.
     *
     * @param follower The follower instance.
     * @param path     The Path to follow.
     * @param holdEnd  Whether to hold position at the end of the path.
     */
    public FollowPathCommand(Follower follower, Path path, boolean holdEnd) {
        this(follower, new PathChain(path), holdEnd);
    }

    /**
     * Creates a new FollowPathCommand for a single Path with max power configuration.
     *
     * @param follower The follower instance.
     * @param path     The Path to follow.
     * @param maxPower The maximum power scaling (0.0 to 1.0).
     */
    public FollowPathCommand(Follower follower, Path path, double maxPower) {
        this(follower, new PathChain(path), maxPower);
    }

    /**
     * Creates a new FollowPathCommand for a single Path with full configuration.
     *
     * @param follower The follower instance.
     * @param path     The Path to follow.
     * @param holdEnd  Whether to hold position at the end of the path.
     * @param maxPower The maximum power scaling (0.0 to 1.0).
     */
    public FollowPathCommand(Follower follower, Path path, boolean holdEnd, double maxPower) {
        this(follower, new PathChain(path), holdEnd, maxPower);
    }

    // --- Constructor for Fluent Building ---

    /**
     * Creates a new FollowPathCommand in builder mode.
     * <p>
     * Use methods like {@link #curveThrough(double, Pose...)} to build the path inline.
     * The {@link PathChain} is constructed when {@link #initialize()} is called.
     * </p>
     *
     * @param follower The follower instance.
     */
    public FollowPathCommand(Follower follower) {
        this.follower = follower;
        this.pathBuilder = new PathBuilder(follower);
    }

    // --- Configuration Methods ---

    /**
     * Sets whether to hold position at the end of the path.
     *
     * @param holdEnd {@code true} to hold, {@code false} to coast.
     * @return This command (for chaining).
     */
    public FollowPathCommand setHoldEnd(boolean holdEnd) {
        this.holdEnd = holdEnd;
        return this;
    }

    /**
     * Sets the maximum power scaling for the follower.
     *
     * @param maxPower The maximum power (0.0 to 1.0).
     * @return This command (for chaining).
     */
    public FollowPathCommand setMaxPower(double maxPower) {
        this.maxPower = maxPower;
        return this;
    }

    // --- PathBuilder Delegation Methods ---

    /**
     * Ensures that the PathBuilder is initialized.
     *
     * @throws IllegalStateException If the command was created with a pre-built PathChain.
     */
    private void ensureBuilder() {
        if (pathBuilder == null) {
            throw new IllegalStateException("Cannot add path steps to a FollowPathCommand created with a pre-built PathChain.");
        }
    }

    /**
     * Adds a {@link Path} to the chain being built.
     *
     * @param path The path to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addPath(Path path) {
        ensureBuilder();
        pathBuilder.addPath(path);
        return this;
    }

    /**
     * Adds a {@link Curve} (Bezier curve) to the chain being built.
     *
     * @param curve The curve to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addPath(Curve curve) {
        ensureBuilder();
        pathBuilder.addPath(curve);
        return this;
    }

    /**
     * Adds multiple {@link Path} objects to the chain being built.
     *
     * @param paths The paths to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addPaths(Path... paths) {
        ensureBuilder();
        pathBuilder.addPaths(paths);
        return this;
    }

    /**
     * Creates a spline through a set of points and adds it to the chain.
     *
     * @param tension The tension of the spline (typically 0.5).
     * @param points  The control points (poses) for the spline.
     * @return This command (for chaining).
     */
    public FollowPathCommand curveThrough(double tension, Pose... points) {
        ensureBuilder();
        pathBuilder.curveThrough(tension, points);
        return this;
    }

    /**
     * Creates a spline through a set of points (with explicit start/prev points) and adds it.
     *
     * @param prevPoint  The point preceding the start (for tangent calculation).
     * @param startPoint The starting point of the spline.
     * @param tension    The tension of the spline.
     * @param points     The subsequent control points.
     * @return This command (for chaining).
     */
    public FollowPathCommand curveThrough(Pose prevPoint, Pose startPoint, double tension, Pose... points) {
        ensureBuilder();
        pathBuilder.curveThrough(prevPoint, startPoint, tension, points);
        return this;
    }

    /**
     * Sets linear heading interpolation for the current path segment.
     *
     * @param startHeading The starting heading in radians.
     * @param endHeading   The ending heading in radians.
     * @return This command (for chaining).
     */
    public FollowPathCommand setLinearHeadingInterpolation(double startHeading, double endHeading) {
        ensureBuilder();
        pathBuilder.setLinearHeadingInterpolation(startHeading, endHeading);
        return this;
    }

    /**
     * Sets linear heading interpolation with a time constraint.
     *
     * @param startHeading The starting heading in radians.
     * @param endHeading   The ending heading in radians.
     * @param endTime      The normalized time (0-1) at which the interpolation ends.
     * @return This command (for chaining).
     */
    public FollowPathCommand setLinearHeadingInterpolation(double startHeading, double endHeading, double endTime) {
        ensureBuilder();
        pathBuilder.setLinearHeadingInterpolation(startHeading, endHeading, endTime);
        return this;
    }

    /**
     * Sets linear heading interpolation with start and end time constraints.
     *
     * @param startHeading The starting heading in radians.
     * @param endHeading   The ending heading in radians.
     * @param endTime      The normalized time (0-1) at which the interpolation ends.
     * @param startTime    The normalized time (0-1) at which the interpolation starts.
     * @return This command (for chaining).
     */
    public FollowPathCommand setLinearHeadingInterpolation(double startHeading, double endHeading, double endTime, double startTime) {
        ensureBuilder();
        pathBuilder.setLinearHeadingInterpolation(startHeading, endHeading, endTime, startTime);
        return this;
    }

    /**
     * Sets constant heading interpolation for the current path segment.
     *
     * @param setHeading The constant heading to maintain, in radians.
     * @return This command (for chaining).
     */
    public FollowPathCommand setConstantHeadingInterpolation(double setHeading) {
        ensureBuilder();
        pathBuilder.setConstantHeadingInterpolation(setHeading);
        return this;
    }

    /**
     * Sets tangent heading interpolation (robot faces direction of travel) for the current path segment.
     *
     * @return This command (for chaining).
     */
    public FollowPathCommand setTangentHeadingInterpolation() {
        ensureBuilder();
        pathBuilder.setTangentHeadingInterpolation();
        return this;
    }

    /**
     * Sets a custom heading interpolation function for the current path segment.
     *
     * @param function The custom {@link HeadingInterpolator}.
     * @return This command (for chaining).
     */
    public FollowPathCommand setHeadingInterpolation(HeadingInterpolator function) {
        ensureBuilder();
        pathBuilder.setHeadingInterpolation(function);
        return this;
    }

    /**
     * Sets path constraints (velocity, acceleration) for the current path segment.
     *
     * @param constraints The {@link PathConstraints} to apply.
     * @return This command (for chaining).
     */
    public FollowPathCommand setConstraints(PathConstraints constraints) {
        ensureBuilder();
        pathBuilder.setConstraints(constraints);
        return this;
    }

    /**
     * Adds a callback to be executed during the path following.
     *
     * @param callback The {@link PathCallback} to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addCallback(PathCallback callback) {
        ensureBuilder();
        pathBuilder.addCallback(callback);
        return this;
    }

    /**
     * Adds a callback triggered at a specific parametric t-value along the path.
     *
     * @param t        The parametric value (0.0 to 1.0).
     * @param runnable The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand addParametricCallback(double t, Runnable runnable) {
        ensureBuilder();
        pathBuilder.addParametricCallback(t, runnable);
        return this;
    }

    /**
     * Adds a callback triggered at a specific time (in ms?) or duration.
     * (Note: Check PathBuilder docs for exact unit, assuming normalized or absolute based on implementation).
     *
     * @param time     The time value.
     * @param runnable The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand addTemporalCallback(double time, Runnable runnable) {
        ensureBuilder();
        pathBuilder.addTemporalCallback(time, runnable);
        return this;
    }

    // --- Command Interface Implementation ---

    /**
     * Initializes the path following.
     * <p>
     * If using the builder mode, the PathChain is built here.
     * Then, the follower is instructed to follow the path.
     * </p>
     */
    @Override
    public void initialize() {
        if (pathChain == null) {
            if (pathBuilder != null) {
                // Build the chain on first run
                pathChain = pathBuilder.build();
            } else {
                throw new IllegalStateException("No PathChain provided or built.");
            }
        }
        follower.followPath(pathChain, maxPower, holdEnd);
    }

    /**
     * Executes the command.
     * <p>
     * The follower update is typically handled by the main OpMode loop, so this method is empty.
     * </p>
     */
    @Override
    public void execute() {
        // No-op: Follower update is typically handled by the OpMode loop.
        // If specific telemetry is needed, it can be added here.
    }

    /**
     * Checks if the path following is complete.
     *
     * @return {@code true} if the follower is no longer busy, {@code false} otherwise.
     */
    @Override
    public boolean isFinished() {
        return !follower.isBusy();
    }

    /**
     * Ends the path following.
     * <p>
     * If interrupted, the follower is commanded to break following.
     * </p>
     *
     * @param interrupted whether the command was interrupted.
     */
    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            follower.breakFollowing();
        }
    }

    /**
     * Specifies that this command requires the {@link Follower}.
     *
     * @return A set containing the follower.
     */
    @Override
    public Set<Object> getRequirements() {
        return Collections.singleton(follower);
    }
}
