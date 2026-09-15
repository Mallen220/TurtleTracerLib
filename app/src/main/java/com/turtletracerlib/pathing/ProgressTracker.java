package com.turtletracerlib.pathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.turtletracerlib.pathing.event.ParametricEvent;
import com.turtletracerlib.pathing.event.PathEvent;
import com.turtletracerlib.pathing.event.SpatialEvent;
import com.turtletracerlib.pathing.event.TemporalEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * Tracks the progress of a {@link Follower} along a {@link Path} or during a turn operation.
 * <p>
 * This class monitors the robot's traversal and triggers registered events at specific points along the path
 * or during a turn. It abstracts the complexity of determining when to execute commands based on position
 * or percentage completion.
 * </p>
 */
public class ProgressTracker {

  /**
   * The {@link Follower} instance being tracked.
   */
  private final Follower follower;

  /**
   * The current {@link Path} being followed.
   */
  private Path currentPath;

  /**
   * A map of event names to their trigger zones.
   */
  private final Map<String, EventZone> eventZones = new HashMap<>();

  /**
   * A map tracking whether each event has been triggered to prevent duplicate executions.
   */
  private final Map<String, Boolean> eventTriggered = new HashMap<>();

  /**
   * Registered dynamic path events (parametric, temporal, spatial).
   */
  private final List<PathEvent> pathEvents = new ArrayList<>();

  /**
   * Direct actions mapped to named events (e.g. from visualizer event markers).
   */
  private final Map<String, Runnable> namedEventActions = new HashMap<>();

  /**
   * The telemetry object used for debugging output.
   */
  private Telemetry telemetry;

  /**
   * The name of the current path being followed, for display purposes.
   */
  private String currentPathName = "";

  /**
   * The overall progress along the entire {@link Path} (0.0 to 1.0).
   */
  private double totalProgress = 0.0;

  /**
   * The progress along the current individual {@link Path} segment (0.0 to 1.0).
   */
  private double pathProgress = 0.0;

  // Turn tracking fields

  /**
   * Indicates whether the tracker is currently monitoring a turn operation instead of a path.
   */
  private boolean isTrackingTurn = false;

  /**
   * The heading of the robot when the turn started (in radians).
   */
  private double startHeading;

  /**
   * The target heading for the turn (in radians).
   */
  private double targetHeading;

  /**
   * The total angular distance of the turn (in radians).
   */
  private double totalTurnRadians;

  /**
   * Constructs a new {@code ProgressTracker}.
   *
   * @param follower  The {@link Follower} to track.
   * @param telemetry The {@link Telemetry} object for debugging output (can be null).
   */
  public ProgressTracker(Follower follower, Telemetry telemetry) {
    this.follower = follower;
    this.telemetry = telemetry;
  }

  /**
   * Sets the current {@link Path} to track and resets all event triggers.
   *
   * @param path The new {@link Path} to follow.
   */
  public void setCurrentPath(Path path) {
    this.currentPath = path;
    resetEvents();
    if (telemetry != null) {
      telemetry.addData("ProgressTracker", "Set new path");
      telemetry.addData("Current Index", follower.pathIndex());
    }
  }

  /**
   * Sets the name of the current path for identification in telemetry.
   *
   * @param name The descriptive name of the path.
   */
  public void setCurrentPathName(String name) {
    this.currentPathName = name;
    if (telemetry != null) {
      telemetry.addData("Current Path", name);
    }
  }

  /**
   * Registers a callback action to execute when a named event (e.g. from an event marker) triggers.
   *
   * @param eventName The name of the event marker.
   * @param action    The action to execute.
   * @return This tracker (for chaining).
   */
  public ProgressTracker onEvent(String eventName, Runnable action) {
    namedEventActions.put(eventName, action);
    return this;
  }

  /**
   * Registers an event that triggers when parametric progress reaches a threshold.
   *
   * @param progress Threshold (0.0 to 1.0) along the current curve.
   * @param action   Action to execute.
   * @return This tracker (for chaining).
   */
  public ProgressTracker onParametric(double progress, Runnable action) {
    return addEvent(new ParametricEvent(this::getPathProgress, progress, action));
  }

  /**
   * Registers an event that triggers within a parametric progress range.
   *
   * @param startProgress Start threshold (0.0 to 1.0).
   * @param endProgress   End threshold (0.0 to 1.0).
   * @param action        Action to execute.
   * @return This tracker (for chaining).
   */
  public ProgressTracker onParametric(double startProgress, double endProgress, Runnable action) {
    return addEvent(new ParametricEvent(this::getPathProgress, startProgress, endProgress, action));
  }

  /**
   * Registers an event that triggers when total path completion reaches a threshold.
   *
   * @param completion Threshold (0.0 to 1.0) of overall path completion.
   * @param action     Action to execute.
   * @return This tracker (for chaining).
   */
  public ProgressTracker onCompletion(double completion, Runnable action) {
    return addEvent(new ParametricEvent(this::getTotalProgress, completion, action));
  }

  /**
   * Registers a temporal event that triggers after a specified duration has elapsed.
   *
   * @param durationMs Duration in milliseconds after path start to trigger.
   * @param action     Action to execute.
   * @return This tracker (for chaining).
   */
  public ProgressTracker onTemporal(long durationMs, Runnable action) {
    TemporalEvent event = new TemporalEvent(durationMs, action);
    if (follower.isBusy()) {
      event.start();
    }
    return addEvent(event);
  }

  /**
   * Registers a temporal event with a custom {@link TimeUnit}.
   *
   * @param duration Duration value.
   * @param unit     Time unit.
   * @param action   Action to execute.
   * @return This tracker (for chaining).
   */
  public ProgressTracker onTemporal(long duration, TimeUnit unit, Runnable action) {
    return onTemporal(unit.toMillis(duration), action);
  }

  /**
   * Registers a spatial event that triggers when the robot is within a radius of a target pose.
   *
   * @param targetPose Target coordinate on the field.
   * @param radius     Proximity radius (in inches).
   * @param action     Action to execute.
   * @return This tracker (for chaining).
   */
  public ProgressTracker onSpatial(Pose targetPose, double radius, Runnable action) {
    return addEvent(new SpatialEvent(follower::pose, targetPose, radius, action));
  }

  /**
   * Adds a custom {@link PathEvent} to this tracker.
   *
   * @param event The event to add.
   * @return This tracker (for chaining).
   */
  public ProgressTracker addEvent(PathEvent event) {
    pathEvents.add(event);
    return this;
  }

  /**
   * Retrieves all dynamic path events registered with this tracker.
   *
   * @return The list of path events.
   */
  public List<PathEvent> getPathEvents() {
    return pathEvents;
  }

  /**
   * Registers an event to be triggered at a specific progress point.
   *
   * @param eventName The unique name of the event (which should correspond to a registered command).
   * @param position  The progress threshold (0.0 to 1.0) at which to trigger the event.
   */
  public void registerEvent(String eventName, double position) {
    registerEvent(eventName, position, position);
  }

  /**
   * Registers a zoned event to be active when the robot is within the specified progress range.
   *
   * @param eventName      The unique name of the event.
   * @param startPosition  The start progress threshold (0.0 to 1.0).
   * @param endPosition    The end progress threshold (0.0 to 1.0).
   */
  public void registerEvent(String eventName, double startPosition, double endPosition) {
    eventZones.put(eventName, new EventZone(startPosition, endPosition));
    eventTriggered.put(eventName, false);
    if (telemetry != null) {
      telemetry.addData("Event Registered", eventName + " @ [" + startPosition + ", " + endPosition + "]");
      telemetry.update();
    }
  }

  /**
   * Clears all registered events and resets their trigger status.
   */
  public void clearEvents() {
    eventZones.clear();
    eventTriggered.clear();
    pathEvents.clear();
    namedEventActions.clear();
    if (telemetry != null) {
      telemetry.addData("ProgressTracker", "Events cleared");
    }
  }

  /**
   * Resets all registered events to allow them to trigger again on subsequent runs.
   */
  public void resetEvents() {
    for (String key : eventZones.keySet()) {
      eventTriggered.put(key, false);
    }
    for (PathEvent event : pathEvents) {
      event.reset();
      if (event instanceof TemporalEvent) {
        ((TemporalEvent) event).start();
      }
    }
  }

  /**
   * Manually triggers an event by name if it hasn't been triggered already.
   * <p>
   * This executes any bound action or falls back to {@link NamedCommands}.
   * </p>
   *
   * @param eventName The name of the event to execute.
   */
  public void executeEvent(String eventName) {
    if (!eventTriggered.getOrDefault(eventName, true)) {
      eventTriggered.put(eventName, true);
      if (telemetry != null) {
        telemetry.addLine("EVENT TRIGGERED: " + eventName);
        telemetry.update();
      }
      if (namedEventActions.containsKey(eventName)) {
        try {
          namedEventActions.get(eventName).run();
        } catch (Exception e) {
          System.err.println("Error executing event action for '" + eventName + "': " + e.getMessage());
        }
      } else if (NamedCommands.hasCommand(eventName)) {
        NamedCommands.getCommand(eventName).run();
      }
    }
  }

  /**
   * Checks if an event has already been triggered.
   *
   * @param eventName The name of the event.
   * @return {@code true} if the event has been triggered, {@code false} otherwise.
   */
  public boolean isEventTriggered(String eventName) {
    return eventTriggered.getOrDefault(eventName, false);
  }

  /**
   * Checks if an event should be triggered based on current progress.
   * <p>
   * This method updates the progress tracker and compares the current progress against the event's
   * registered threshold.
   * </p>
   *
   * @param eventName The name of the event to check.
   * @return {@code true} if the event threshold has been reached and it hasn't been triggered yet.
   */
  public boolean shouldTriggerEvent(String eventName) {
    if (!eventZones.containsKey(eventName) || isEventTriggered(eventName)) {
      return false;
    }

    updateProgress();
    EventZone zone = eventZones.get(eventName);
    boolean shouldTrigger = zone.contains(pathProgress) || pathProgress >= zone.getStartPosition();

    if (telemetry != null) {
      telemetry.addData("Event Check", eventName);
      telemetry.addData("Event Zone", "[" + zone.getStartPosition() + ", " + zone.getEndPosition() + "]");
      telemetry.addData("Current Progress", pathProgress);
      telemetry.addData("Should Trigger", shouldTrigger);
      telemetry.update();
    }

    return shouldTrigger;
  }

  /**
   * Checks if the given event is currently active (i.e. the robot's progress is within the event's zone).
   *
   * @param eventName The name of the event.
   * @return {@code true} if the current progress is within the event's zone.
   */
  public boolean isEventActive(String eventName) {
    if (!eventZones.containsKey(eventName)) {
      return false;
    }

    updateProgress();
    return eventZones.get(eventName).contains(pathProgress);
  }

  /**
   * Initiates a turn operation and registers an event to trigger during the turn.
   * <p>
   * This method instructs the follower to turn to the specified angle and sets up the tracker to monitor
   * angular progress instead of path progress.
   * </p>
   *
   * @param radians        The target heading in radians.
   * @param eventName      The name of the event to trigger.
   * @param eventThreshold The percentage (0.0 to 1.0) of the turn completion at which to trigger the event.
   */
  public void turn(double radians, String eventName, double eventThreshold) {
    follower.hold(follower.pose().withHeading(radians));
    startHeading = follower.pose().heading();
    targetHeading = radians;
    totalTurnRadians = Math.abs(getSmallestAngleDifference(targetHeading, startHeading));
    isTrackingTurn = true;
    resetEvents();
    registerEvent(eventName, eventThreshold);
  }

  /**
   * Calculates the smallest difference between two angles in radians.
   *
   * @param angle1 The first angle.
   * @param angle2 The second angle.
   * @return The difference in the range [-PI, PI].
   */
  private double getSmallestAngleDifference(double angle1, double angle2) {
    double diff = angle1 - angle2;
    while (diff > Math.PI) diff -= 2 * Math.PI;
    while (diff < -Math.PI) diff += 2 * Math.PI;
    return diff;
  }

  /**
   * Updates the current progress metrics (path and chain progress).
   * <p>
   * This method calculates progress based on whether the robot is following a path chain or performing a turn.
   * </p>
   */
  private void updateProgress() {
    if (isTrackingTurn) {
      if (follower.isBusy()) {
        double currentHeading = follower.pose().heading();
        double remainingRadians = Math.abs(getSmallestAngleDifference(targetHeading, currentHeading));

        double progress;
        if (totalTurnRadians < 1e-6) {
          progress = 1.0;
        } else {
          progress = 1.0 - (remainingRadians / totalTurnRadians);
        }

        pathProgress = Math.max(0.0, Math.min(1.0, progress));
        totalProgress = pathProgress; // For turn, total progress mirrors turn progress

        if (telemetry != null) {
          telemetry.addData("Turn Progress", String.format("%.3f", pathProgress));
          telemetry.addData("Turn Remaining", String.format("%.3f rad", remainingRadians));
        }
      } else {
        // Turn finished
        isTrackingTurn = false;
        pathProgress = 1.0;
        totalProgress = 1.0;
      }
    } else if (currentPath != null && follower.currentPath() != null) {
      // For individual segment progress (0 to 1)
      pathProgress = Math.max(0.0, Math.min(1.0, follower.parametricCompletion()));

      // For overall path progress (0 to 1)
      totalProgress = Math.max(0.0, Math.min(1.0, follower.completion()));
      int currentIndex = follower.pathIndex();

      if (telemetry != null) {
        telemetry.addData("Path Progress", String.format("%.3f", pathProgress));
        telemetry.addData("Total Progress", String.format("%.3f", totalProgress));
        telemetry.addData("Current T Value", follower.parametricCompletion());
        telemetry.addData("Path Index", currentIndex);
      }
    }
  }

  /**
   * Gets the progress of the current path segment.
   *
   * @return The progress from 0.0 to 1.0.
   */
  public double getPathProgress() {
    updateProgress();
    return pathProgress;
  }

  /**
   * Gets the overall progress of the current path.
   *
   * @return The progress from 0.0 to 1.0.
   */
  public double getTotalProgress() {
    updateProgress();
    return totalProgress;
  }

  /**
   * Gets the overall completion percentage of the current path (0.0 to 1.0).
   *
   * @return The completion from 0.0 to 1.0.
   */
  public double getCompletion() {
    return getTotalProgress();
  }

  /**
   * Gets the overall progress of the current path.
   *
   * @return The progress from 0.0 to 1.0.
   */
  public double getChainProgress() {
    return getTotalProgress();
  }

  /**
   * Updates progress metrics and executes any registered events whose conditions are met.
   * <p>
   * Call this in your main OpMode loop alongside {@link Follower#update()}.
   * </p>
   */
  public void update() {
    updateProgress();

    // Check zoned / named events
    for (String eventName : new ArrayList<>(eventZones.keySet())) {
      if (shouldTriggerEvent(eventName)) {
        executeEvent(eventName);
      }
    }

    // Check dynamic PathEvents
    for (PathEvent event : pathEvents) {
      event.update();
    }
  }

  /**
   * Delegates to {@link Follower#isBusy()}.
   *
   * @return {@code true} if the follower is currently executing a path or turn.
   */
  public boolean isBusy() {
    return follower.isBusy();
  }

  /**
   * Delegates to {@link Follower#stop()}.
   * <p>
   * Stops the current path following or turn operation.
   * </p>
   */
  public void breakFollowing() {
    follower.stop();
  }

  /**
   * Stops the current path following or turn operation.
   */
  public void stop() {
    follower.stop();
  }
}
