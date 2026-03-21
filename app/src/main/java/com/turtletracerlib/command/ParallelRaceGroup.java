package com.turtletracerlib.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A command group that runs a set of commands in parallel until ONE of them finishes.
 * <p>
 * This is useful for "race" conditions, such as "drive forward until a sensor is triggered OR time runs out".
 * When the first command finishes, all other running commands are interrupted.
 * </p>
 */
public class ParallelRaceGroup extends CommandGroupBase {

    /**
     * The list of commands to run in parallel race.
     */
    private final List<Command> commands = new ArrayList<>();

    /**
     * A flag indicating if the race has finished (one command has finished).
     */
    private boolean finished = false;

    /**
     * Creates a new ParallelRaceGroup with the given commands.
     *
     * @param commands The commands to race against each other.
     */
    public ParallelRaceGroup(Command... commands) {
        addCommands(commands);
    }

    /**
     * Adds commands to the race group.
     * <p>
     * Also aggregates requirements from all added commands.
     * </p>
     *
     * @param commands The commands to add.
     */
    @Override
    public void addCommands(Command... commands) {
        this.commands.addAll(Arrays.asList(commands));
        for (Command command : commands) {
            addRequirements(command.getRequirements());
        }
    }

    /**
     * Initializes the race group.
     * <p>
     * Starts all commands in the group.
     * </p>
     */
    @Override
    public void initialize() {
        finished = false;
        for (Command command : commands) {
            command.initialize();
        }
    }

    /**
     * Executes all running commands.
     * <p>
     * Checks if any command has finished. If so, it ends that command and marks the race as finished.
     * </p>
     */
    @Override
    public void execute() {
        if (finished) return;

        for (Command command : commands) {
            command.execute();
            if (command.isFinished()) {
                finished = true;
                command.end(false);
            }
        }
    }

    /**
     * Ends the race group.
     * <p>
     * Interrupts all commands that are still running (didn't win the race).
     * </p>
     *
     * @param interrupted whether the race group itself was interrupted.
     */
    @Override
    public void end(boolean interrupted) {
        for (Command command : commands) {
            // interrupt all commands that haven't finished yet
             if (!command.isFinished()) {
                 command.end(true);
             }
        }
    }

    /**
     * Checks if the race has finished.
     *
     * @return {@code true} if any command has finished, {@code false} otherwise.
     */
    @Override
    public boolean isFinished() {
        return finished;
    }
}
