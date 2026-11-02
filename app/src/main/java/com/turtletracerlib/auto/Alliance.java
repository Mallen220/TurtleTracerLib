package com.turtletracerlib.auto;

/**
 * The alliance a team is on. {@link #NONE} means the alliance has not been chosen; autos then run
 * as if the team were on the {@link AutoConfig#getBaseAlliance() base alliance}.
 */
public enum Alliance {
  NONE,
  RED,
  BLUE;

  /**
   * @return The other alliance, or {@link #NONE} for {@link #NONE}.
   */
  public Alliance opposite() {
    switch (this) {
      case RED:
        return BLUE;
      case BLUE:
        return RED;
      default:
        return NONE;
    }
  }

  /**
   * @return The next alliance in the order NONE, RED, BLUE (wrapping), used by the selector menu.
   */
  public Alliance next() {
    Alliance[] all = values();
    return all[(ordinal() + 1) % all.length];
  }

  /**
   * @return The previous alliance in the order NONE, RED, BLUE (wrapping).
   */
  public Alliance previous() {
    Alliance[] all = values();
    return all[(ordinal() + all.length - 1) % all.length];
  }
}
