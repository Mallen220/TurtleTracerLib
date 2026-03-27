package com.turtletracerlib.command;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;

/**
 * A command that allows users to select an autonomous routine using the gamepad.
 * <p>
 * This class maintains a global list of registered autonomous options, which can be
 * populated from anywhere in the robot code. When scheduled, it displays the available
 * options on the Driver Station telemetry and allows the driver to cycle through them
 * using the D-Pad Left and D-Pad Right buttons.
 * </p>
 * <p>
 * The chooser automatically finishes when the OpMode is started or stopped. Upon finishing,
 * it schedules the selected autonomous command using the {@link CommandScheduler}.
 * </p>
 */
public class AutoChooser implements Command {

    /**
     * A helper class representing a registered autonomous option.
     */
    public static class AutoOption {
        private final String name;
        private final Object command;

        /**
         * Constructs an AutoOption.
         *
         * @param name    The display name of the autonomous routine.
         * @param command The command object representing the routine.
         */
        public AutoOption(String name, Object command) {
            this.name = name;
            this.command = command;
        }

        /**
         * Retrieves the display name of the option.
         *
         * @return The name of the routine.
         */
        public String getName() {
            return name;
        }

        /**
         * Retrieves the command object of the option.
         *
         * @return The command object.
         */
        public Object getCommand() {
            return command;
        }
    }

    /**
     * The global list of registered autonomous options.
     */
    private static final List<AutoOption> registeredAutos = new ArrayList<>();

    private final LinearOpMode opMode;
    private final Gamepad gamepad;
    private final Telemetry telemetry;

    private int selectedIndex = 0;
    private AutoOption selectedOption;
    private long lastButtonPressTime = 0;

    /**
     * Registers a new autonomous routine to be available in the chooser.
     *
     * @param name    The display name for the routine.
     * @param command The command object to run when this routine is selected.
     */
    public static void addOption(String name, Object command) {
        registeredAutos.add(new AutoOption(name, command));
    }

    /**
     * Clears the global list of registered autonomous options.
     * <p>
     * This should be called at the beginning of an OpMode to ensure options from
     * previous runs do not persist.
     * </p>
     */
    public static void clear() {
        registeredAutos.clear();
    }

    /**
     * Constructs a new AutoChooser.
     *
     * @param opMode    The active LinearOpMode, used to check if the OpMode has started or stopped.
     * @param gamepad   The gamepad used to navigate the menu (typically Gamepad 1).
     * @param telemetry The telemetry object used to display the menu to the Driver Station.
     */
    public AutoChooser(LinearOpMode opMode, Gamepad gamepad, Telemetry telemetry) {
        this.opMode = opMode;
        this.gamepad = gamepad;
        this.telemetry = telemetry;
        if (!registeredAutos.isEmpty()) {
            this.selectedOption = registeredAutos.get(0);
        }
    }

    /**
     * Initializes the AutoChooser, displaying the initial menu and resetting timers.
     */
    @Override
    public void initialize() {
        telemetry.addLine("Auto Selection Active");
        telemetry.addLine("Use D-Pad Left/Right to choose mode");
        telemetry.addLine("Gamepad will start selected auto");
        updateTelemetry();
    }

    /**
     * Periodically checks for gamepad input to navigate the menu and updates telemetry.
     */
    @Override
    public void execute() {
        if (registeredAutos.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        // 300ms debounce
        if (currentTime - lastButtonPressTime > 300) {
            if (gamepad.dpad_right) {
                selectedIndex = (selectedIndex + 1) % registeredAutos.size();
                selectedOption = registeredAutos.get(selectedIndex);
                lastButtonPressTime = currentTime;
                updateTelemetry();
            } else if (gamepad.dpad_left) {
                selectedIndex = (selectedIndex - 1 + registeredAutos.size()) % registeredAutos.size();
                selectedOption = registeredAutos.get(selectedIndex);
                lastButtonPressTime = currentTime;
                updateTelemetry();
            }
        }

        // Update telemetry periodically (roughly once per second)
        if (currentTime % 1000 < 50) {
            updateTelemetry();
        }
    }

    /**
     * Updates the Driver Station telemetry with the current list of options and the selected option.
     */
    private void updateTelemetry() {
        telemetry.clear();
        telemetry.addLine("=== AUTONOMOUS SELECTION ===");
        telemetry.addLine("Use D-Pad Left/Right to choose");
        telemetry.addLine("");

        if (registeredAutos.isEmpty()) {
            telemetry.addLine("No autonomous routines registered.");
        } else {
            for (int i = 0; i < registeredAutos.size(); i++) {
                if (i == selectedIndex) {
                    telemetry.addLine(">>> " + registeredAutos.get(i).getName() + " <<<");
                } else {
                    telemetry.addLine("    " + registeredAutos.get(i).getName());
                }
            }
        }

        telemetry.addLine("");
        telemetry.update();
    }

    /**
     * Checks if the active OpMode has been started or a stop has been requested.
     *
     * @return true if the OpMode is started or stopped, false otherwise.
     */
    @Override
    public boolean isFinished() {
        return opMode.isStarted() || opMode.isStopRequested();
    }

    /**
     * Ends the chooser, displaying the final selection and scheduling the selected command.
     *
     * @param interrupted whether the command was interrupted.
     */
    @Override
    public void end(boolean interrupted) {
        telemetry.clear();
        if (selectedOption != null && !opMode.isStopRequested()) {
            telemetry.addData("Selected Auto Mode", selectedOption.getName());
            telemetry.addLine("Starting autonomous...");
            telemetry.update();
            CommandScheduler.getInstance().schedule(selectedOption.getCommand());
        } else {
            telemetry.addLine("No auto mode selected or OpMode stopped.");
            telemetry.update();
        }
    }

    /**
     * Retrieves the currently selected autonomous option.
     *
     * @return The selected AutoOption, or null if no options are registered.
     */
    public AutoOption getSelectedOption() {
        return selectedOption;
    }
}
