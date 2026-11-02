package com.turtletracerlib.auto;

/**
 * Settings for an {@link AutoSelector}. Build one with {@link #builder()}; every setting has a
 * default, so {@code AutoConfig.builder().build()} is valid.
 */
public final class AutoConfig {
  private final Alliance baseAlliance;
  private final Alliance defaultAlliance;
  private final MirrorAxes mirrorAxes;
  private final MirrorPolicy mirrorPolicy;
  private final boolean allowAllianceSelection;
  private final boolean allowMirrorToggle;
  private final int maxVisibleRows;
  private final double fieldSize;
  private final boolean updateTelemetry;

  private AutoConfig(Builder b) {
    this.baseAlliance = b.baseAlliance;
    this.defaultAlliance = b.defaultAlliance;
    this.mirrorAxes = b.mirrorAxes;
    this.mirrorPolicy = b.mirrorPolicy;
    this.allowAllianceSelection = b.allowAllianceSelection;
    this.allowMirrorToggle = b.allowMirrorToggle;
    this.maxVisibleRows = b.maxVisibleRows;
    this.fieldSize = b.fieldSize;
    this.updateTelemetry = b.updateTelemetry;
  }

  /** @return A config with every default. */
  public static AutoConfig defaults() {
    return builder().build();
  }

  /** @return A new builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** @return The alliance the paths were drawn for (default RED). */
  public Alliance getBaseAlliance() {
    return baseAlliance;
  }

  /** @return The alliance selected before the driver chooses one (default NONE). */
  public Alliance getDefaultAlliance() {
    return defaultAlliance;
  }

  /** @return The axes flipped when mirroring (default BOTH). */
  public MirrorAxes getMirrorAxes() {
    return mirrorAxes;
  }

  /** @return When autos are mirrored, unless an entry overrides it (default FOLLOW_ALLIANCE). */
  public MirrorPolicy getMirrorPolicy() {
    return mirrorPolicy;
  }

  /** @return Whether the driver can change the alliance in the menu. */
  public boolean isAllianceSelectionAllowed() {
    return allowAllianceSelection;
  }

  /** @return Whether the driver can override mirroring in the menu. */
  public boolean isMirrorToggleAllowed() {
    return allowMirrorToggle;
  }

  /** @return How many autos the menu shows at once before it scrolls. */
  public int getMaxVisibleRows() {
    return maxVisibleRows;
  }

  /** @return The field size in inches used for mirroring. */
  public double getFieldSize() {
    return fieldSize;
  }

  /** @return Whether {@link AutoSelector#update(com.qualcomm.robotcore.hardware.Gamepad, org.firstinspires.ftc.robotcore.external.Telemetry)} calls {@code telemetry.update()}. */
  public boolean updatesTelemetry() {
    return updateTelemetry;
  }

  /** Builder for {@link AutoConfig}. */
  public static final class Builder {
    private Alliance baseAlliance = Alliance.RED;
    private Alliance defaultAlliance = Alliance.NONE;
    private MirrorAxes mirrorAxes = MirrorAxes.BOTH;
    private MirrorPolicy mirrorPolicy = MirrorPolicy.FOLLOW_ALLIANCE;
    private boolean allowAllianceSelection = true;
    private boolean allowMirrorToggle = true;
    private int maxVisibleRows = 6;
    private double fieldSize = FieldMirror.DEFAULT_FIELD_SIZE;
    private boolean updateTelemetry = true;

    private Builder() {}

    /**
     * Sets the alliance the paths were drawn for. {@link Alliance#NONE} is treated as RED.
     *
     * @param baseAlliance The alliance whose side the paths are on.
     * @return This builder.
     */
    public Builder baseAlliance(Alliance baseAlliance) {
      this.baseAlliance = baseAlliance == null || baseAlliance == Alliance.NONE ? Alliance.RED : baseAlliance;
      return this;
    }

    /**
     * @param defaultAlliance The alliance selected until the driver changes it.
     * @return This builder.
     */
    public Builder defaultAlliance(Alliance defaultAlliance) {
      this.defaultAlliance = defaultAlliance == null ? Alliance.NONE : defaultAlliance;
      return this;
    }

    /**
     * @param mirrorAxes The axes flipped when mirroring.
     * @return This builder.
     */
    public Builder mirrorAxes(MirrorAxes mirrorAxes) {
      this.mirrorAxes = mirrorAxes == null ? MirrorAxes.BOTH : mirrorAxes;
      return this;
    }

    /**
     * @param mirrorPolicy When autos are mirrored.
     * @return This builder.
     */
    public Builder mirrorPolicy(MirrorPolicy mirrorPolicy) {
      this.mirrorPolicy = mirrorPolicy == null ? MirrorPolicy.FOLLOW_ALLIANCE : mirrorPolicy;
      return this;
    }

    /**
     * @param allow Whether the driver can change the alliance in the menu.
     * @return This builder.
     */
    public Builder allowAllianceSelection(boolean allow) {
      this.allowAllianceSelection = allow;
      return this;
    }

    /**
     * @param allow Whether the driver can override mirroring in the menu.
     * @return This builder.
     */
    public Builder allowMirrorToggle(boolean allow) {
      this.allowMirrorToggle = allow;
      return this;
    }

    /**
     * @param rows Autos shown at once (at least 1).
     * @return This builder.
     */
    public Builder maxVisibleRows(int rows) {
      this.maxVisibleRows = Math.max(1, rows);
      return this;
    }

    /**
     * @param fieldSize The field size in inches.
     * @return This builder.
     */
    public Builder fieldSize(double fieldSize) {
      this.fieldSize = fieldSize;
      return this;
    }

    /**
     * @param update Whether the selector calls {@code telemetry.update()} itself (default true).
     * @return This builder.
     */
    public Builder updateTelemetry(boolean update) {
      this.updateTelemetry = update;
      return this;
    }

    /** @return The config. */
    public AutoConfig build() {
      return new AutoConfig(this);
    }
  }
}
