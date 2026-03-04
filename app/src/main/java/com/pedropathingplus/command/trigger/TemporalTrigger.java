package com.pedropathingplus.command.trigger;

import com.pedropathing.follower.Follower;

/**
 * Triggers that activate based on the elapsed execution time of the current path.
 */
public class TemporalTrigger extends Trigger {

    /**
     * Creates a trigger that activates after a specific amount of time has elapsed since the current path started.
     *
     * @param follower   The follower providing the current path execution time.
     * @param timeSeconds The target elapsed time in seconds.
     * @return A new trigger that activates after the time has elapsed.
     */
    public static TemporalTrigger timeElapsed(Follower follower, double timeSeconds) {
        return new TemporalTrigger(() -> {
            return follower.getCurrentPath() != null && follower.getCurrentPath().getPathTime() >= timeSeconds;
        });
    }

    /**
     * Creates a trigger that activates only while the elapsed time of the current path is within a specific range.
     *
     * @param follower     The follower providing the current path execution time.
     * @param minTimeSeconds The minimum elapsed time in seconds.
     * @param maxTimeSeconds The maximum elapsed time in seconds.
     * @return A new trigger that activates within the time range.
     */
    public static TemporalTrigger timeRange(Follower follower, double minTimeSeconds, double maxTimeSeconds) {
        return new TemporalTrigger(() -> {
            if (follower.getCurrentPath() == null) return false;
            double currentTime = follower.getCurrentPath().getPathTime();
            return currentTime >= minTimeSeconds && currentTime <= maxTimeSeconds;
        });
    }

    private TemporalTrigger(java.util.function.BooleanSupplier condition) {
        super(condition);
    }
}
