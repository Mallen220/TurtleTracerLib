package com.turtletracerlib.auto;

/** The driver's mirror choice in the selector menu. */
public enum MirrorOverride {
  /** Use the {@link MirrorPolicy}. */
  AUTO,
  /** Force mirroring on. */
  ON,
  /** Force mirroring off. */
  OFF;

  /** @return The next value in the order AUTO, ON, OFF (wrapping). */
  public MirrorOverride next() {
    MirrorOverride[] all = values();
    return all[(ordinal() + 1) % all.length];
  }
}
