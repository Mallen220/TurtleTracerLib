package com.turtletracerlib.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A base class for command groups (commands that contain other commands).
 * <p>
 * This class handles the logic for aggregating requirements from all component commands.
 * If any command within the group requires a subsystem, the entire group requires that subsystem.
 * </p>
 */
public abstract class CommandGroupBase implements Command {

    /**
     * The set of subsystems required by this command group.
     */
    protected final Set<Object> requirements = new HashSet<>();

    /**
     * Adds a collection of requirements to this command group.
     * <p>
     * This is typically called internally when commands are added to the group.
     * </p>
     *
     * @param requirements The set of subsystems to add.
     */
    public final void addRequirements(Set<Object> requirements) {
        this.requirements.addAll(requirements);
    }

    /**
     * Retrieves the set of subsystems required by this command group.
     *
     * @return The aggregated set of required subsystems.
     */
    @Override
    public Set<Object> getRequirements() {
        return requirements;
    }

    /**
     * Adds commands to the group.
     * <p>
     * Subclasses must implement this method to define how commands are stored and executed
     * (e.g., sequentially, in parallel).
     * </p>
     *
     * @param commands The commands to add to the group.
     */
    public abstract void addCommands(Command... commands);
}
