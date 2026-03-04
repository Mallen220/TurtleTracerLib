package com.pedropathingplus.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pedropathingplus.command.trigger.Trigger;

/**
 * The core component of the command-based framework, responsible for managing command execution,
 * subsystem requirements, and periodic updates.
 * <p>
 * The scheduler orchestrates which commands run when, ensures that conflicting commands do not run
 * simultaneously on the same subsystem, and handles the lifecycle of commands (initialize, execute, end).
 * </p>
 * <p>
 * It uses a Singleton pattern, so there is only one scheduler instance per OpMode.
 * </p>
 */
public class CommandScheduler {

    /**
     * The single instance of the CommandScheduler.
     */
    private static CommandScheduler instance;

    /**
     * Maps each subsystem (as an Object) to the command currently requiring it.
     * This enforces the rule that a subsystem can only be used by one command at a time.
     */
    private final Map<Object, Command> requirements = new HashMap<>();

    /**
     * Maps each subsystem (as an Object) to its default command.
     * The default command runs automatically when no other command requires the subsystem.
     */
    private final Map<Object, Command> defaultCommands = new HashMap<>();

    /**
     * The list of currently scheduled (running) commands.
     */
    private final List<Command> scheduledCommands = new ArrayList<>();

    /**
     * The list of registered subsystems that need periodic updates.
     */
    private final List<Object> registeredSubsystems = new ArrayList<>();

    /**
     * A temporary list of commands to be scheduled in the next iteration of the run loop.
     * This prevents concurrent modification exceptions during iteration.
     */
    private final List<Command> toSchedule = new ArrayList<>();

    /**
     * A temporary list of commands to be canceled in the next iteration of the run loop.
     */
    private final List<Command> toCancel = new ArrayList<>();

    /**
     * A flag indicating whether the scheduler is currently iterating through the scheduled commands.
     * If true, modifications to the command list are deferred.
     */
    private boolean inRunLoop = false;

    /**
     * The list of registered triggers.
     */
    private final List<Trigger> activeTriggers = new ArrayList<>();

    /**
     * Retrieves the singleton instance of the {@code CommandScheduler}.
     *
     * @return The singleton instance.
     */
    public static synchronized CommandScheduler getInstance() {
        if (instance == null) {
            instance = new CommandScheduler();
        }
        return instance;
    }

    /**
     * Private constructor to enforce the Singleton pattern.
     */
    private CommandScheduler() {}

    /**
     * Registers a trigger to be polled periodically.
     *
     * @param trigger The trigger to register.
     */
    public void registerTrigger(Trigger trigger) {
        if (!activeTriggers.contains(trigger)) {
            activeTriggers.add(trigger);
        }
    }

    /**
     * Registers a subsystem to receive periodic updates.
     * <p>
     * If the object implements {@link Subsystem}, its {@code periodic()} method will be called.
     * If it is a generic object, the scheduler will attempt to find and call a {@code periodic()} method via reflection.
     * </p>
     *
     * @param subsystem The subsystem object to register.
     */
    public void registerSubsystem(Object subsystem) {
        if (!registeredSubsystems.contains(subsystem)) {
            registeredSubsystems.add(subsystem);
        }
    }

    /**
     * Sets the default command for a specific subsystem.
     * <p>
     * The default command is automatically scheduled whenever the subsystem is not required by any other running command.
     * </p>
     *
     * @param subsystem      The subsystem object.
     * @param defaultCommand The command to run by default.
     * @throws IllegalArgumentException If the default command does not require the specified subsystem.
     */
    public void setDefaultCommand(Object subsystem, Object defaultCommand) {
        Command cmd = asCommand(defaultCommand);
        if (!cmd.getRequirements().contains(subsystem)) {
            throw new IllegalArgumentException("Default command must require the subsystem");
        }
        defaultCommands.put(subsystem, cmd);
    }

    /**
     * Schedules one or more commands for execution.
     * <p>
     * If a command requires a subsystem that is already in use, the current command using that subsystem is interrupted.
     * Commands can be {@link Command} objects, {@link Runnable}s, or generic objects adapted via reflection.
     * </p>
     *
     * @param commands The commands or objects to schedule.
     */
    public void schedule(Object... commands) {
        if (inRunLoop) {
            for (Object command : commands) {
                toSchedule.add(asCommand(command));
            }
            return;
        }

        for (Object commandObj : commands) {
            Command command = asCommand(commandObj);

            if (scheduledCommands.contains(command)) continue;

            // Check requirements and resolve conflicts
            Set<Object> requirementsSet = command.getRequirements();
            for (Object subsystem : requirementsSet) {
                if (requirements.containsKey(subsystem)) {
                    Command currentCommand = requirements.get(subsystem);
                    // Cancel the current command using this subsystem
                    cancel(currentCommand);
                }
            }

            // Schedule the command
            initCommand(command, requirementsSet);
        }
    }

    /**
     * Converts a generic object into a {@link Command}.
     * <ul>
     *   <li>If it's already a {@code Command}, it casts it.</li>
     *   <li>If it's a {@code Runnable}, it wraps it in an {@link InstantCommand}.</li>
     *   <li>Otherwise, it wraps it in a {@link ReflectiveCommandAdapter}.</li>
     * </ul>
     *
     * @param commandObj The object to convert.
     * @return The resulting {@link Command}.
     */
    private Command asCommand(Object commandObj) {
        if (commandObj instanceof Command) {
            return (Command) commandObj;
        } else if (commandObj instanceof Runnable) {
            return new InstantCommand((Runnable) commandObj);
        } else {
            return new ReflectiveCommandAdapter(commandObj);
        }
    }

    /**
     * Initializes a command and registers its requirements.
     *
     * @param command         The command to initialize.
     * @param requirementsSet The set of subsystems required by the command.
     */
    private void initCommand(Command command, Set<Object> requirementsSet) {
        command.initialize();
        scheduledCommands.add(command);
        for (Object subsystem : requirementsSet) {
            requirements.put(subsystem, command);
        }
    }

    /**
     * Cancels one or more currently running commands.
     * <p>
     * Calls {@link Command#end(boolean)} with {@code interrupted = true} for each canceled command.
     * </p>
     *
     * @param commands The commands to cancel.
     */
    public void cancel(Command... commands) {
        if (inRunLoop) {
            for (Command command : commands) {
                toCancel.add(command);
            }
            return;
        }

        for (Command command : commands) {
            if (!scheduledCommands.contains(command)) continue;

            command.end(true);
            scheduledCommands.remove(command);

            // Remove from requirements map
            for (Object subsystem : command.getRequirements()) {
                requirements.remove(subsystem);
            }
        }
    }

    /**
     * The main run loop of the scheduler.
     * <p>
     * This method should be called periodically (e.g., in {@code loop()} or inside a while loop).
     * It performs the following actions:
     * <ol>
     *   <li>Runs periodic updates for all registered subsystems.</li>
     *   <li>Executes the {@code execute()} method for all scheduled commands.</li>
     *   <li>Checks if commands are finished and calls {@code end()}.</li>
     *   <li>Processes pending scheduling and cancellation requests.</li>
     *   <li>Schedules default commands for idle subsystems.</li>
     * </ol>
     * </p>
     */
    public void run() {
        inRunLoop = true;

        // Poll triggers
        for (Trigger trigger : activeTriggers) {
            trigger.poll();
        }

        // Run subsystem periodic methods
        for (Object subsystem : registeredSubsystems) {
            runPeriodic(subsystem);
        }

        // Run scheduled commands
        Iterator<Command> iterator = scheduledCommands.iterator();
        while (iterator.hasNext()) {
            Command command = iterator.next();
            command.execute();
            if (command.isFinished()) {
                command.end(false);
                iterator.remove();

                // Remove from requirements map
                for (Object subsystem : command.getRequirements()) {
                    requirements.remove(subsystem);
                }
            }
        }

        inRunLoop = false;

        // Process pending scheduling and cancellations
        if (!toSchedule.isEmpty()) {
            Command[] cmds = toSchedule.toArray(new Command[0]);
            toSchedule.clear();
            schedule((Object[])cmds);
        }

        if (!toCancel.isEmpty()) {
            Command[] cmds = toCancel.toArray(new Command[0]);
            toCancel.clear();
            cancel(cmds);
        }

        // Schedule default commands if needed
        for (Object subsystem : registeredSubsystems) {
            if (!requirements.containsKey(subsystem) && defaultCommands.containsKey(subsystem)) {
                schedule(defaultCommands.get(subsystem));
            }
        }
    }

    /**
     * Executes the periodic method of a subsystem.
     * <p>
     * Uses reflection if the object does not implement the {@link Subsystem} interface.
     * </p>
     *
     * @param subsystem The subsystem object.
     */
    private void runPeriodic(Object subsystem) {
        if (subsystem instanceof Subsystem) {
            ((Subsystem) subsystem).periodic();
        } else {
            try {
                Method periodic = subsystem.getClass().getMethod("periodic");
                periodic.invoke(subsystem);
            } catch (NoSuchMethodException ignored) {
                // If it doesn't have periodic, that's fine.
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Resets the scheduler by clearing all internal state.
     * <p>
     * This stops all running commands, clears registered subsystems and default commands.
     * Use this method between tests or OpModes to ensure a clean slate.
     * </p>
     */
    public void reset() {
        // Clear all internal state
        scheduledCommands.clear();
        activeTriggers.clear();
        requirements.clear();
        registeredSubsystems.clear();
        defaultCommands.clear();
        toSchedule.clear();
        toCancel.clear();
        inRunLoop = false;
    }
}
