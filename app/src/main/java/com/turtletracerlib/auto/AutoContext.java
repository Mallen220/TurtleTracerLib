package com.turtletracerlib.auto;

import com.pedropathing.math.Pose;

/**
 * What an auto needs to know about the driver's choices when it is built: the alliance and how to
 * mirror. Passed to every {@link AutoFactory}.
 * <p>
 * Whether to mirror depends on the alliance each path was drawn for, which can differ from path to
 * path. A routine that knows its own side calls {@link #drawnFor(Alliance)} to get the context for it;
 * otherwise the side registered for the auto (or the {@link AutoConfig}'s base alliance) is used.
 * </p>
 */
public final class AutoContext {
  private final Alliance alliance;
  private final Alliance baseAlliance;
  private final MirrorOverride override;
  private final MirrorPolicy policy;
  private final MirrorAxes axes;
  private final double fieldSize;
  private final boolean mirrored;
  private final FieldMirror mirror;

  /**
   * @param alliance     The selected alliance.
   * @param baseAlliance The alliance the paths were drawn for. {@link Alliance#NONE} is treated as RED.
   * @param override     The driver's mirror override; AUTO to leave it to the policy.
   * @param policy       When to mirror, if the driver hasn't forced it.
   * @param axes         The axes a mirror flips.
   * @param fieldSize    The field size in inches.
   */
  public AutoContext(
      Alliance alliance,
      Alliance baseAlliance,
      MirrorOverride override,
      MirrorPolicy policy,
      MirrorAxes axes,
      double fieldSize) {
    this.alliance = alliance == null ? Alliance.NONE : alliance;
    this.baseAlliance =
        baseAlliance == null || baseAlliance == Alliance.NONE ? Alliance.RED : baseAlliance;
    this.override = override == null ? MirrorOverride.AUTO : override;
    this.policy = policy == null ? MirrorPolicy.FOLLOW_ALLIANCE : policy;
    this.axes = axes == null ? MirrorAxes.BOTH : axes;
    this.fieldSize = fieldSize;
    this.mirrored = decide(this.alliance, this.baseAlliance, this.override, this.policy);
    this.mirror = mirrored ? FieldMirror.of(this.axes, fieldSize) : FieldMirror.none();
  }

  /**
   * A context for paths drawn for {@code baseAlliance}, mirrored when the selected alliance is the
   * other one.
   *
   * @param alliance     The selected alliance.
   * @param baseAlliance The alliance the paths were drawn for.
   * @return The context, flipping both axes of a 144 inch field when mirrored.
   */
  public static AutoContext forAlliance(Alliance alliance, Alliance baseAlliance) {
    return new AutoContext(
        alliance,
        baseAlliance,
        MirrorOverride.AUTO,
        MirrorPolicy.FOLLOW_ALLIANCE,
        MirrorAxes.BOTH,
        FieldMirror.DEFAULT_FIELD_SIZE);
  }

  /** @return A context with no alliance chosen and nothing mirrored. */
  public static AutoContext unmirrored() {
    return forAlliance(Alliance.NONE, Alliance.RED);
  }

  /**
   * Whether to mirror: the driver's ON or OFF override wins; otherwise the policy decides, and
   * {@link MirrorPolicy#FOLLOW_ALLIANCE} mirrors when an alliance is selected and isn't the one the
   * paths were drawn for.
   */
  static boolean decide(
      Alliance alliance, Alliance baseAlliance, MirrorOverride override, MirrorPolicy policy) {
    if (override == MirrorOverride.ON) {
      return true;
    }
    if (override == MirrorOverride.OFF) {
      return false;
    }
    switch (policy) {
      case ALWAYS:
        return true;
      case NEVER:
        return false;
      default:
        return alliance != Alliance.NONE && alliance != baseAlliance;
    }
  }

  /**
   * The same choices for paths drawn for {@code baseAlliance}. Generated code calls this with the
   * alliance its project is drawn for, so each path decides for itself whether it is mirrored.
   *
   * @param baseAlliance The alliance the paths were drawn for; {@link Alliance#NONE} changes nothing.
   * @return The context for those paths.
   */
  public AutoContext drawnFor(Alliance baseAlliance) {
    if (baseAlliance == null || baseAlliance == Alliance.NONE || baseAlliance == this.baseAlliance) {
      return this;
    }
    return new AutoContext(alliance, baseAlliance, override, policy, axes, fieldSize);
  }

  /**
   * The same choices with this auto's own mirror policy, replacing the one it was registered with.
   * Generated code calls this with the policy its project asks for, so it mirrors (or doesn't)
   * whatever the selector's config says. The driver's ON or OFF override still applies.
   *
   * @param policy When to mirror; null changes nothing.
   * @return The context with that policy.
   */
  public AutoContext mirroring(MirrorPolicy policy) {
    if (policy == null || policy == this.policy) {
      return this;
    }
    return new AutoContext(alliance, baseAlliance, override, policy, axes, fieldSize);
  }

  /** @return The policy that decided whether to mirror. */
  public MirrorPolicy mirrorPolicy() {
    return policy;
  }

  /** @return The selected alliance, possibly {@link Alliance#NONE}. */
  public Alliance alliance() {
    return alliance;
  }

  /** @return The alliance the paths are drawn for. */
  public Alliance baseAlliance() {
    return baseAlliance;
  }

  /** @return The selected alliance, or the base alliance if none was selected. */
  public Alliance effectiveAlliance() {
    return alliance == Alliance.NONE ? baseAlliance : alliance;
  }

  /** @return Whether the auto should be mirrored. */
  public boolean isMirrored() {
    return mirrored;
  }

  /** @return The mirror to apply; the identity when {@link #isMirrored()} is false. */
  public FieldMirror mirror() {
    return mirror;
  }

  /**
   * @param pose A pose drawn for the base alliance.
   * @return The pose to use for the selected alliance.
   */
  public Pose pose(Pose pose) {
    return mirror.pose(pose);
  }

  /**
   * @param heading A heading in radians drawn for the base alliance.
   * @return The heading to use for the selected alliance.
   */
  public double heading(double heading) {
    return mirror.heading(heading);
  }

  /**
   * @param x An x coordinate in inches drawn for the base alliance.
   * @return The x coordinate to use for the selected alliance.
   */
  public double x(double x) {
    return mirror.x(x);
  }

  /**
   * @param y A y coordinate in inches drawn for the base alliance.
   * @return The y coordinate to use for the selected alliance.
   */
  public double y(double y) {
    return mirror.y(y);
  }

  /**
   * Picks between two hand-made versions of something, such as a red and a blue {@code .turt} file.
   *
   * @param red  The value for the red alliance.
   * @param blue The value for the blue alliance.
   * @param <V>  The value type.
   * @return {@code blue} if the effective alliance is blue, otherwise {@code red}.
   */
  public <V> V pick(V red, V blue) {
    return effectiveAlliance() == Alliance.BLUE ? blue : red;
  }
}
