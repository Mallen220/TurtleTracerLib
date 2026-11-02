package com.turtletracerlib.auto;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Reads the selector menu's buttons from an FTC {@link Gamepad}: D-pad up and down scroll, D-pad
 * left and right (or the bumpers) change the alliance, X toggles mirroring and A locks the choice.
 */
public final class GamepadAutoInput implements AutoInput {
  private final Gamepad gamepad;

  /** @param gamepad The gamepad to read, usually {@code gamepad1}. */
  public GamepadAutoInput(Gamepad gamepad) {
    this.gamepad = gamepad;
  }

  @Override
  public boolean up() {
    return gamepad.dpad_up;
  }

  @Override
  public boolean down() {
    return gamepad.dpad_down;
  }

  @Override
  public boolean left() {
    return gamepad.dpad_left || gamepad.left_bumper;
  }

  @Override
  public boolean right() {
    return gamepad.dpad_right || gamepad.right_bumper;
  }

  @Override
  public boolean mirrorToggle() {
    return gamepad.x;
  }

  @Override
  public boolean lock() {
    return gamepad.a;
  }
}
