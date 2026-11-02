package com.turtletracerlib.auto;

import com.qualcomm.robotcore.hardware.Gamepad;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * A driver-station menu for choosing an auto, an alliance and whether to mirror, during init.
 * <p>
 * Call {@link #update(Gamepad, Telemetry)} every init loop, then {@link #buildSelected()} once the
 * OpMode starts. Controls (gamepad1 by default): D-pad up and down scroll the autos, D-pad left and
 * right (or the bumpers) change the alliance, X cycles the mirror override (AUTO, ON, OFF), and A locks
 * the choice. The alliance and mirroring are separate: by default mirroring follows the alliance, but
 * {@link AutoConfig} and {@link AutoEntry#mirror(MirrorPolicy)} can make it independent.
 * </p>
 *
 * @param <T> The type of auto.
 */
public final class AutoSelector<T> {
  private static final long REPEAT_DELAY_MS = 400;
  private static final long REPEAT_INTERVAL_MS = 120;

  private final AutoRegistry<T> registry;
  private final AutoConfig config;
  private final LongSupplier clockMs;

  private int selected = 0;
  private int windowStart = 0;
  private Alliance alliance;
  private MirrorOverride mirrorOverride = MirrorOverride.AUTO;
  private boolean locked = false;

  private boolean lastUp;
  private boolean lastDown;
  private boolean lastLeft;
  private boolean lastRight;
  private boolean lastMirror;
  private boolean lastLock;
  private long nextRepeatMs;

  /**
   * @param registry The autos to choose from.
   * @param config   The settings.
   */
  public AutoSelector(AutoRegistry<T> registry, AutoConfig config) {
    this(registry, config, System::currentTimeMillis);
  }

  /**
   * @param registry The autos to choose from.
   * @param config   The settings.
   * @param clockMs  A millisecond clock, for tests.
   */
  AutoSelector(AutoRegistry<T> registry, AutoConfig config, LongSupplier clockMs) {
    this.registry = registry;
    this.config = config == null ? AutoConfig.defaults() : config;
    this.clockMs = clockMs;
    this.alliance = this.config.getDefaultAlliance();

    if (AutoSelection.hasSelection()) {
      alliance = AutoSelection.alliance();
      mirrorOverride = AutoSelection.mirrorOverride();
      int index = AutoSelection.autoName() == null ? -1 : registry.indexOf(AutoSelection.autoName());
      if (index >= 0) {
        selected = index;
      }
    }
    scrollIntoView();
    store();
  }

  /**
   * Reads the gamepad, draws the menu into telemetry and (unless disabled in {@link AutoConfig})
   * calls {@code telemetry.update()}. Call every loop of the init phase.
   *
   * @param gamepad   The gamepad to read.
   * @param telemetry The telemetry to draw into.
   */
  public void update(Gamepad gamepad, Telemetry telemetry) {
    update(new GamepadAutoInput(gamepad));
    for (String line : menuLines()) {
      telemetry.addLine(line);
    }
    if (config.updatesTelemetry()) {
      telemetry.update();
    }
  }

  /**
   * Reads the buttons and updates the state without drawing anything. Use with {@link #menuLines()}
   * for custom telemetry, or with a gamepad wrapper that implements {@link AutoInput}.
   *
   * @param input The buttons.
   */
  public void update(AutoInput input) {
    boolean up = input.up();
    boolean down = input.down();
    boolean left = input.left();
    boolean right = input.right();
    boolean mirror = input.mirrorToggle();
    boolean lock = input.lock();
    long now = clockMs.getAsLong();

    if (lock && !lastLock) {
      locked = !locked;
      store();
    }

    if (!locked) {
      handleScroll(up, down, now);
      if (config.isAllianceSelectionAllowed()) {
        if (left && !lastLeft) {
          setAlliance(alliance.previous());
        }
        if (right && !lastRight) {
          setAlliance(alliance.next());
        }
      }
      if (mirror && !lastMirror && canToggleMirror()) {
        mirrorOverride = mirrorOverride.next();
        store();
      }
    }

    lastUp = up;
    lastDown = down;
    lastLeft = left;
    lastRight = right;
    lastMirror = mirror;
    lastLock = lock;
  }

  private void handleScroll(boolean up, boolean down, long now) {
    int size = registry.size();
    if (size == 0) {
      return;
    }
    int step = 0;
    boolean pressed = (up && !lastUp) || (down && !lastDown);
    if (pressed) {
      step = up && !lastUp ? -1 : 1;
      nextRepeatMs = now + REPEAT_DELAY_MS;
    } else if ((up != down) && now >= nextRepeatMs) {
      step = up ? -1 : 1;
      nextRepeatMs = now + REPEAT_INTERVAL_MS;
    }
    if (step != 0) {
      selected = ((selected + step) % size + size) % size;
      scrollIntoView();
      store();
    }
  }

  private void scrollIntoView() {
    int rows = visibleRows();
    if (selected < windowStart) {
      windowStart = selected;
    } else if (selected >= windowStart + rows) {
      windowStart = selected - rows + 1;
    }
    windowStart = Math.max(0, Math.min(windowStart, Math.max(0, registry.size() - rows)));
  }

  private int visibleRows() {
    return Math.max(1, Math.min(config.getMaxVisibleRows(), registry.size()));
  }

  private boolean canToggleMirror() {
    AutoEntry<T> entry = getSelected();
    return config.isMirrorToggleAllowed() && (entry == null || entry.getMirrorPolicy() != MirrorPolicy.NEVER);
  }

  private void store() {
    AutoEntry<T> entry = getSelected();
    AutoSelection.store(alliance, isMirrored(), mirrorOverride, entry == null ? null : entry.getName());
  }

  /**
   * Decides whether an auto is mirrored.
   * <ol>
   *   <li>An entry policy of {@link MirrorPolicy#NEVER} wins over everything.</li>
   *   <li>Otherwise the driver's ON or OFF override, if set.</li>
   *   <li>Otherwise the entry's policy, or the config's if the entry has none.
   *   {@link MirrorPolicy#FOLLOW_ALLIANCE} mirrors when an alliance is selected and differs from the
   *   alliance the auto is drawn for: the entry's {@link AutoEntry#drawnFor}, or the config's base
   *   alliance.</li>
   * </ol>
   *
   * @param config   The settings.
   * @param entry    The selected auto, or null.
   * @param alliance The selected alliance.
   * @param override The driver's mirror override.
   * @return Whether to mirror.
   */
  public static boolean resolveMirrored(
      AutoConfig config, AutoEntry<?> entry, Alliance alliance, MirrorOverride override) {
    return contextFor(config, entry, alliance, override).isMirrored();
  }

  private static AutoContext contextFor(
      AutoConfig config, AutoEntry<?> entry, Alliance alliance, MirrorOverride override) {
    MirrorPolicy entryPolicy = entry == null ? null : entry.getMirrorPolicy();
    // NEVER on the entry can't be overridden, and the override only counts when the toggle is on.
    MirrorOverride effectiveOverride =
        entryPolicy == MirrorPolicy.NEVER || !config.isMirrorToggleAllowed()
            ? MirrorOverride.AUTO
            : override;
    MirrorPolicy policy = entryPolicy != null ? entryPolicy : config.getMirrorPolicy();
    Alliance drawnFor =
        entry != null && entry.getDrawnFor() != null ? entry.getDrawnFor() : config.getBaseAlliance();
    return new AutoContext(
        alliance, drawnFor, effectiveOverride, policy, config.getMirrorAxes(), config.getFieldSize());
  }

  /** @return The selected auto, or null if none are registered. */
  public AutoEntry<T> getSelected() {
    return registry.size() == 0 ? null : registry.entries().get(selected);
  }

  /** @return The index of the selected auto. */
  public int getSelectedIndex() {
    return selected;
  }

  /** @return The selected alliance. */
  public Alliance getAlliance() {
    return alliance;
  }

  /**
   * @param alliance The alliance to select.
   */
  public void setAlliance(Alliance alliance) {
    this.alliance = alliance == null ? Alliance.NONE : alliance;
    store();
  }

  /** @return The driver's mirror override. */
  public MirrorOverride getMirrorOverride() {
    return mirrorOverride;
  }

  /**
   * @param override The mirror override.
   */
  public void setMirrorOverride(MirrorOverride override) {
    this.mirrorOverride = override == null ? MirrorOverride.AUTO : override;
    store();
  }

  /** @return Whether the selected auto will be built mirrored. */
  public boolean isMirrored() {
    return resolveMirrored(config, getSelected(), alliance, mirrorOverride);
  }

  /** @return Whether the choice is locked. */
  public boolean isLocked() {
    return locked;
  }

  /** @return The alliance and mirror choices for the selected auto, as an {@link AutoContext}. */
  public AutoContext createContext() {
    return contextFor(config, getSelected(), alliance, mirrorOverride);
  }

  /**
   * Builds the selected auto with the current alliance and mirror choices.
   *
   * @return The auto.
   * @throws IllegalStateException If no autos are registered.
   */
  public T buildSelected() {
    AutoEntry<T> entry = getSelected();
    if (entry == null) {
      throw new IllegalStateException("No autos are registered");
    }
    store();
    return entry.create(createContext());
  }

  /** @return The menu as lines of text, as {@link #update(Gamepad, Telemetry)} draws them. */
  public List<String> menuLines() {
    List<String> lines = new ArrayList<>();
    lines.add("== Auto Select ==" + (locked ? "  [LOCKED]" : ""));

    if (alliance == Alliance.NONE) {
      lines.add("Alliance: NONE (not set)"
          + (config.isAllianceSelectionAllowed() ? "  <D-pad L/R>" : ""));
    } else {
      lines.add("Alliance: " + alliance + (config.isAllianceSelectionAllowed() ? "  <D-pad L/R>" : ""));
    }

    AutoEntry<T> entry = getSelected();
    String mirrorState = isMirrored() ? "ON" : "OFF";
    if (entry != null && entry.getMirrorPolicy() == MirrorPolicy.NEVER) {
      lines.add("Mirror: " + mirrorState + " (set by auto)");
    } else if (!config.isMirrorToggleAllowed()) {
      lines.add("Mirror: " + mirrorState);
    } else {
      lines.add("Mirror: " + mirrorState + (mirrorOverride == MirrorOverride.AUTO ? " (auto)" : " (manual)")
          + "  <X>");
    }

    int size = registry.size();
    List<String> skipped = registry.skipped();
    String skippedLine = skipped.isEmpty()
        ? null
        : "! " + skipped.get(0) + (skipped.size() > 1 ? " (+" + (skipped.size() - 1) + " more)" : "");
    if (size == 0) {
      lines.add("No autos registered");
      if (skippedLine != null) {
        lines.add(skippedLine);
      }
      return lines;
    }

    int rows = visibleRows();
    lines.add(windowStart > 0 ? "  ^ " + windowStart + " more" : "");
    for (int i = windowStart; i < windowStart + rows; i++) {
      lines.add((i == selected ? " > " : "   ") + registry.entries().get(i).getDisplayName());
    }
    int below = size - (windowStart + rows);
    lines.add(below > 0 ? "  v " + below + " more" : "");
    lines.add((selected + 1) + "/" + size + "  <D-pad U/D>  lock: <A>");
    if (skippedLine != null) {
      lines.add(skippedLine);
    }
    return lines;
  }
}
