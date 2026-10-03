package com.turtletracerlib;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pedropathing.math.Pose;
import com.turtletracerlib.pathing.NamedCommands;
import com.turtletracerlib.pathing.ProgressTracker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for reading and parsing Turtle Tracer (.turt) files.
 * <p>
 * This class handles the deserialization of the JSON-based path files and
 * converts the coordinate system used in the visualizer to the robot's coordinate system (Pose).
 * It supports extracting start poses, waypoint poses, Bézier control points, shapes, sequence items,
 * and binding/registering all event marker types (parametric, temporal, pose/spatial) to a {@link ProgressTracker}.
 * </p>
 */
public final class TurtleTracerReader {

  /**
   * The parsed structure of the Turtle Tracer file (.turt).
   */
  private final TurtleTurt file;

  /**
   * A map storing the parsed poses, keyed by their name (with spaces removed).
   */
  private final Map<String, Pose> poses = new HashMap<>();


  /**
   * A map storing action bindings for event marker names.
   */
  private final Map<String, Runnable> boundActions = new HashMap<>();

  /**
   * Constructs a new {@code TurtleTracerReader} from an assets file.
   *
   * @param filename The name of the .turt file (relative to the "AutoPaths" folder in assets).
   * @param context  The Android context used to access the assets manager.
   * @throws IOException If the file cannot be found or an error occurs during reading.
   */
  public TurtleTracerReader(String filename, Context context) throws IOException {
    InputStream stream = context.getAssets().open("AutoPaths/" + filename);
    if (stream == null) {
      throw new FileNotFoundException("Turt File not found: " + filename);
    }
    Gson gson = new GsonBuilder().create();
    try (InputStreamReader reader = new InputStreamReader(stream)) {
      this.file = gson.fromJson(reader, TurtleTurt.class);
    }
    loadAllPoints();
  }

  /**
   * Constructs a new {@code TurtleTracerReader} from an {@link InputStream}.
   *
   * @param stream The input stream containing the .turt JSON data.
   * @throws IOException If an error occurs reading the stream.
   */
  public TurtleTracerReader(InputStream stream) throws IOException {
    if (stream == null) {
      throw new IllegalArgumentException("InputStream cannot be null");
    }
    Gson gson = new GsonBuilder().create();
    try (InputStreamReader reader = new InputStreamReader(stream)) {
      this.file = gson.fromJson(reader, TurtleTurt.class);
    }
    loadAllPoints();
  }

  /**
   * Constructs a new {@code TurtleTracerReader} from a {@link Reader}.
   *
   * @param reader The reader containing the .turt JSON data.
   */
  public TurtleTracerReader(Reader reader) {
    if (reader == null) {
      throw new IllegalArgumentException("Reader cannot be null");
    }
    Gson gson = new GsonBuilder().create();
    this.file = gson.fromJson(reader, TurtleTurt.class);
    loadAllPoints();
  }

  /**
   * Constructs a new {@code TurtleTracerReader} from a raw JSON string.
   *
   * @param jsonContent The JSON content string of the .turt file.
   */
  public TurtleTracerReader(String jsonContent) {
    this(new StringReader(jsonContent));
  }

  /**
   * Retrieves the version of the Turtle Tracer file format.
   *
   * @return Version string (e.g. "2.2.1").
   */
  public String getVersion() {
    return file != null ? file.version : "";
  }

  /**
   * Retrieves the file header metadata.
   *
   * @return The header metadata.
   */
  public TurtleTurt.Header getHeader() {
    return file != null ? file.header : null;
  }

  /**
   * Retrieves the start point definition.
   *
   * @return The start point.
   */
  public TurtleTurt.StartPoint getStartPoint() {
    return file != null ? file.startPoint : null;
  }

  /**
   * Retrieves the raw list of line segments parsed from the JSON.
   *
   * @return The list of line segments, or empty list if none.
   */
  public List<TurtleTurt.Line> getLines() {
    return file != null && file.lines != null ? file.lines : Collections.emptyList();
  }

  /**
   * Retrieves the sequence of autonomous steps (paths, waits, rotations) defined in the file.
   *
   * @return The sequence items list.
   */
  public List<TurtleTurt.SequenceItem> getSequence() {
    return file != null && file.sequence != null ? file.sequence : Collections.emptyList();
  }

  /**
   * Retrieves the field shapes/obstacles parsed from the JSON.
   *
   * @return The list of shapes.
   */
  public List<TurtleTurt.Shape> getShapes() {
    return file != null && file.shapes != null ? file.shapes : Collections.emptyList();
  }

  /**
   * Retrieves arbitrary extra metadata defined in the file.
   *
   * @return Map of extra data properties.
   */
  public Map<String, Object> getExtraData() {
    return file != null && file.extraData != null ? file.extraData : Collections.emptyMap();
  }

  /**
   * Retrieves all parsed poses by their name.
   *
   * @return An unmodifiable view of all poses.
   */
  public Map<String, Pose> getAllPoses() {
    return Collections.unmodifiableMap(poses);
  }

  /**
   * Retrieves a parsed {@link Pose} by its name.
   * <p>
   * Supported names include:
   * <ul>
   *   <li>{@code "startPoint"} - The starting pose.</li>
   *   <li>Segment names (e.g., {@code "DriveToShoot"}, {@code "dave"}).</li>
   *   <li>Bézier control points, named by their line (e.g., {@code "greg_line1_control1"}). The older
   *   {@code "greg_control1"} form still finds the first line named {@code greg}.</li>
   * </ul>
   * </p>
   *
   * @param name The name of the point or control point.
   * @return The corresponding {@link Pose}, or {@code null} if not found.
   */
  public Pose get(String name) {
    return poses.get(name);
  }

  /**
   * Binds an action to an event marker name.
   *
   * @param markerName The name of the event marker from the visualizer.
   * @param action     The action to execute when this marker triggers.
   * @return This reader (for chaining).
   */
  public TurtleTracerReader onEvent(String markerName, Runnable action) {
    boundActions.put(markerName, action);
    return this;
  }

  /**
   * Alias for {@link #onEvent(String, Runnable)}.
   *
   * @param markerName The name of the event marker.
   * @param action     The action to execute.
   * @return This reader (for chaining).
   */
  public TurtleTracerReader bindEvent(String markerName, Runnable action) {
    return onEvent(markerName, action);
  }

  /**
   * Registers all event markers parsed from the JSON file to the provided {@link ProgressTracker}.
   * <p>
   * Automatically parses the event marker's {@code type}:
   * <ul>
   *   <li><b>parametric:</b> Registers progress thresholds ($t$-values) via {@link ProgressTracker#onParametric}.</li>
   *   <li><b>temporal:</b> Registers timer delays (in milliseconds) via {@link ProgressTracker#onTemporal}.</li>
   *   <li><b>pose / spatial:</b> Converts $(x, y)$ coordinates to field {@link Pose} and registers proximity triggers via {@link ProgressTracker#onSpatial}.</li>
   * </ul>
   * If an action was bound via {@link #onEvent(String, Runnable)}, it is linked directly; otherwise, it resolves dynamically via {@link NamedCommands}.
   * </p>
   *
   * @param tracker The {@link ProgressTracker} to register the events to.
   */
  public void registerEvents(ProgressTracker tracker) {
    if (file == null || tracker == null) {
      return;
    }

    // Transfer all bound actions to the tracker
    for (Map.Entry<String, Runnable> entry : boundActions.entrySet()) {
      tracker.onEvent(entry.getKey(), entry.getValue());
    }

    // Register event markers on path lines
    if (file.lines != null) {
      for (TurtleTurt.Line line : file.lines) {
        if (line.eventMarkers != null) {
          for (TurtleTurt.EventMarker marker : line.eventMarkers) {
            registerSingleMarker(tracker, marker, -1, false);
          }
        }
      }
    }

    // Register event markers on sequence items (waits, rotations, etc.)
    if (file.sequence != null) {
      for (TurtleTurt.SequenceItem item : file.sequence) {
        if (item.eventMarkers != null) {
          for (TurtleTurt.EventMarker marker : item.eventMarkers) {
            registerSingleMarker(tracker, marker, -1, true);
          }
        }
      }
    }
  }

  /**
   * Registers the event markers of one path line with the provided {@link ProgressTracker}.
   * <p>
   * Use this instead of {@link #registerEvents(ProgressTracker)} when following several paths in a
   * row: {@code registerEvents} registers every line's markers at once, so they would all trigger
   * during whichever path the follower is on. Instead, call {@link ProgressTracker#clearPathEvents()}
   * before each path, register just that path's lines here, then call
   * {@link ProgressTracker#setCurrentPath}.
   * </p>
   * <p>
   * Parametric progress is per segment, so pass the position of the line within its chain as
   * {@code segmentIndex} (0 for a path that isn't chained).
   * </p>
   *
   * @param tracker      The {@link ProgressTracker} to register the events to.
   * @param lineIndex    The index of the line in the file, as listed by {@link #getLines()}.
   * @param segmentIndex The index of the line within the chain being followed.
   */
  public void registerLineEvents(ProgressTracker tracker, int lineIndex, int segmentIndex) {
    if (file == null || tracker == null || file.lines == null) {
      return;
    }
    if (lineIndex < 0 || lineIndex >= file.lines.size()) {
      return;
    }

    for (Map.Entry<String, Runnable> entry : boundActions.entrySet()) {
      tracker.onEvent(entry.getKey(), entry.getValue());
    }

    TurtleTurt.Line line = file.lines.get(lineIndex);
    if (line.eventMarkers != null) {
      for (TurtleTurt.EventMarker marker : line.eventMarkers) {
        registerSingleMarker(tracker, marker, segmentIndex, false);
      }
    }
  }

  /**
   * Registers a single event marker with the tracker according to its type.
   *
   * @param tracker      The progress tracker.
   * @param marker       The event marker definition.
   * @param segmentIndex The chain segment a parametric marker belongs to, or -1 for any segment.
   * @param registerZone Whether to also register the marker as a named event, so that
   *                     {@link ProgressTracker#executeEvent} can fire it. Waits and turns do that; a
   *                     path's marker must not, or {@link ProgressTracker#update()} would trigger the
   *                     action a second time through the named event.
   */
  private void registerSingleMarker(
      ProgressTracker tracker, TurtleTurt.EventMarker marker, int segmentIndex, boolean registerZone) {
    if (marker == null || marker.name == null) {
      return;
    }

    Runnable action = boundActions.getOrDefault(marker.name, () -> {
      if (NamedCommands.hasCommand(marker.name)) {
        NamedCommands.getCommand(marker.name).run();
      }
    });

    String type = marker.type != null ? marker.type.toLowerCase().trim() : "parametric";

    switch (type) {
      case "temporal":
        long timeMs = marker.time > 0 ? marker.time : marker.endTime;
        tracker.onTemporal(timeMs, action);
        break;

      case "pose":
      case "spatial":
        Pose targetPose = toPose(marker.poseX, marker.poseY, marker.poseHeading);
        tracker.onSpatial(targetPose, 2.0, action);
        break;

      case "parametric":
      default:
        double start = marker.position;
        double end = marker.position;
        boolean isRange = false;
        if (marker.points != null && marker.points.length > 0) {
          start = marker.points[0];
          isRange = marker.points.length > 1;
          end = isRange ? marker.points[1] : start;
        }
        if (isRange) {
          if (segmentIndex >= 0) {
            tracker.onParametric(segmentIndex, start, end, action);
          } else {
            tracker.onParametric(start, end, action);
          }
        } else if (segmentIndex >= 0) {
          tracker.onParametric(segmentIndex, start, action);
        } else {
          tracker.onParametric(start, action);
        }
        if (registerZone) {
          tracker.registerEvent(marker.name, start, end);
        }
        break;
    }
  }

  /**
   * Populates the {@code poses} map from the start point and lines, following the same rules the
   * code Turtle Tracer generates does when it embeds the numbers:
   * <ul>
   *   <li>Coordinates are Pedro field coordinates, used as they are.</li>
   *   <li>{@code startPoint} has the heading the file's start point records. Turtle Tracer saves it
   *   as a linear heading whose {@code startDeg} is the heading of the first path the robot drives.</li>
   *   <li>A line's pose is named after the line with everything but letters and digits removed
   *   ({@code point<n>} if that leaves nothing), and has the heading the line ends with when that is
   *   a fixed angle, or 0 when the robot follows the path.</li>
   *   <li>Lines with the same name share the first one's pose.</li>
   *   <li>Control points are named {@code <line>_line<index>_control<n>}.</li>
   * </ul>
   * Files from older versions and {@code .pp} files follow the same rules as far as they have the
   * fields. Their start heading can differ from the app's when only the app can resolve it, such as
   * when the first path in the sequence isn't the first line.
   */
  private void loadAllPoints() {
    if (file == null) {
      return;
    }

    if (file.startPoint != null) {
      TurtleTurt.StartPoint start = file.startPoint;
      double deg = 0;
      if ("constant".equals(start.heading)) {
        deg = start.degrees;
      } else if ("linear".equals(start.heading)) {
        deg = start.startDeg;
      }
      poses.put("startPoint", toPose(start.x, start.y, deg));
    }

    if (file.lines == null) {
      return;
    }

    for (int lineIdx = 0; lineIdx < file.lines.size(); lineIdx++) {
      TurtleTurt.Line line = file.lines.get(lineIdx);
      if (line == null || line.endPoint == null) {
        continue;
      }

      // Duplicate names are written as "Name (1)", "Name (2)" with the real one kept in _linkedName.
      String rawName = line._linkedName != null && !line._linkedName.isEmpty() ? line._linkedName : line.name;
      String name = rawName == null ? "" : rawName.replaceAll("[^a-zA-Z0-9]", "");
      if (name.isEmpty()) {
        name = "point" + (lineIdx + 1);
      }

      TurtleTurt.EndPoint end = line.endPoint;
      double deg = 0;
      if ("constant".equals(end.heading)) {
        deg = end.degrees;
      } else if ("linear".equals(end.heading)) {
        deg = end.endDeg;
      }
      if (!poses.containsKey(name)) {
        poses.put(name, toPose(end.x, end.y, deg));
      }

      if (line.controlPoints != null) {
        for (int cpIdx = 0; cpIdx < line.controlPoints.size(); cpIdx++) {
          TurtleTurt.ControlPoint cp = line.controlPoints.get(cpIdx);
          Pose controlPose = toPose(cp.x, cp.y, 0);
          poses.put(name + "_line" + lineIdx + "_control" + (cpIdx + 1), controlPose);
          // Code generated before control points were named by line asks for "<line>_control<n>".
          poses.putIfAbsent(name + "_control" + (cpIdx + 1), controlPose);
        }
      }
    }
  }

  /**
   * Converts a point from a Turtle Tracer file to a robot {@link Pose}.
   * <p>
   * Turtle Tracer stores Pedro field coordinates (origin at the field corner, heading 0 along +x), so
   * only the heading is converted, from degrees to radians. Earlier versions rotated points 90°
   * ({@code new Pose(y, 144 - x, deg - 90)}), which no longer matches what Turtle Tracer exports.
   * </p>
   *
   * @param x   The X coordinate in inches.
   * @param y   The Y coordinate in inches.
   * @param deg The heading in degrees.
   * @return The {@link Pose}.
   */
  public static Pose toPose(double x, double y, double deg) {
    return new Pose(x, y, Math.toRadians(deg));
  }

  /**
   * Converts a point to a robot {@link Pose} with 0 heading.
   *
   * @param x The X coordinate in inches.
   * @param y The Y coordinate in inches.
   * @return The {@link Pose}.
   */
  public static Pose toPose(double x, double y) {
    return toPose(x, y, 0);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// TURTLE TRACER FILE DEFINITIONS
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Represents the root structure of a Turtle Tracer (.turt) JSON file.
   */
  public static class TurtleTurt {
    public String version;
    public Header header;
    public StartPoint startPoint;
    public List<Line> lines;
    public List<SequenceItem> sequence;
    public List<Shape> shapes;
    public Map<String, Object> extraData;

    public static class Header {
      public String info;
      public String copyright;
      public String link;
    }

    public static class StartPoint {
      public double x;
      public double y;
      public String heading;
      public boolean locked;
      public double degrees;
      public double startDeg;
      public double endDeg;
    }

    public static class Line {
      public String id;
      public String name;
      /** The line's real name when it shares it with others and is saved as "Name (1)". */
      public String _linkedName;
      public EndPoint endPoint;
      public List<ControlPoint> controlPoints;
      public String color;
      public List<EventMarker> eventMarkers;
      public boolean locked;
      public long waitBeforeMs;
      public long waitAfterMs;
      public String waitBeforeName;
      public String waitAfterName;
      public boolean hidden;
      public boolean isChain;
      public String globalHeading;
      public double globalStartDeg;
      public double globalEndDeg;
    }

    public static class EndPoint {
      public double x;
      public double y;
      public String heading;
      public boolean reverse;
      public double startDeg;
      public double endDeg;
      public double degrees;
      public double targetX;
      public double targetY;
      public List<Segment> segments;
    }

    public static class Segment {
      public double tStart;
      public double tEnd;
      public String heading;
      public boolean reverse;
      public double startDeg;
      public double endDeg;
      public double targetX;
      public double targetY;
      public double degrees;
    }

    public static class ControlPoint {
      public double x;
      public double y;
    }

    public static class EventMarker {
      public String id;
      public String name;
      public String type; // "parametric", "temporal", "pose"
      public double position;
      public double[] points; // for backwards compatibility
      public long time;
      public long endTime;
      public double poseX;
      public double poseY;
      public double poseHeading;
      public int lineIndex;
    }

    public static class SequenceItem {
      public String kind; // "path", "wait", "rotate"
      public String id;
      public String lineId;
      public String name;
      public boolean isChain;
      public long durationMs;
      public double degrees;
      public boolean locked;
      public List<EventMarker> eventMarkers;
    }

    public static class Shape {
      public String id;
      public String name;
      public String type; // e.g. "obstacle"
      public String color;
      public String fillColor;
      public List<Vertex> vertices;
    }

    public static class Vertex {
      public double x;
      public double y;
    }
  }
}
