package com.turtletracerlib.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A command group that runs a list of commands in sequence.
 * <p>
 * As each command finishes, the next one is started. The group finishes when the last command
 * in the sequence finishes. If the group is interrupted, the currently running command is interrupted.
 * </p>
 * @deprecated Marked for removal.
 */
@Deprecated
public class SequentialCommandGroup extends CommandGroupBase {

    /**
     * The list of commands to run in sequence.
     */
    private final List<Command> commands = new ArrayList<>();

    /**
     * The index of the currently running command. -1 indicates not started or finished.
     */
    private int currentCommandIndex = -1;

    /**
     * Creates a new SequentialCommandGroup with the given commands.
     *
     * @param commands The commands to run in sequence.
     */
    public SequentialCommandGroup(Command... commands) {
        addCommands(commands);
    }

    /**
     * Adds commands to the sequence.
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
     * Initializes the command group.
     * <p>
     * Starts the first command in the sequence.
     * </p>
     */
    @Override
    public void initialize() {
        currentCommandIndex = 0;
        if (!commands.isEmpty()) {
            commands.get(0).initialize();
        }
    }

    /**
     * Executes the currently running command.
     * <p>
     * Checks if the current command has finished. If so, it ends that command and starts the next one.
     * </p>
     */
    @Override
    public void execute() {
        if (commands.isEmpty()) {
            return;
        }

        if (currentCommandIndex >= commands.size()) {
            return;
        }

        Command currentCommand = commands.get(currentCommandIndex);
        currentCommand.execute();

        if (currentCommand.isFinished()) {
            currentCommand.end(false);
            currentCommandIndex++;
            if (currentCommandIndex < commands.size()) {
                commands.get(currentCommandIndex).initialize();
            }
        }
    }

    /**
     * Ends the command group.
     * <p>
     * If interrupted, interrupts the currently running command.
     * </p>
     *
     * @param interrupted whether the command group was interrupted.
     */
    @Override
    public void end(boolean interrupted) {
        if (interrupted && !commands.isEmpty() && currentCommandIndex > -1 && currentCommandIndex < commands.size()) {
            commands.get(currentCommandIndex).end(true);
        }
        currentCommandIndex = -1;
    }

    /**
     * Checks if the command group has finished.
     *
     * @return {@code true} if all commands in the sequence have finished, {@code false} otherwise.
     */
    @Override
    public boolean isFinished() {
        return currentCommandIndex >= commands.size();
    }
}
