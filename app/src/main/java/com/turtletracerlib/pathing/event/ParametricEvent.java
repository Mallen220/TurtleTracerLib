package com.turtletracerlib.pathing.event;

import com.turtletracerlib.pathing.EventZone;
import java.util.function.DoubleSupplier;

/**
 * An event that triggers when a path's progress reaches a specified threshold or enters an {@link EventZone}.
 */
public class ParametricEvent extends PathEvent {

    private final DoubleSupplier progressSupplier;
    private final EventZone zone;

    /**
     * Constructs a ParametricEvent that triggers when progress reaches or exceeds the target.
     *
     * @param progressSupplier Supplier providing current progress (0.0 to 1.0).
     * @param targetProgress   The threshold at which this event triggers (0.0 to 1.0).
     * @param action           The action to execute.
     */
    public ParametricEvent(DoubleSupplier progressSupplier, double targetProgress, Runnable action) {
        this(progressSupplier, new EventZone(targetProgress, 1.0), action);
    }

    /**
     * Constructs a ParametricEvent that triggers within a progress range.
     *
     * @param progressSupplier Supplier providing current progress (0.0 to 1.0).
     * @param startProgress    Start threshold (0.0 to 1.0).
     * @param endProgress      End threshold (0.0 to 1.0).
     * @param action           The action to execute.
     */
    public ParametricEvent(DoubleSupplier progressSupplier, double startProgress, double endProgress, Runnable action) {
        this(progressSupplier, new EventZone(startProgress, endProgress), action);
    }

    /**
     * Constructs a ParametricEvent using a defined {@link EventZone}.
     *
     * @param progressSupplier Supplier providing current progress (0.0 to 1.0).
     * @param zone             The event zone within which the event triggers.
     * @param action           The action to execute.
     */
    public ParametricEvent(DoubleSupplier progressSupplier, EventZone zone, Runnable action) {
        super(action);
        this.progressSupplier = progressSupplier;
        this.zone = zone;
    }

    @Override
    public boolean isConditionMet() {
        if (progressSupplier == null || zone == null) {
            return false;
        }
        double progress = progressSupplier.getAsDouble();
        return zone.contains(progress) || progress >= zone.getStartPosition();
    }

    /**
     * Gets the {@link EventZone} associated with this event.
     *
     * @return The event zone.
     */
    public EventZone getZone() {
        return zone;
    }
}
