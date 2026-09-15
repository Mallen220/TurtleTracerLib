package com.turtletracerlib.pathing.event;

import java.util.concurrent.TimeUnit;

/**
 * An event that triggers after a specified duration has elapsed from when tracking began.
 */
public class TemporalEvent extends PathEvent {

    private final long durationMs;
    private long startTimeMs = -1;

    /**
     * Constructs a TemporalEvent with a duration in milliseconds.
     *
     * @param durationMs Duration in milliseconds to wait before triggering.
     * @param action     The action to execute.
     */
    public TemporalEvent(long durationMs, Runnable action) {
        super(action);
        this.durationMs = durationMs;
    }

    /**
     * Constructs a TemporalEvent with a duration and {@link TimeUnit}.
     *
     * @param duration Time value.
     * @param unit     Unit of the duration.
     * @param action   The action to execute.
     */
    public TemporalEvent(long duration, TimeUnit unit, Runnable action) {
        this(unit.toMillis(duration), action);
    }

    /**
     * Starts the timer for this event using current system time.
     */
    public void start() {
        start(System.currentTimeMillis());
    }

    /**
     * Starts the timer for this event with a specified timestamp.
     *
     * @param currentTimeMs Start time in milliseconds.
     */
    public void start(long currentTimeMs) {
        this.startTimeMs = currentTimeMs;
    }

    @Override
    public boolean isConditionMet() {
        if (startTimeMs < 0) {
            return false;
        }
        return (System.currentTimeMillis() - startTimeMs) >= durationMs;
    }

    @Override
    public void reset() {
        super.reset();
        this.startTimeMs = -1;
    }

    /**
     * Gets the configured duration in milliseconds.
     *
     * @return Duration in milliseconds.
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * Checks whether the timer has been started.
     *
     * @return {@code true} if started, {@code false} otherwise.
     */
    public boolean isStarted() {
        return startTimeMs >= 0;
    }
}
