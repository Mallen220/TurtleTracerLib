package com.turtletracerlib.command;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * A utility class that wraps an arbitrary object and adapts it to the {@link Command} interface.
 * <p>
 * This class uses reflection to look for methods matching the standard command lifecycle names:
 * <ul>
 *   <li>{@code initialize()}</li>
 *   <li>{@code execute()}</li>
 *   <li>{@code isFinished()} (returns boolean)</li>
 *   <li>{@code end(boolean)} or {@code end()}</li>
 *   <li>{@code getRequirements()} (returns Set)</li>
 * </ul>
 * This allows integrating commands from other libraries or simple objects without implementing the interface directly.
 * </p>
 */
public class ReflectiveCommandAdapter implements Command {

    /**
     * The target object being adapted.
     */
    private final Object target;

    // Reflected methods
    private Method initializeMethod;
    private Method executeMethod;
    private Method isFinishedMethod;
    private Method endMethod; // end(boolean)
    private Method endNoArgMethod; // end()
    private Method getRequirementsMethod;

    /**
     * Creates a new ReflectiveCommandAdapter for the given target object.
     * <p>
     * Scans the target object's class for matching methods.
     * </p>
     *
     * @param target The object to wrap. Must not be null.
     * @throws IllegalArgumentException If the target is null.
     */
    public ReflectiveCommandAdapter(Object target) {
        if (target == null) throw new IllegalArgumentException("Target object cannot be null");
        this.target = target;

        Class<?> clazz = target.getClass();

        try { initializeMethod = clazz.getMethod("initialize"); } catch (NoSuchMethodException ignored) {}
        try { executeMethod = clazz.getMethod("execute"); } catch (NoSuchMethodException ignored) {}
        try { isFinishedMethod = clazz.getMethod("isFinished"); } catch (NoSuchMethodException ignored) {}
        try { endMethod = clazz.getMethod("end", boolean.class); } catch (NoSuchMethodException ignored) {}
        try { endNoArgMethod = clazz.getMethod("end"); } catch (NoSuchMethodException ignored) {}
        try { getRequirementsMethod = clazz.getMethod("getRequirements"); } catch (NoSuchMethodException ignored) {}
    }

    /**
     * Invokes the {@code initialize()} method on the target object, if it exists.
     */
    @Override
    public void initialize() {
        if (initializeMethod != null) {
            try {
                initializeMethod.invoke(target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Invokes the {@code execute()} method on the target object, if it exists.
     */
    @Override
    public void execute() {
        if (executeMethod != null) {
            try {
                executeMethod.invoke(target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Invokes the {@code isFinished()} method on the target object, if it exists.
     *
     * @return The result of the method call, or {@code false} if the method does not exist or fails.
     */
    @Override
    public boolean isFinished() {
        if (isFinishedMethod != null) {
            try {
                return (boolean) isFinishedMethod.invoke(target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Invokes the {@code end(boolean)} or {@code end()} method on the target object.
     * <p>
     * Prioritizes {@code end(boolean)} if available.
     * </p>
     *
     * @param interrupted whether the command was interrupted.
     */
    @Override
    public void end(boolean interrupted) {
        try {
            if (endMethod != null) {
                endMethod.invoke(target, interrupted);
            } else if (endNoArgMethod != null) {
                endNoArgMethod.invoke(target);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Invokes the {@code getRequirements()} method on the target object, if it exists.
     *
     * @return The returned Set of requirements, or an empty set if not available.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<Object> getRequirements() {
        if (getRequirementsMethod != null) {
            try {
                Object result = getRequirementsMethod.invoke(target);
                if (result instanceof Set) {
                    return (Set<Object>) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Collections.emptySet();
    }

    /**
     * Returns a string representation of the adapter.
     *
     * @return A string identifying the adapted class.
     */
    @Override
    public String toString() {
        return "Adapter(" + target.getClass().getSimpleName() + ")";
    }

    /**
     * Checks if this adapter is equal to another object.
     * <p>
     * Adapters are considered equal if they wrap the same target object.
     * </p>
     *
     * @param o The object to compare.
     * @return {@code true} if the objects are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReflectiveCommandAdapter that = (ReflectiveCommandAdapter) o;
        return target.equals(that.target);
    }

    /**
     * Returns the hash code of the target object.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return target.hashCode();
    }
}
