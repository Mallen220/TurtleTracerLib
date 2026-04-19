package com.turtletracerlib.command;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A command that runs a {@link Runnable} once and finishes immediately.
 * <p>
 * This is useful for one-off actions like setting a servo position, logging a message,
 * or triggering a state change that doesn't need to persist over time.
 * </p>
 * @deprecated Marked for removal.
 */
@Deprecated
public class InstantCommand implements Command {

    /**
     * The runnable action to execute.
     */
    private final Runnable toRun;

    /**
     * The set of subsystems required by this command.
     */
    private final Set<Object> requirements;

    /**
     * Creates a new InstantCommand that runs the given Runnable and requires the specified subsystems.
     *
     * @param toRun        The Runnable to execute.
     * @param requirements The subsystems required by this command (optional).
     */
    public InstantCommand(Runnable toRun, Object... requirements) {
        this.toRun = toRun;
        this.requirements = new HashSet<>();
        Collections.addAll(this.requirements, requirements);
    }

    /**
     * Creates a new InstantCommand that does nothing (no-op).
     * <p>
     * Useful as a placeholder or default command.
     * </p>
     */
    public InstantCommand() {
        this(() -> {});
    }

    /**
     * Executes the runnable action.
     * <p>
     * Called once when the command is initialized.
     * </p>
     */
    @Override
    public void initialize() {
        toRun.run();
    }

    /**
     * Returns true immediately, as this command finishes instantly.
     *
     * @return always {@code true}.
     */
    @Override
    public boolean isFinished() {
        return true;
    }

    /**
     * Retrieves the set of subsystems required by this command.
     *
     * @return The set of required subsystems.
     */
    @Override
    public Set<Object> getRequirements() {
        return requirements;
    }
}
