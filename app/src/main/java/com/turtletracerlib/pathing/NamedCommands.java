package com.turtletracerlib.pathing;

import java.util.HashMap;
import java.util.Map;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * A utility class for globally registering and retrieving commands by name.
 * <p>
 * This class facilitates the integration of text-based command scheduling (e.g., from path files or dashboards)
 * with the robot's command framework. It acts as a central registry where commands can be stored and later
 * looked up to be executed dynamically.
 * </p>
 * <p>
 * This is particularly useful for autonomous routines where events are triggered by string keys in a data file.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // In RobotContainer or Init:
 * NamedCommands.registerCommand("IntakeOn", () -> intake.on());
 * NamedCommands.registerCommand("Shoot", () -> shooter.shoot());
 *
 * // In Autonomous execution:
 * NamedCommands.getCommand("IntakeOn").run();
 * }</pre>
 */
public class NamedCommands {

  /**
   * The map storing registered commands, keyed by their unique name.
   */
  private static final Map<String, Runnable> commands = new HashMap<>();

  /**
   * The map storing descriptions for registered commands, keyed by the command name.
   */
  private static final Map<String, String> commandDescriptions = new HashMap<>();

  /**
   * Registers a command with a specific name.
   *
   * @param name    The unique name to register the command under (case-sensitive, trimmed).
   * @param command The command object to register. Must not be null.
   * @throws IllegalArgumentException If the name is null/empty or the command is null.
   */
  public static void registerCommand(String name, Runnable command) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Command name cannot be null or empty");
    }

    if (command == null) {
      throw new IllegalArgumentException("Command cannot be null");
    }

    String trimmedName = name.trim();
    commands.put(trimmedName, command);
    commandDescriptions.put(trimmedName, command.getClass().getSimpleName());
  }

  /**
   * Registers a command with a specific name and a custom description.
   *
   * @param name        The unique name to register the command under.
   * @param command     The command object to register.
   * @param description A human-readable description of what the command does.
   */
  public static void registerCommand(String name, Runnable command, String description) {
    registerCommand(name, command);
    commandDescriptions.put(name.trim(), description);
  }

  /**
   * Retrieves a registered command by its name.
   * <p>
   * If the command is not found, a warning is printed to the standard error output, and a safe,
   * no-op {@link Runnable} is returned to prevent crashes during execution.
   * </p>
   *
   * @param name The name of the command to retrieve.
   * @return The registered {@link Runnable}, or a no-op command if not found.
   * @throws IllegalArgumentException If the provided name is null or empty.
   */
  public static Runnable getCommand(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Command name cannot be null or empty");
    }

    String trimmedName = name.trim();
    Runnable command = commands.get(trimmedName);

    if (command == null) {
      System.err.println("Warning: No command registered with name: " + trimmedName);
      // Return a safe no-op command instead of null
      return () -> System.out.println("Warning: Attempted to execute unregistered command: " + trimmedName);
    }

    return command;
  }

  /**
   * Checks if a command is currently registered with the given name.
   *
   * @param name The name to check.
   * @return {@code true} if a command is registered with that name, {@code false} otherwise.
   */
  public static boolean hasCommand(String name) {
    if (name == null) return false;
    return commands.containsKey(name.trim());
  }

  /**
   * Retrieves the description of a registered command.
   *
   * @param name The name of the command.
   * @return The description of the command, or an empty string if the command is not found.
   */
  public static String getCommandDescription(String name) {
    if (name == null) return "";
    return commandDescriptions.getOrDefault(name.trim(), "");
  }

  /**
   * Retrieves an array of all currently registered command names.
   *
   * @return An array of strings containing the names of all registered commands.
   */
  public static String[] getAllCommandNames() {
    return commands.keySet().toArray(new String[0]);
  }

  /**
   * Clears all registered commands and their descriptions.
   * <p>
   * This is useful for resetting the state between tests or OpModes to ensure a clean environment.
   * </p>
   */
  public static void clearAllCommands() {
    commands.clear();
    commandDescriptions.clear();
  }

  /**
   * Removes a specific command from the registry.
   *
   * @param name The name of the command to remove.
   * @return {@code true} if the command was successfully found and removed, {@code false} otherwise.
   */
  public static boolean removeCommand(String name) {
    if (name == null) return false;

    String trimmedName = name.trim();
    boolean removed = commands.remove(trimmedName) != null;
    commandDescriptions.remove(trimmedName);

    return removed;
  }

  /**
   * Returns the total number of registered commands.
   *
   * @return The count of registered commands.
   */
  public static int getCommandCount() {
    return commands.size();
  }

  /**
   * Prints a list of all registered commands and their descriptions to the provided telemetry.
   * <p>
   * This is intended for debugging purposes to verify which commands are available.
   * </p>
   *
   * @param tell The {@link Telemetry} object to print the list to.
   */
  public static void listAllCommands(Telemetry tell) {
    tell.addLine("=== Registered NamedCommands ===");
    for (String name : commands.keySet()) {
      tell.addLine(
          name
              + "  |  "
              + commands.get(name).getClass().getSimpleName()
              + "  |  "
              + getCommandDescription(name));

      //      System.out.printf(
      //          "%-20s -> %s (%s)%n",
      //          name, commands.get(name).getClass().getSimpleName(), getCommandDescription(name));
    }
    tell.addLine("Total: " + getCommandCount() + " commands");
    tell.addLine("===============================");
  }
}
