package com.turtletracerlib;

/**
 * A collection of general-purpose utility methods for mathematics and unit conversion.
 * <p>
 * This class provides static helper functions commonly used in robot control logic,
 * such as value clamping, tolerance checking, and unit conversions.
 * </p>
 */
public class Utils {

  /**
   * Clamps a value within a specified range [min, max].
   * <p>
   * If the value is less than the minimum, the minimum is returned.
   * If the value is greater than the maximum, the maximum is returned.
   * Otherwise, the original value is returned.
   * </p>
   *
   * @param val The value to clamp.
   * @param min The minimum allowed value.
   * @param max The maximum allowed value.
   * @return The clamped value.
   */
  public static double clamp(final double val, final double min, final double max) {
    return Math.max(min, Math.min(max, val));
  }

  /**
   * Checks if a value is within a specified tolerance of a target value.
   * <p>
   * This is useful for checking if a sensor reading or control loop error is "close enough"
   * to the desired state.
   * </p>
   *
   * @param targetValue  The desired target value.
   * @param currentValue The actual current value.
   * @param tolerance    The maximum allowed difference (absolute value).
   * @return {@code true} if {@code |targetValue - currentValue| <= tolerance}, {@code false} otherwise.
   */
  public static boolean isWithinTolerance(
      final double targetValue, final double currentValue, final double tolerance) {
    return Math.abs(targetValue - currentValue) <= tolerance;
  }

  /**
   * Converts encoder ticks to inches traveled for a wheel.
   * <p>
   * This calculation is based on the wheel diameter, gear ratio, and the encoder resolution
   * (specifically for a GoBILDA 5202/5203 Series Yellow Jacket Motor with a 13.7:1 gear ratio).
   * </p>
   *
   * @param ticks         The number of encoder ticks.
   * @param wheelDiameter The diameter of the wheel in inches.
   * @param gearRatio     The external gear ratio (if any). If the motor is directly driving the wheel, use 1.0.
   * @return The distance traveled in inches.
   */
  public static double ticksToInches(
      final double ticks, final double wheelDiameter, final double gearRatio) {
    final double TICKS_PER_REV = 384.5; // specific to GoBILDA 13.7:1 Yellow Jacket
    return (ticks / (gearRatio * TICKS_PER_REV)) * (Math.PI * wheelDiameter);
  }

  /**
   * Converts rotational speed from Revolutions Per Minute (RPM) to Radians Per Second (rad/s).
   * <p>
   * This is useful for physics calculations or control systems that use SI units.
   * </p>
   *
   * @param rpm The rotational speed in RPM.
   * @return The rotational speed in rad/s.
   */
  private static double rpmToRadPerSec(double rpm) {
    return rpm * (2 * Math.PI / 60);
  }
}
