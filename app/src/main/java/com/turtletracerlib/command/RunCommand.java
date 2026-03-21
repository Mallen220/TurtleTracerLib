package com.turtletracerlib.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A command that runs a {@link Runnable} repeatedly until interrupted or finished.
 * <p>
 * This is commonly used as a default command for subsystems (e.g., continuously checking joystick input
 * and updating motor power for a drivetrain).
 * </p>
 */
public class RunCommand implements Command {

    /**
     * The runnable action to execute repeatedly.
     */
    private final Runnable toRun;

    /**
     * The set of subsystems required by this command.
     */
    private final Set<Object> requirements;

    /**
     * Creates a new RunCommand that repeatedly executes the given Runnable and requires the specified subsystems.
     *
     * @param toRun        The Runnable to execute.
     * @param requirements The subsystems required by this command (optional).
     */
    public RunCommand(Runnable toRun, Object... requirements) {
        this.toRun = toRun;
        this.requirements = new HashSet<>(Arrays.asList(requirements));
    }

    /**
     * Executes the runnable action repeatedly.
     * <p>
     * Called in every loop iteration while the command is scheduled.
     * </p>
     */
    @Override
    public void execute() {
        toRun.run();
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
