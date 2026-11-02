package com.turtletracerlib.auto;

/**
 * Which field axes are flipped when an auto is mirrored.
 * <ul>
 *   <li>{@link #X} flips left and right ({@code x' = size - x}, {@code heading' = PI - heading}).</li>
 *   <li>{@link #Y} flips up and down ({@code y' = size - y}, {@code heading' = -heading}).</li>
 *   <li>{@link #BOTH} flips both, a 180 degree rotation about the field centre
 *   ({@code heading' = heading + PI}).</li>
 * </ul>
 */
public enum MirrorAxes {
  NONE(false, false),
  X(true, false),
  Y(false, true),
  BOTH(true, true);

  private final boolean flipX;
  private final boolean flipY;

  MirrorAxes(boolean flipX, boolean flipY) {
    this.flipX = flipX;
    this.flipY = flipY;
  }

  /** @return Whether x coordinates are flipped. */
  public boolean flipsX() {
    return flipX;
  }

  /** @return Whether y coordinates are flipped. */
  public boolean flipsY() {
    return flipY;
  }
}
