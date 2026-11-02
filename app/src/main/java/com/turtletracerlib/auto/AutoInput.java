package com.turtletracerlib.auto;

/**
 * The buttons the selector menu reads. Implement it over any gamepad wrapper, or use
 * {@link GamepadAutoInput} for the FTC SDK's {@code Gamepad}.
 */
public interface AutoInput {
  /** @return Whether the scroll-up button is held. */
  boolean up();

  /** @return Whether the scroll-down button is held. */
  boolean down();

  /** @return Whether the previous-alliance button is held. */
  boolean left();

  /** @return Whether the next-alliance button is held. */
  boolean right();

  /** @return Whether the mirror-toggle button is held. */
  boolean mirrorToggle();

  /** @return Whether the lock button is held. */
  boolean lock();
}
