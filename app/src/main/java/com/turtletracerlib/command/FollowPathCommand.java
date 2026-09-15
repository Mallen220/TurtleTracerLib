package com.turtletracerlib.command;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.curves.Curve;
import com.pedropathing.paths.interpolator.Interpolator;
import com.turtletracerlib.pathing.event.ParametricEvent;
import com.turtletracerlib.pathing.event.PathEvent;
import com.turtletracerlib.pathing.event.SpatialEvent;
import com.turtletracerlib.pathing.event.TemporalEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A Command that commands the Follower to follow a Path.
 * <p>
 * This command supports two modes of operation:
 * 1. **Pre-Built:** Pass an existing {@link Path} to the constructor.
 * 2. **Fluent Builder:** Create a command with just the {@link Follower}, and use the fluent API
 *    (e.g., {@link #curveThrough(Pose...)}) to build the path inline.
 * </p>
 * <p>
 * Events can be attached to execute actions at specific progress, time, or spatial coordinates.
 * </p>
 * @deprecated Marked for removal.
 */
@Deprecated
public class FollowPathCommand implements Command {

    /**
     * The follower instance used to execute the path.
     */
    private final Follower follower;

    /**
     * The compiled Path to follow. This is either provided in the constructor or built dynamically.
     */
    private Path path;

    /**
     * The list of paths used for fluent path construction.
     */
    private final List<Path> paths = new ArrayList<>();

    /**
     * The list of events to trigger during path execution.
     */
    private final List<PathEvent> events = new ArrayList<>();

    /**
     * Whether to hold the position at the end of the path.
     */
    private boolean holdEnd = true;

    // --- Constructors for Pre-Built Path ---

    /**
     * Creates a new FollowPathCommand for a single Path.
     *
     * @param follower The follower instance.
     * @param path     The Path to follow.
     */
    public FollowPathCommand(Follower follower, Path path) {
        this.follower = follower;
        this.path = path;
    }

    /**
     * Creates a new FollowPathCommand for a single Path with hold-end configuration.
     *
     * @param follower The follower instance.
     * @param path     The Path to follow.
     * @param holdEnd  Whether to hold position at the end of the path.
     */
    public FollowPathCommand(Follower follower, Path path, boolean holdEnd) {
        this(follower, path);
        this.holdEnd = holdEnd;
    }

    // --- Constructor for Fluent Building ---

    /**
     * Creates a new FollowPathCommand in builder mode.
     * <p>
     * Use methods like {@link #curveThrough(double, Pose...)} to build the path inline.
     * The {@link Path} is constructed when {@link #initialize()} is called.
     * </p>
     *
     * @param follower The follower instance.
     */
    public FollowPathCommand(Follower follower) {
        this.follower = follower;
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

    // --- Fluent Path Construction Methods ---

    /**
     * Adds a {@link Path} to the path being built.
     *
     * @param path The path to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addPath(Path path) {
        paths.add(path);
        return this;
    }

    /**
     * Adds a {@link Curve} to the path being built.
     *
     * @param curve The curve to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addPath(Curve curve) {
        paths.add(Paths.path(curve));
        return this;
    }

    /**
     * Adds multiple {@link Path} objects to the path being built.
     *
     * @param paths The paths to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addPaths(Path... paths) {
        Collections.addAll(this.paths, paths);
        return this;
    }

    /**
     * Creates a bezier curve through a set of points and adds it to the path.
     *
     * @param points The control points (poses) for the spline.
     * @return This command (for chaining).
     */
    public FollowPathCommand curveThrough(Pose... points) {
        paths.add(Paths.through(points));
        return this;
    }

    /**
     * Creates a bezier curve through a set of points and adds it to the path.
     *
     * @param tension The tension of the spline (ignored, preserved for API compatibility).
     * @param points  The control points (poses) for the spline.
     * @return This command (for chaining).
     */
    public FollowPathCommand curveThrough(double tension, Pose... points) {
        return curveThrough(points);
    }

    /**
     * Creates a bezier curve through a set of points (with explicit start/prev points) and adds it.
     *
     * @param prevPoint  The point preceding the start (preserved for API compatibility).
     * @param startPoint The starting point of the spline.
     * @param tension    The tension of the spline (preserved for API compatibility).
     * @param points     The subsequent control points.
     * @return This command (for chaining).
     */
    public FollowPathCommand curveThrough(Pose prevPoint, Pose startPoint, double tension, Pose... points) {
        Pose[] all = new Pose[points.length + 1];
        all[0] = startPoint;
        System.arraycopy(points, 0, all, 1, points.length);
        paths.add(Paths.through(all));
        return this;
    }

    private Path getLastPath() {
        if (paths.isEmpty()) {
            throw new IllegalStateException("No path segments have been added yet.");
        }
        return paths.get(paths.size() - 1);
    }

    private void updateLastPath(Path updated) {
        paths.set(paths.size() - 1, updated);
    }

    /**
     * Sets linear heading interpolation for the current path segment.
     *
     * @param startHeading The starting heading in radians.
     * @param endHeading   The ending heading in radians.
     * @return This command (for chaining).
     */
    public FollowPathCommand setLinearHeadingInterpolation(double startHeading, double endHeading) {
        updateLastPath(getLastPath().linear(startHeading, endHeading));
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
        updateLastPath(getLastPath().linear(startHeading, endHeading, endTime));
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
        updateLastPath(getLastPath().linear(startHeading, endHeading, endTime));
        return this;
    }

    /**
     * Sets constant heading interpolation for the current path segment.
     *
     * @param setHeading The constant heading to maintain, in radians.
     * @return This command (for chaining).
     */
    public FollowPathCommand setConstantHeadingInterpolation(double setHeading) {
        updateLastPath(getLastPath().constant(setHeading));
        return this;
    }

    /**
     * Sets tangent heading interpolation (robot faces direction of travel) for the current path segment.
     *
     * @return This command (for chaining).
     */
    public FollowPathCommand setTangentHeadingInterpolation() {
        updateLastPath(getLastPath().tangent());
        return this;
    }

    /**
     * Sets a custom heading interpolation function for the current path segment.
     *
     * @param function The custom {@link Interpolator}.
     * @return This command (for chaining).
     */
    public FollowPathCommand setHeadingInterpolation(Interpolator function) {
        updateLastPath(getLastPath().heading(function));
        return this;
    }

    /**
     * Attaches an event that triggers when the parametric completion along the current curve reaches a threshold.
     *
     * @param progress Progress threshold (0.0 to 1.0).
     * @param action   The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand onParametric(double progress, Runnable action) {
        return addEvent(new ParametricEvent(follower::parametricCompletion, progress, action));
    }

    /**
     * Attaches an event that triggers within a parametric progress range.
     *
     * @param startProgress Start progress (0.0 to 1.0).
     * @param endProgress   End progress (0.0 to 1.0).
     * @param action        The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand onParametric(double startProgress, double endProgress, Runnable action) {
        return addEvent(new ParametricEvent(follower::parametricCompletion, startProgress, endProgress, action));
    }

    /**
     * Attaches an event that triggers when total path completion reaches a threshold.
     *
     * @param completion Completion threshold (0.0 to 1.0).
     * @param action     The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand onCompletion(double completion, Runnable action) {
        return addEvent(new ParametricEvent(follower::completion, completion, action));
    }

    /**
     * Attaches a temporal event that triggers after a specified duration from path start.
     *
     * @param durationMs Duration in milliseconds.
     * @param action     The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand onTemporal(long durationMs, Runnable action) {
        return addEvent(new TemporalEvent(durationMs, action));
    }

    /**
     * Attaches a temporal event with custom {@link TimeUnit}.
     *
     * @param duration Time value.
     * @param unit     Time unit.
     * @param action   The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand onTemporal(long duration, TimeUnit unit, Runnable action) {
        return onTemporal(unit.toMillis(duration), action);
    }

    /**
     * Attaches a spatial event that triggers when the robot is within a radius of a target pose.
     *
     * @param targetPose Target coordinates.
     * @param radius     Proximity radius (in inches).
     * @param action     The action to execute.
     * @return This command (for chaining).
     */
    public FollowPathCommand onSpatial(Pose targetPose, double radius, Runnable action) {
        return addEvent(new SpatialEvent(follower::pose, targetPose, radius, action));
    }

    /**
     * Adds a custom {@link PathEvent} to this command.
     *
     * @param event The event to add.
     * @return This command (for chaining).
     */
    public FollowPathCommand addEvent(PathEvent event) {
        events.add(event);
        return this;
    }

    // --- Command Interface Implementation ---

    /**
     * Initializes the path following.
     * <p>
     * If using the builder mode, the Path is built here.
     * Then, the follower is instructed to follow the path, and events are initialized.
     * </p>
     */
    @Override
    public void initialize() {
        if (path == null) {
            if (!paths.isEmpty()) {
                path = paths.size() == 1 ? paths.get(0) : Paths.path(paths.toArray(new Path[0]));
            } else {
                throw new IllegalStateException("No Path provided or built.");
            }
        }
        for (PathEvent event : events) {
            event.reset();
            if (event instanceof TemporalEvent) {
                ((TemporalEvent) event).start();
            }
        }
        follower.holdEnd.set(holdEnd);
        follower.follow(path);
    }

    /**
     * Executes the command, evaluating and triggering any active events.
     */
    @Override
    public void execute() {
        for (PathEvent event : events) {
            event.update();
        }
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
     * If interrupted, the follower is stopped.
     * </p>
     *
     * @param interrupted whether the command was interrupted.
     */
    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            follower.stop();
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
