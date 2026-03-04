package com.pedropathingplus.pathing;

/**
 * Represents a zone of progression along a path or turn.
 * Used for triggering commands when the robot's progress falls within the defined range.
 */
public class EventZone {
    private final double startPosition;
    private final double endPosition;

    /**
     * Constructs a new EventZone.
     *
     * @param startPosition The start progress of the zone (0.0 to 1.0).
     * @param endPosition   The end progress of the zone (0.0 to 1.0).
     */
    public EventZone(double startPosition, double endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     * Checks if the given progress is within the zone.
     *
     * @param progress The current progress (0.0 to 1.0).
     * @return True if the progress is within the zone, false otherwise.
     */
    public boolean contains(double progress) {
        return progress >= startPosition && progress <= endPosition;
    }

    /**
     * Gets the start position of the zone.
     *
     * @return The start position.
     */
    public double getStartPosition() {
        return startPosition;
    }

    /**
     * Gets the end position of the zone.
     *
     * @return The end position.
     */
    public double getEndPosition() {
        return endPosition;
    }
}
