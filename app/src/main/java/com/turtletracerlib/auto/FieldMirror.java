package com.turtletracerlib.auto;

import com.pedropathing.math.Pose;

/**
 * Mirrors field coordinates and headings so one path can run on either side of the field.
 * <p>
 * Coordinates are Pedro field coordinates (origin in a corner, 144 inch square by default,
 * heading 0 along +x, radians counter-clockwise). Mirrored headings are normalised to
 * {@code [0, 2*PI)}. An instance that mirrors nothing ({@link #none()}) returns every value
 * unchanged, so code can call it unconditionally.
 * </p>
 */
public final class FieldMirror {
  /** Default field size in inches. */
  public static final double DEFAULT_FIELD_SIZE = 144.0;

  private static final FieldMirror NONE = new FieldMirror(MirrorAxes.NONE, DEFAULT_FIELD_SIZE);

  private final MirrorAxes axes;
  private final double fieldSize;

  private FieldMirror(MirrorAxes axes, double fieldSize) {
    this.axes = axes;
    this.fieldSize = fieldSize;
  }

  /** @return A mirror that changes nothing. */
  public static FieldMirror none() {
    return NONE;
  }

  /**
   * @param axes The axes to flip.
   * @return A mirror for a {@value #DEFAULT_FIELD_SIZE} inch field.
   */
  public static FieldMirror of(MirrorAxes axes) {
    return of(axes, DEFAULT_FIELD_SIZE);
  }

  /**
   * @param axes      The axes to flip.
   * @param fieldSize The field size in inches.
   * @return A mirror for that field.
   */
  public static FieldMirror of(MirrorAxes axes, double fieldSize) {
    if (axes == null || axes == MirrorAxes.NONE) {
      return NONE;
    }
    return new FieldMirror(axes, fieldSize);
  }

  /** @return Whether this mirror leaves everything unchanged. */
  public boolean isIdentity() {
    return axes == MirrorAxes.NONE;
  }

  /** @return The axes this mirror flips. */
  public MirrorAxes getAxes() {
    return axes;
  }

  /**
   * @param x An x coordinate in inches.
   * @return The mirrored x coordinate.
   */
  public double x(double x) {
    return axes.flipsX() ? fieldSize - x : x;
  }

  /**
   * @param y A y coordinate in inches.
   * @return The mirrored y coordinate.
   */
  public double y(double y) {
    return axes.flipsY() ? fieldSize - y : y;
  }

  /**
   * @param heading A heading in radians.
   * @return The mirrored heading in radians, in {@code [0, 2*PI)} (unchanged if this is the identity).
   */
  public double heading(double heading) {
    if (isIdentity()) {
      return heading;
    }
    double h = heading;
    if (axes.flipsX()) {
      h = Math.PI - h;
    }
    if (axes.flipsY()) {
      h = -h;
    }
    double twoPi = 2 * Math.PI;
    h %= twoPi;
    if (h < 0) {
      h += twoPi;
    }
    return h;
  }

  /**
   * @param degrees A heading in degrees.
   * @return The mirrored heading in degrees (unchanged if this is the identity).
   */
  public double degrees(double degrees) {
    if (isIdentity()) {
      return degrees;
    }
    return Math.toDegrees(heading(Math.toRadians(degrees)));
  }

  /**
   * @param pose A pose.
   * @return The mirrored pose (the same instance if this is the identity).
   */
  public Pose pose(Pose pose) {
    if (pose == null || isIdentity()) {
      return pose;
    }
    return new Pose(x(pose.x()), y(pose.y()), heading(pose.heading()));
  }
}
