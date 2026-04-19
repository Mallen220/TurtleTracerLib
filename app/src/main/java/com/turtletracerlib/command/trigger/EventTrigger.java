package com.turtletracerlib.command.trigger;

import com.turtletracerlib.pathing.ProgressTracker;

/**
 * A trigger that is active when the robot is within a specific event zone along a path.
 * @deprecated Marked for removal.
 */
@Deprecated
public class EventTrigger extends Trigger {

    /**
     * Constructs a new EventTrigger for the given event name.
     *
     * @param tracker   The ProgressTracker instance to check for active events.
     * @param eventName The name of the event to trigger on.
     */
    public EventTrigger(ProgressTracker tracker, String eventName) {
        super(() -> tracker.isEventActive(eventName));
    }
}
