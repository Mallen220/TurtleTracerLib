package com.turtletracerlib.command;

/**
 * A subsystem is a specific component or mechanism of the robot, such as a drivetrain, arm, or claw.
 * <p>
 * Subsystems are used to specify requirements for commands, ensuring that multiple commands do not
 * attempt to control the same hardware simultaneously (e.g., one command trying to raise the arm while
 * another tries to lower it).
 * </p>
 * <p>
 * This interface provides a standard structure for subsystems, including methods for periodic updates
 * and default commands. However, the {@link CommandScheduler} can manage any object as a subsystem,
 * provided it is registered.
 * </p>
 */
public interface Subsystem {
    /**
     * This method is called periodically by the {@link CommandScheduler} (typically once per loop iteration).
     * <p>
     * Use this for updating subsystem state, reading sensors, or performing continuous checks (e.g., limit switches).
     * </p>
     */
    default void periodic() {}

    /**
     * Registers this subsystem with the {@link CommandScheduler}.
     * <p>
     * This is necessary for the scheduler to call {@link #periodic()} and manage default commands.
     * </p>
     */
    default void register() {
        CommandScheduler.getInstance().registerSubsystem(this);
    }

    /**
     * Sets the default command for this subsystem.
     * <p>
     * The default command runs automatically whenever no other command is currently requiring this subsystem.
     * This is useful for idle behaviors (e.g., holding position, driving with joystick input).
     * </p>
     *
     * @param defaultCommand the command to run by default.
     */
    default void setDefaultCommand(Command defaultCommand) {
        CommandScheduler.getInstance().setDefaultCommand(this, defaultCommand);
    }
}
