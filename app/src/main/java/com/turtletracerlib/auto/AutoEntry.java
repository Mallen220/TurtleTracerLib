package com.turtletracerlib.auto;

/**
 * An auto in the selector: a name and a factory.
 *
 * @param <T> The type of auto.
 */
public final class AutoEntry<T> {
  private final String name;
  private final AutoFactory<T> factory;
  private MirrorPolicy mirrorPolicy;
  private Alliance drawnFor;
  private String group = "";

  /**
   * @param name    The name shown in the menu.
   * @param factory Builds the auto.
   */
  public AutoEntry(String name, AutoFactory<T> factory) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Auto name cannot be empty");
    }
    if (factory == null) {
      throw new IllegalArgumentException("Auto factory cannot be null");
    }
    this.name = name;
    this.factory = factory;
  }

  /**
   * Sets a mirror policy for this auto, replacing the config's. {@link MirrorPolicy#NEVER} also stops
   * the driver from mirroring it, which suits autos drawn separately for each alliance.
   *
   * @param policy The policy.
   * @return This entry.
   */
  public AutoEntry<T> mirror(MirrorPolicy policy) {
    this.mirrorPolicy = policy;
    return this;
  }

  /**
   * Sets the alliance this auto's paths are drawn for, replacing the config's base alliance. Each
   * auto can be drawn for a different side.
   *
   * @param alliance The alliance; {@link Alliance#NONE} or null to use the config's.
   * @return This entry.
   */
  public AutoEntry<T> drawnFor(Alliance alliance) {
    this.drawnFor = alliance == Alliance.NONE ? null : alliance;
    return this;
  }

  /**
   * @param group A group name shown in front of the name in the menu.
   * @return This entry.
   */
  public AutoEntry<T> group(String group) {
    this.group = group == null ? "" : group;
    return this;
  }

  /** @return The name. */
  public String getName() {
    return name;
  }

  /** @return The group, or an empty string. */
  public String getGroup() {
    return group;
  }

  /** @return The name as shown in the menu, with the group if there is one. */
  public String getDisplayName() {
    return group.isEmpty() ? name : group + ": " + name;
  }

  /** @return This entry's own mirror policy, or null to use the config's. */
  public MirrorPolicy getMirrorPolicy() {
    return mirrorPolicy;
  }

  /** @return The alliance this auto's paths are drawn for, or null to use the config's. */
  public Alliance getDrawnFor() {
    return drawnFor;
  }

  /**
   * @param context The driver's choices.
   * @return The built auto.
   */
  public T create(AutoContext context) {
    return factory.create(context);
  }
}
