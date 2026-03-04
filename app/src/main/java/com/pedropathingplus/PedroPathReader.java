package com.pedropathingplus;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pedropathing.geometry.Pose;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pedropathingplus.pathing.ProgressTracker;

/**
 * A utility class for reading and parsing Pedro Pathing (.pp) files from the Android assets directory.
 * <p>
 * This class handles the deserialization of the JSON-based path files and converts the coordinate system
 * used in the visualizer to the robot's coordinate system (Pose). It caches the parsed poses for quick retrieval
 * by name.
 * </p>
 */
public final class PedroPathReader {

  /**
   * The parsed structure of the Pedro Pathing file.
   */
  private final PedroPP file;

  /**
   * A map storing the parsed poses, keyed by their name (with spaces removed).
   */
  private final Map<String, Pose> poses = new HashMap<>();

  /**
   * The last recorded X coordinate during parsing, used for relative calculations or heading extraction.
   */
  private double lastX;

  /**
   * The last recorded Y coordinate during parsing, used for relative calculations or heading extraction.
   */
  private double lastY;

  /**
   * The last recorded heading (in degrees) during parsing, used when no new heading is specified.
   */
  private double lastDeg;

  /**
   * Constructs a new {@code PedroPathReader} and loads the specified path file.
   *
   * @param filename The name of the .pp file to load (relative to the "AutoPaths" folder in assets).
   * @param context  The Android context used to access the assets manager.
   * @throws IOException If the file cannot be found or an error occurs during reading.
   */
  public PedroPathReader(String filename, Context context) throws IOException {
    InputStream stream = null;
    try {
      stream = context.getAssets().open("AutoPaths/" + filename);
    } catch (IOException e) {
      throw e;
    }

    if (stream == null) {
      throw new FileNotFoundException("PP File not found: " + filename);
    }

    Gson gson = new GsonBuilder().create();
    try (InputStreamReader reader = new InputStreamReader(stream)) {
      this.file = gson.fromJson(reader, PedroPP.class);
    }

    loadAllPoints();
  }

  /**
   * Retrieves the raw list of line segments parsed from the JSON.
   * This is useful for building autonomous routines that need to process lines and their event markers.
   *
   * @return The list of line segments.
   */
  public List<PedroPP.Line> getLines() {
      return file.lines;
  }

  /**
   * Processes the raw data from the {@code PedroPP} object and populates the {@code poses} map.
   * <p>
   * This method iterates through the start point and all lines defined in the file, converting
   * coordinates and calculating headings as necessary.
   * </p>
   */
  private void loadAllPoints() {
    double x = file.startPoint.x;
    double y = file.startPoint.y;
    double deg = file.startPoint.startDeg;
    if (Double.isNaN(deg)) deg = 0;

    lastX = x;
    lastY = y;
    lastDeg = deg;

    poses.put("startPoint", toPose(lastX, lastY, lastDeg));

    for (PedroPP.Line line : file.lines) {
      double lx = line.endPoint.x;
      double ly = line.endPoint.y;

      double heading = extractHeading(line.endPoint.heading, lastX, lastY, lx, ly, lastDeg);

      String name = line.name.replace(" ", "");
      poses.put(name, toPose(lx, ly, heading));

      lastX = lx;
      lastY = ly;
      lastDeg = heading;
    }
  }

  /**
   * Registers all event markers parsed from the JSON file to the provided {@link ProgressTracker}.
   * <p>
   * Iterates through all lines and their associated event markers. If a marker has a single point,
   * it registers a single point event. If it has two points, it registers a zoned event.
   * </p>
   *
   * @param tracker The {@link ProgressTracker} to register the events to.
   */
  public void registerEvents(ProgressTracker tracker) {
    if (file == null || file.lines == null) return;

    for (PedroPP.Line line : file.lines) {
      if (line.eventMarkers != null) {
        for (PedroPP.EventMarker marker : line.eventMarkers) {
          if (marker.points != null && marker.points.length > 0) {
            if (marker.points.length == 1) {
              tracker.registerEvent(marker.name, marker.points[0]);
            } else if (marker.points.length >= 2) {
              tracker.registerEvent(marker.name, marker.points[0], marker.points[1]);
            }
          }
        }
      }
    }
  }

  /**
   * Retrieves a parsed {@link Pose} by its name.
   *
   * @param name The name of the point or line (e.g., "startPoint", "spikeMark").
   * @return The corresponding {@link Pose}, or {@code null} if not found.
   */
  public Pose get(String name) {
    return poses.get(name);
  }

  /**
   * Converts the visualizer's coordinate system to the robot's {@link Pose}.
   * <p>
   * The visualizer typically uses a different coordinate frame (e.g., origin top-left vs center).
   * This method applies the necessary transformations:
   * <ul>
   *     <li>Swaps X and Y.</li>
   *     <li>Inverts X (144 - x).</li>
   *     <li>Adjusts heading by -90 degrees and converts to radians.</li>
   * </ul>
   * </p>
   *
   * @param x   The X coordinate from the visualizer.
   * @param y   The Y coordinate from the visualizer.
   * @param deg The heading in degrees from the visualizer.
   * @return The converted {@link Pose}.
   */
  private static Pose toPose(double x, double y, double deg) {
    return new Pose(y, 144 - x, Math.toRadians(deg - 90));
  }

  /**
   * Calculates the heading for a point based on the specified mode and previous coordinates.
   *
   * @param mode    The heading mode ("linear", "tangential", or others).
   * @param lastX   The X coordinate of the previous point.
   * @param lastY   The Y coordinate of the previous point.
   * @param x       The X coordinate of the current point.
   * @param y       The Y coordinate of the current point.
   * @param lastDeg The heading of the previous point.
   * @return The calculated heading in degrees.
   */
  private static double extractHeading(
      String mode, double lastX, double lastY, double x, double y, double lastDeg) {
    double dx = x - lastX;
    double dy = y - lastY;

    if (Math.abs(dx) < 1e-6 && Math.abs(dy) < 1e-6) {
      return lastDeg;
    }

    double linearDeg = Math.toDegrees(Math.atan2(dy, dx));

    if (mode.equals("linear")) return linearDeg;
    if (mode.equals("tangential")) return linearDeg;

    return lastDeg;
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
///                                                                                              ///
///  PEDRO PP FILE DEFINITIONS                                                                   ///
///                                                                                              ///
////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Represents the root structure of a Pedro Pathing (.pp) JSON file.
 * <p>
 * This class matches the JSON schema expected by the parser.
 * </p>
 */
class PedroPP {

  /**
   * The starting point of the path.
   */
  public StartPoint startPoint;

  /**
   * A list of lines (path segments) following the start point.
   */
  public List<Line> lines;

  /**
   * Represents the start point definition in the JSON file.
   */
  public static class StartPoint {
    /** The X coordinate of the start point. */
    public double x;
    /** The Y coordinate of the start point. */
    public double y;
    /** The heading string description (unused in logic but present in JSON). */
    public String heading;
    /** The starting heading in degrees. */
    public double startDeg;
    /** The ending heading in degrees (unused in start point logic typically). */
    public double endDeg;
  }

  /**
   * Represents a line segment definition in the JSON file.
   */
  public static class Line {
    /** The name of the line segment (e.g., "scorePreload"). */
    public String name;
    /** The endpoint definition of this line segment. */
    public EndPoint endPoint;
    /** The event markers associated with this line segment. */
    public List<EventMarker> eventMarkers;
  }

  /**
   * Represents an event marker in the JSON file.
   */
  public static class EventMarker {
    /** The name of the event marker. */
    public String name;
    /** The points defining the event marker zone. Single value for point, two for zone. */
    public double[] points;
  }

  /**
   * Represents the endpoint of a line segment in the JSON file.
   */
  public static class EndPoint {
    /** The X coordinate of the endpoint. */
    public double x;
    /** The Y coordinate of the endpoint. */
    public double y;
    /** The heading mode or value string (e.g., "tangential", "linear"). */
    public String heading;
    /** Whether the segment is traversed in reverse. */
    public boolean reverse;
  }
}
