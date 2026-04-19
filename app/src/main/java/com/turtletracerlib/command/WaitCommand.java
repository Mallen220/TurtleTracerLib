package com.turtletracerlib.command;

/**
 * A command that waits for a specified duration before finishing.
 * <p>
 * This is useful in command sequences where a delay is needed (e.g., waiting for a mechanism to settle).
 * </p>
 * @deprecated Marked for removal.
 */
@Deprecated
public class WaitCommand implements Command {

    /**
     * The duration to wait in milliseconds.
     */
    private final long durationMs;

    /**
     * The system time (in milliseconds) when the command started.
     */
    private long startTimeMs;

    /**
     * Creates a new WaitCommand that waits for the specified duration.
     *
     * @param durationMs The duration to wait in milliseconds.
     */
    public WaitCommand(long durationMs) {
        this.durationMs = durationMs;
        this.startTimeMs = Long.MIN_VALUE;
    }

    /**
     * Records the start time when the command is scheduled.
     */
    @Override
    public void initialize() {
        this.startTimeMs = System.currentTimeMillis();
    }

    /**
     * Checks if the wait duration has elapsed.
     *
     * @return {@code true} if the elapsed time since initialization is greater than or equal to the duration.
     */
    @Override
    public boolean isFinished() {
        if (durationMs <= 0) {
            return true;
        }
        if (startTimeMs == Long.MIN_VALUE) {
            // haven't been initialized yet; treat as not finished until initialized
            return false;
        }
        return System.currentTimeMillis() - startTimeMs >= durationMs;
    }
}
