package com.turtletracerlib.command;

import java.util.HashMap;
import java.util.Map;

/**
 * A command group that runs a set of commands in parallel.
 * <p>
 * The group ends when all commands in the group have finished.
 * If the group is interrupted, all running commands are interrupted.
 * </p>
 */
public class ParallelCommandGroup extends CommandGroupBase {

    /**
     * A map of commands to their running status (true if running, false if finished).
     */
    private final Map<Command, Boolean> commands = new HashMap<>();

    /**
     * Creates a new ParallelCommandGroup with the given commands.
     *
     * @param commands The commands to run in parallel.
     */
    public ParallelCommandGroup(Command... commands) {
        addCommands(commands);
    }

    /**
     * Adds commands to the group.
     * <p>
     * Also aggregates requirements from all added commands.
     * </p>
     *
     * @param commands The commands to add.
     */
    @Override
    public void addCommands(Command... commands) {
        for (Command command : commands) {
            this.commands.put(command, false);
            addRequirements(command.getRequirements());
        }
    }

    /**
     * Initializes the command group.
     * <p>
     * Starts all commands in the group.
     * </p>
     */
    @Override
    public void initialize() {
        for (Map.Entry<Command, Boolean> entry : commands.entrySet()) {
            entry.getKey().initialize();
            entry.setValue(true); // mark as running
        }
    }

    /**
     * Executes all running commands.
     * <p>
     * Checks if each command has finished. If so, it ends that command and marks it as finished.
     * </p>
     */
    @Override
    public void execute() {
        for (Map.Entry<Command, Boolean> entry : commands.entrySet()) {
            if (!entry.getValue()) continue; // already finished

            Command command = entry.getKey();
            command.execute();
            if (command.isFinished()) {
                command.end(false);
                entry.setValue(false); // mark as finished
            }
        }
    }

    /**
     * Ends the command group.
     * <p>
     * If interrupted, interrupts all currently running commands.
     * </p>
     *
     * @param interrupted whether the command group was interrupted.
     */
    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            for (Map.Entry<Command, Boolean> entry : commands.entrySet()) {
                if (entry.getValue()) {
                    entry.getKey().end(true);
                }
            }
        }
    }

    /**
     * Checks if the command group has finished.
     *
     * @return {@code true} if all commands in the group have finished, {@code false} otherwise.
     */
    @Override
    public boolean isFinished() {
        for (Boolean running : commands.values()) {
            if (running) return false;
        }
        return true;
    }
}
