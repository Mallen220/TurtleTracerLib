package com.pedropathingplus.command.trigger;

import com.pedropathingplus.command.Command;
import com.pedropathingplus.command.CommandScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A class that evaluates a boolean condition and schedules commands based on state changes.
 * <p>
 * This class is the core of the new trigger framework, allowing commands to be scheduled based on
 * complex boolean logic, continuous conditions, and zoned events.
 * </p>
 */
public class Trigger {
    protected final BooleanSupplier condition;
    private boolean previousState = false;

    // Callbacks to run on specific state transitions
    private final List<Runnable> onTrueRunnables = new ArrayList<>();
    private final List<Runnable> whileTrueRunnables = new ArrayList<>();
    private final List<Runnable> onFalseRunnables = new ArrayList<>();

    /**
     * Constructs a new Trigger with the given condition.
     *
     * @param condition The condition to evaluate.
     */
    public Trigger(BooleanSupplier condition) {
        this.condition = condition;
    }

    /**
     * Evaluates the condition and runs any scheduled callbacks based on state changes.
     * This method is called continuously by the CommandScheduler.
     */
    public void poll() {
        boolean currentState = condition.getAsBoolean();

        // Rising edge (false -> true)
        if (currentState && !previousState) {
            for (Runnable runnable : onTrueRunnables) {
                runnable.run();
            }
        }

        // Continuous true
        if (currentState) {
            for (Runnable runnable : whileTrueRunnables) {
                runnable.run();
            }
        }

        // Falling edge (true -> false)
        if (!currentState && previousState) {
            for (Runnable runnable : onFalseRunnables) {
                runnable.run();
            }
        }

        previousState = currentState;
    }

    /**
     * Schedules the command when the condition changes from false to true.
     *
     * @param command The command to schedule.
     * @return This trigger for chaining.
     */
    public Trigger onTrue(Command command) {
        onTrueRunnables.add(command::schedule);
        CommandScheduler.getInstance().registerTrigger(this);
        return this;
    }

    /**
     * Schedules the command when the condition becomes true, and cancels it when the condition becomes false.
     *
     * @param command The command to schedule.
     * @return This trigger for chaining.
     */
    public Trigger whileTrue(Command command) {
        onTrueRunnables.add(command::schedule);
        onFalseRunnables.add(() -> CommandScheduler.getInstance().cancel(command));
        CommandScheduler.getInstance().registerTrigger(this);
        return this;
    }

    /**
     * Schedules the command when the condition changes from true to false.
     *
     * @param command The command to schedule.
     * @return This trigger for chaining.
     */
    public Trigger onFalse(Command command) {
        onFalseRunnables.add(command::schedule);
        CommandScheduler.getInstance().registerTrigger(this);
        return this;
    }

    /**
     * Composes this trigger with another trigger, returning a new trigger that is true when both are true.
     *
     * @param other The other trigger to combine with.
     * @return A new trigger representing the logical AND of both.
     */
    public Trigger and(Trigger other) {
        return new Trigger(() -> this.condition.getAsBoolean() && other.condition.getAsBoolean());
    }

    /**
     * Composes this trigger with another trigger, returning a new trigger that is true when either is true.
     *
     * @param other The other trigger to combine with.
     * @return A new trigger representing the logical OR of both.
     */
    public Trigger or(Trigger other) {
        return new Trigger(() -> this.condition.getAsBoolean() || other.condition.getAsBoolean());
    }
}
