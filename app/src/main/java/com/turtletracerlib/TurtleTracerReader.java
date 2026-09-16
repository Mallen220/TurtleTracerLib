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
   * The last recorded X coordinate during parsing.
   */
  private double lastX;

  /**
   * The last recorded Y coordinate during parsing.
   */
  private double lastY;

  /**
   * The last recorded heading (in degrees) during parsing.
   */
  private double lastDeg;

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
   *   <li>Bézier control points (e.g., {@code "greg_control1"}, {@code "geerger_control2"}).</li>
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
            registerSingleMarker(tracker, marker);
          }
        }
      }
    }

    // Register event markers on sequence items (waits, rotations, etc.)
    if (file.sequence != null) {
      for (TurtleTurt.SequenceItem item : file.sequence) {
        if (item.eventMarkers != null) {
          for (TurtleTurt.EventMarker marker : item.eventMarkers) {
            registerSingleMarker(tracker, marker);
          }
        }
      }
    }
  }

  /**
   * Registers a single event marker with the tracker according to its type.
   *
   * @param tracker The progress tracker.
   * @param marker  The event marker definition.
   */
  private void registerSingleMarker(ProgressTracker tracker, TurtleTurt.EventMarker marker) {
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
        if (marker.points != null && marker.points.length > 0) {
          if (marker.points.length == 1) {
            tracker.onParametric(marker.points[0], action);
            tracker.registerEvent(marker.name, marker.points[0]);
          } else {
            tracker.onParametric(marker.points[0], marker.points[1], action);
            tracker.registerEvent(marker.name, marker.points[0], marker.points[1]);
          }
        } else {
          tracker.onParametric(marker.position, action);
          tracker.registerEvent(marker.name, marker.position);
        }
        break;
    }
  }

  /**
   * Processes the raw data from the {@code TurtleTurt} object and populates the {@code poses} map.
   */
  private void loadAllPoints() {
    if (file == null) {
      return;
    }

    if (file.startPoint != null) {
      double x = file.startPoint.x;
      double y = file.startPoint.y;
      double deg = file.startPoint.startDeg;
      if (Double.isNaN(deg)) deg = 0;

      lastX = x;
      lastY = y;
      lastDeg = deg;

      poses.put("startPoint", toPose(lastX, lastY, lastDeg));
    }

    if (file.lines != null) {
      for (int lineIdx = 0; lineIdx < file.lines.size(); lineIdx++) {
        TurtleTurt.Line line = file.lines.get(lineIdx);
        if (line.endPoint != null) {
          double lx = line.endPoint.x;
          double ly = line.endPoint.y;

          double heading = extractHeading(line.endPoint.heading, lastX, lastY, lx, ly, lastDeg);

          String name = line.name != null ? line.name.replace(" ", "") : "line" + lineIdx;
          poses.put(name, toPose(lx, ly, heading));

          // Parse and populate Bézier control points
          if (line.controlPoints != null) {
            for (int cpIdx = 0; cpIdx < line.controlPoints.size(); cpIdx++) {
              TurtleTurt.ControlPoint cp = line.controlPoints.get(cpIdx);
              Pose cpPose = toPose(cp.x, cp.y, 0);
              poses.put(name + "_control" + (cpIdx + 1), cpPose);
              poses.put(name + "_line" + lineIdx + "_control" + (cpIdx + 1), cpPose);
            }
          }

          lastX = lx;
          lastY = ly;
          lastDeg = heading;
        }
      }
    }
  }

  /**
   * Converts the visualizer's coordinate system to the robot's {@link Pose}.
   * <p>
   * Transformations:
   * <ul>
   *   <li>Swaps X and Y: {@code newX = visualizerY}.</li>
   *   <li>Inverts X: {@code newY = 144 - visualizerX}.</li>
   *   <li>Adjusts heading: {@code radians = Math.toRadians(deg - 90)}.</li>
   * </ul>
   * </p>
   *
   * @param x   The X coordinate from the visualizer.
   * @param y   The Y coordinate from the visualizer.
   * @param deg The heading in degrees from the visualizer.
   * @return The converted {@link Pose}.
   */
  public static Pose toPose(double x, double y, double deg) {
    return new Pose(y, 144 - x, Math.toRadians(deg - 90));
  }

  /**
   * Converts visualizer coordinates to robot {@link Pose} with 0 heading.
   *
   * @param x The X coordinate from the visualizer.
   * @param y The Y coordinate from the visualizer.
   * @return The converted {@link Pose}.
   */
  public static Pose toPose(double x, double y) {
    return toPose(x, y, 0);
  }

  /**
   * Calculates the heading for a point based on the specified mode and previous coordinates.
   */
  private static double extractHeading(
      String mode, double lastX, double lastY, double x, double y, double lastDeg) {
    if (mode == null) {
      return lastDeg;
    }
    double dx = x - lastX;
    double dy = y - lastY;

    if (Math.abs(dx) < 1e-6 && Math.abs(dy) < 1e-6) {
      return lastDeg;
    }

    double linearDeg = Math.toDegrees(Math.atan2(dy, dx));

    if (mode.equals("linear") || mode.equals("tangential")) {
      return linearDeg;
    }

    return lastDeg;
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
      public double startDeg;
      public double endDeg;
    }

    public static class Line {
      public String id;
      public String name;
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
