package com.pedropathingplus.pathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import java.util.HashMap;
import java.util.Map;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * Tracks the progress of a {@link Follower} along a {@link PathChain} or during a turn operation.
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
   * The current {@link PathChain} being followed.
   */
  private PathChain currentChain;

  /**
   * A map of event names to their trigger zones.
   */
  private final Map<String, EventZone> eventZones = new HashMap<>();

  /**
   * A map tracking whether each event has been triggered to prevent duplicate executions.
   */
  private final Map<String, Boolean> eventTriggered = new HashMap<>();

  /**
   * The telemetry object used for debugging output.
   */
  private Telemetry telemetry;

  /**
   * The name of the current path being followed, for display purposes.
   */
  private String currentPathName = "";

  /**
   * The overall progress along the entire {@link PathChain} (0.0 to 1.0).
   */
  private double chainProgress = 0.0;

  /**
   * The progress along the current individual {@link Path} within the chain (0.0 to 1.0).
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
   * Sets the current {@link PathChain} to track and resets all event triggers.
   *
   * @param chain The new {@link PathChain} to follow.
   */
  public void setCurrentChain(PathChain chain) {
    this.currentChain = chain;
    clearEvents();
    if (telemetry != null) {
      telemetry.addData("ProgressTracker", "Set new chain");
      telemetry.addData("Chain Size", chain.size());
      telemetry.addData("Current Index", follower.getChainIndex());
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
    if (telemetry != null) {
      telemetry.addData("ProgressTracker", "Events cleared");
    }
  }

  /**
   * Manually triggers an event by name if it hasn't been triggered already.
   * <p>
   * This executes the command associated with the event name via {@link NamedCommands}.
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
      // Execute the named command if it exists
      if (NamedCommands.hasCommand(eventName)) {
        NamedCommands.getCommand(eventName).schedule();
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
    follower.turnTo(radians);
    startHeading = follower.getPose().getHeading();
    targetHeading = radians;
    totalTurnRadians = Math.abs(getSmallestAngleDifference(targetHeading, startHeading));
    isTrackingTurn = true;
    clearEvents();
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
      if (follower.isTurning()) {
        double currentHeading = follower.getPose().getHeading();
        double remainingRadians = Math.abs(getSmallestAngleDifference(targetHeading, currentHeading));

        double progress;
        if (totalTurnRadians < 1e-6) {
          progress = 1.0;
        } else {
          progress = 1.0 - (remainingRadians / totalTurnRadians);
        }

        pathProgress = Math.max(0.0, Math.min(1.0, progress));
        chainProgress = pathProgress; // For turn, chain progress mirrors turn progress

        if (telemetry != null) {
          telemetry.addData("Turn Progress", String.format("%.3f", pathProgress));
          telemetry.addData("Turn Remaining", String.format("%.3f rad", remainingRadians));
        }
      } else {
        // Turn finished
        isTrackingTurn = false;
        pathProgress = 1.0;
        chainProgress = 1.0;
      }
    } else if (currentChain != null && follower.getCurrentPath() != null) {
      // For individual path progress (0 to 1)
      pathProgress = Math.min(follower.getCurrentTValue(), 1.0);

      // For chain progress if multiple paths in chain
      int currentIndex = follower.getChainIndex();
      double totalProgress = 0;
      double currentProgress = 0;

      for (int i = 0; i < currentChain.size(); i++) {
        Path path = currentChain.getPath(i);
        if (i < currentIndex) {
          // Path completed
          currentProgress += 1.0;
        } else if (i == currentIndex) {
          // Current path
          currentProgress += pathProgress;
        }
        totalProgress += 1.0;
      }

      chainProgress = totalProgress > 0 ? currentProgress / totalProgress : 0.0;

      if (telemetry != null) {
        telemetry.addData("Path Progress", String.format("%.3f", pathProgress));
        telemetry.addData("Chain Progress", String.format("%.3f", chainProgress));
        telemetry.addData("Current T Value", follower.getCurrentTValue());
        telemetry.addData("Chain Index", currentIndex);
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
   * Gets the overall progress of the current path chain.
   *
   * @return The progress from 0.0 to 1.0.
   */
  public double getChainProgress() {
    updateProgress();
    return chainProgress;
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
   * Delegates to {@link Follower#breakFollowing()}.
   * <p>
   * Stops the current path following or turn operation.
   * </p>
   */
  public void breakFollowing() {
    follower.breakFollowing();
  }
}
