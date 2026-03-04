package com.pedropathingplus.command;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for selecting autonomous routines via the gamepad before the match starts.
 * <p>
 * This class allows users to register multiple autonomous commands and select one using the
 * gamepad D-Pad (Left/Right) during the initialization phase.
 * </p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * {@code
 * @Override
 * public void initialize() {
 *     // Clear previous state (important for static utilities)
 *     AutoChooser.clear();
 *
 *     // Register commands
 *     AutoChooser.add("Red Left", new RedLeftAuto());
 *     AutoChooser.add("Blue Right", new BlueRightAuto());
 *
 *     // Start the selection menu
 *     AutoChooser.start(telemetry, gamepad1);
 * }
 *
 * // If NOT using CommandScheduler in init_loop(), call this manually:
 * @Override
 * public void init_loop() {
 *     AutoChooser.update();
 * }
 *
 * @Override
 * public void start() {
 *     // Schedule the selected command
 *     AutoChooser.enable();
 * }
 * }
 * </pre>
 */
public class AutoChooser {

    private static class AutoOption {
        final String name;
        final Object command;

        AutoOption(String name, Object command) {
            this.name = name;
            this.command = command;
        }
    }

    // Static global state
    private static final List<AutoOption> options = new ArrayList<>();
    private static int selectedIndex = 0;

    // UI/Input references
    private static Telemetry telemetry;
    private static Gamepad gamepad;

    // Internal state for debouncing and logic
    private static long lastButtonPressTime = 0;
    private static final long DEBOUNCE_MS = 300;
    private static Command selectionCommand;

    /**
     * Adds an autonomous command to the chooser.
     *
     * @param name    The display name for the command.
     * @param command The command object (can be a Command instance, a Runnable, or any object if using ReflectiveCommandAdapter).
     */
    public static void add(String name, Object command) {
        options.add(new AutoOption(name, command));
    }

    /**
     * Clears all registered commands and resets the selection state.
     * Call this at the beginning of your initialize() method to ensure a clean state.
     */
    public static void clear() {
        options.clear();
        selectedIndex = 0;
        telemetry = null;
        gamepad = null;
        selectionCommand = null;
    }

    /**
     * Starts the selection menu.
     * <p>
     * This creates a background command that updates the selection logic and telemetry.
     * If you are using {@link CommandScheduler#run()} in your {@code init_loop()}, this command will run automatically.
     * Otherwise, you must call {@link #update()} manually in your {@code init_loop()}.
     * </p>
     *
     * @param telemetry The telemetry instance to display the menu.
     * @param gamepad   The gamepad to use for selection (D-Pad Left/Right).
     */
    public static void start(Telemetry telemetry, Gamepad gamepad) {
        AutoChooser.telemetry = telemetry;
        AutoChooser.gamepad = gamepad;

        selectionCommand = new SelectionCommand();
        CommandScheduler.getInstance().schedule(selectionCommand);
    }

    /**
     * Manually updates the selection logic.
     * Call this in {@code init_loop()} if you are NOT running the CommandScheduler during initialization.
     */
    public static void update() {
        if (gamepad == null || telemetry == null) return;
        if (options.isEmpty()) {
            telemetry.addLine("AutoChooser: No commands registered.");
            telemetry.update();
            return;
        }

        // Handle Input
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastButtonPressTime > DEBOUNCE_MS) {
            if (gamepad.dpad_right) {
                selectedIndex = (selectedIndex + 1) % options.size();
                lastButtonPressTime = currentTime;
            } else if (gamepad.dpad_left) {
                selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
                lastButtonPressTime = currentTime;
            }
        }

        // Update Telemetry
        telemetry.addLine("=== AUTONOMOUS SELECTION ===");
        telemetry.addLine("Use D-Pad Left/Right to choose");
        telemetry.addLine("");

        for (int i = 0; i < options.size(); i++) {
            if (i == selectedIndex) {
                telemetry.addLine(">>> " + options.get(i).name + " <<<");
            } else {
                telemetry.addLine("    " + options.get(i).name);
            }
        }
        telemetry.update();
    }

    /**
     * Finalizes the selection and schedules the chosen command.
     * Call this in your {@code start()} method.
     */
    public static void enable() {
        // Stop the selection command if it's running
        if (selectionCommand != null) {
            CommandScheduler.getInstance().cancel(selectionCommand);
            selectionCommand = null;
        }

        // Clear telemetry
        if (telemetry != null) {
            telemetry.clear();
            telemetry.update();
        }

        if (options.isEmpty()) return;

        // Schedule the selected command
        AutoOption selected = options.get(selectedIndex);
        if (selected.command != null) {
            CommandScheduler.getInstance().schedule(selected.command);

            if (telemetry != null) {
                telemetry.addData("AutoChooser", "Starting: " + selected.name);
                telemetry.update();
            }
        }
    }

    /**
     * Internal command to run the update loop via CommandScheduler.
     */
    private static class SelectionCommand implements Command {
        @Override
        public void execute() {
            AutoChooser.update();
        }

        @Override
        public boolean isFinished() {
            return false; // Runs until canceled by enable()
        }

        @Override
        public void end(boolean interrupted) {
             // Optional: Cleanup if needed
        }
    }
}
