package com.turtletracerlib.auto;

/**
 * Decides when an auto is mirrored. Independent of the alliance selection, so a team can pick an
 * alliance without anything being flipped (for example when it keeps separate red and blue files).
 */
public enum MirrorPolicy {
  /** Mirror when an alliance is selected and differs from the base alliance. The default. */
  FOLLOW_ALLIANCE,
  /** Always mirror. */
  ALWAYS,
  /** Never mirror. On an {@link AutoEntry} this also hides the driver's mirror toggle. */
  NEVER
}
