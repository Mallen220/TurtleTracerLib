package com.turtletracerlib.auto;

/**
 * The last choices made in an {@link AutoSelector}, kept for as long as the robot controller app
 * runs. Lets a TeleOp read the alliance chosen in Auto, and lets the menu come back showing the
 * previous choice when an OpMode is re-initialised.
 */
public final class AutoSelection {
  private static volatile Alliance alliance = Alliance.NONE;
  private static volatile boolean mirrored = false;
  private static volatile MirrorOverride mirrorOverride = MirrorOverride.AUTO;
  private static volatile String autoName = null;
  private static volatile boolean hasSelection = false;

  private AutoSelection() {}

  /** @return The last selected alliance, {@link Alliance#NONE} if none was chosen. */
  public static Alliance alliance() {
    return alliance;
  }

  /** @return Whether the last selected auto was built mirrored. */
  public static boolean isMirrored() {
    return mirrored;
  }

  /** @return The last driver mirror override. */
  public static MirrorOverride mirrorOverride() {
    return mirrorOverride;
  }

  /** @return The name of the last selected auto, or null. */
  public static String autoName() {
    return autoName;
  }

  /** @return Whether a selector has stored a selection yet. */
  public static boolean hasSelection() {
    return hasSelection;
  }

  /**
   * Records the alliance an auto is running for and whether it is mirrored, for TeleOp to read with
   * {@link #alliance()} and {@link #isMirrored()}. An auto drawn for one alliance that never mirrors
   * calls this itself, since no selector decided it. Leaves the rest of the selection as it is.
   *
   * @param a        The alliance; null for {@link Alliance#NONE}.
   * @param mirrored Whether the auto is mirrored.
   */
  public static void record(Alliance a, boolean mirrored) {
    alliance = a == null ? Alliance.NONE : a;
    AutoSelection.mirrored = mirrored;
  }

  /**
   * Records what an auto decided from its context: the alliance it runs for (the one picked, or the
   * one it's drawn for if none was) and whether it is mirrored. Code exported from Turtle Tracer
   * calls this once it has told the context how it mirrors.
   *
   * @param context The auto's context.
   */
  public static void record(AutoContext context) {
    record(context.effectiveAlliance(), context.isMirrored());
  }

  static void store(Alliance a, boolean m, MirrorOverride o, String name) {
    alliance = a;
    mirrored = m;
    mirrorOverride = o;
    autoName = name;
    hasSelection = true;
  }

  /** Forgets the stored selection. */
  public static void reset() {
    alliance = Alliance.NONE;
    mirrored = false;
    mirrorOverride = MirrorOverride.AUTO;
    autoName = null;
    hasSelection = false;
  }
}
