package com.turtletracerlib.command;

import java.util.function.BooleanSupplier;

/**
 * A command that runs indefinitely until a specified condition becomes true.
 * <p>
 * This is useful for waiting for sensor conditions, such as "wait until limit switch is pressed"
 * or "wait until distance < 10cm".
 * </p>
 */
public class WaitUntilCommand implements Command {

    /**
     * The condition to check repeatedly.
     */
    private final BooleanSupplier condition;

    /**
     * Creates a new WaitUntilCommand that waits for the given condition to be true.
     *
     * @param condition The {@link BooleanSupplier} that returns true when the command should finish.
     */
    public WaitUntilCommand(BooleanSupplier condition) {
        this.condition = condition;
    }

    /**
     * Checks if the condition is met.
     *
     * @return {@code true} if the condition supplier returns true, {@code false} otherwise.
     */
    @Override
    public boolean isFinished() {
        return condition.getAsBoolean();
    }
}
