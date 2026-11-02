package com.turtletracerlib.auto;

/**
 * Builds an auto once the driver has made their choices. A lambda in Java or Kotlin.
 *
 * @param <T> The type of auto, typically the command framework's {@code Command}.
 */
@FunctionalInterface
public interface AutoFactory<T> {
  /**
   * @param context The driver's alliance and mirror choices.
   * @return The auto, ready to schedule.
   */
  T create(AutoContext context);
}
