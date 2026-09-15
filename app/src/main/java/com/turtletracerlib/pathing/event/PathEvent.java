package com.turtletracerlib.pathing.event;

/**
 * Base class representing an event that triggers during path following or robot operations.
 * <p>
 * Events encapsulate a condition (e.g. parametric progress, elapsed time, spatial proximity)
 * and an associated {@link Runnable} action that executes once the condition is met.
 * </p>
 */
public abstract class PathEvent {

    /**
     * The action to execute when this event triggers.
     */
    private final Runnable action;

    /**
     * Whether this event has already executed.
     */
    private boolean triggered = false;

    /**
     * Constructs a new PathEvent with the specified action.
     *
     * @param action The action to execute when triggered (can be null).
     */
    public PathEvent(Runnable action) {
        this.action = action;
    }

    /**
     * Evaluates whether the trigger condition for this event is satisfied.
     *
     * @return {@code true} if the condition is met, {@code false} otherwise.
     */
    public abstract boolean isConditionMet();

    /**
     * Checks the event condition and executes the action if it has not yet run.
     * <p>
     * Any exception thrown during action execution is safely caught and logged to prevent
     * disrupting the robot's primary control loop.
     * </p>
     */
    public void update() {
        if (!triggered && isConditionMet()) {
            triggered = true;
            if (action != null) {
                try {
                    action.run();
                } catch (Exception e) {
                    System.err.println("Error executing event action: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if this event has already triggered.
     *
     * @return {@code true} if triggered, {@code false} otherwise.
     */
    public boolean hasTriggered() {
        return triggered;
    }

    /**
     * Resets the event trigger state so it can be triggered again on subsequent runs.
     */
    public void reset() {
        triggered = false;
    }

    /**
     * Retrieves the {@link Runnable} action associated with this event.
     *
     * @return The action runnable.
     */
    public Runnable getAction() {
        return action;
    }
}
