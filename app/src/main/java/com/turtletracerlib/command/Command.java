package com.turtletracerlib.command;

import java.util.Collections;
import java.util.Set;

/**
 * A state machine representing a complete action to be performed by the robot.
 * <p>
 * Commands are the core unit of operation in the command-based framework. They encapsulate
 * logic that runs over time (e.g., driving a distance, moving an arm) and can be scheduled,
 * interrupted, and composed into groups.
 * </p>
 * <p>
 * A command's lifecycle consists of:
 * <ol>
 *   <li>{@link #initialize()} - Called once when the command is scheduled.</li>
 *   <li>{@link #execute()} - Called repeatedly while the command is running.</li>
 *   <li>{@link #isFinished()} - Checked after every execution to see if the command should end.</li>
 *   <li>{@link #end(boolean)} - Called once when the command finishes or is interrupted.</li>
 * </ol>
 * </p>
 * @deprecated Marked for removal.
 */
@Deprecated
public interface Command {
    /**
     * The initial state of a command. Called once when the command is scheduled.
     * <p>
     * Use this method to set up the command's internal state, reset sensors, or prepare hardware.
     * </p>
     */
    default void initialize() {}

    /**
     * The execution state of a command. Called repeatedly while the command is scheduled.
     * <p>
     * This is the main loop of the command where the robot's logic is performed.
     * </p>
     */
    default void execute() {}

    /**
     * The ending state of a command. Called once when the command ends or is interrupted.
     * <p>
     * Use this method to safely shut down hardware (e.g., stop motors) and clean up resources.
     * </p>
     *
     * @param interrupted whether the command was interrupted/canceled (true) or finished naturally (false).
     */
    default void end(boolean interrupted) {}

    /**
     * specifices whether the command has finished. Once a command finishes, the scheduler will call its
     * {@link #end(boolean)} method and un-schedule it.
     *
     * @return {@code true} if the command has finished, {@code false} otherwise.
     */
    default boolean isFinished() {
        return false;
    }

    /**
     * Specifies the set of subsystems used by this command.
     * <p>
     * Two commands cannot use the same subsystem at the same time. If a new command is scheduled
     * that requires a subsystem already in use, the currently running command will be interrupted.
     * </p>
     *
     * @return the set of subsystems required by this command.
     */
    default Set<Object> getRequirements() {
        return Collections.emptySet();
    }

    /**
     * Specifies whether the command is interruptible.
     * <p>
     * If false, the command cannot be interrupted by other commands that share its requirements.
     * </p>
     *
     * @return true if the command is interruptible, false otherwise.
     */
    default boolean isInterruptible() {
        return true;
    }

    /**
     * Schedules this command for execution.
     * <p>
     * This is a convenience method that delegates to {@link CommandScheduler#schedule(Object...)}.
     * </p>
     */
    default void schedule() {
        CommandScheduler.getInstance().schedule(this);
    }

    /**
     * Schedules this command for execution with a specific interruptibility.
     * <p>
     * This is a convenience method that delegates to {@link CommandScheduler#scheduleWithInterrupt(boolean, Object...)}.
     * </p>
     *
     * @param interruptible whether the command is interruptible.
     */
    default void schedule(boolean interruptible) {
        CommandScheduler.getInstance().scheduleWithInterrupt(interruptible, this);
    }
}
