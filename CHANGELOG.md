## v3.0.2 - 2026-10-03

- Updated to Pedro Pathing 3.0.1 (`com.pedropathing:core:3.0.1`, which fixes the path completion calculation) and the FTC SDK 12.0.0 (2026/27 season) as the compile-time targets. No source changes were needed.
- Updated the build to Android Gradle Plugin 8.13.2 and Gradle 9.1.0, matching the FTC SDK 12.0 project. Gradle 9.1 also runs on newer JDKs; JitPack still builds with JDK 17.

## v3.0.1 - 2026-10-03

- Updated to Pedro Pathing 3.0.1 (`com.pedropathing:core:3.0.1`, which fixes the path completion calculation) and the FTC SDK 12.0.0 (2026/27 season) as the compile-time targets. No source changes were needed.
- Updated the build to Android Gradle Plugin 8.13.2 and Gradle 9.1.0, matching the FTC SDK 12.0 project. Gradle 9.1 also runs on newer JDKs; JitPack still builds with JDK 17.
- Fixed `ProgressTracker.turn()` not marking the follower busy again. Pedro's `hold()` leaves `isBusy()` as it was, so a turn right after a finished path looked complete immediately. It now calls `follower.algorithm().reset()`.
- Added path-scoped events for routines that follow several paths in a row. `registerEvents(tracker)` registers every line's markers at once, so each one could trigger during whichever path was running.
  - `ProgressTracker.clearPathEvents()` removes the current path's events and keeps actions bound with `onEvent`.
  - `ProgressTracker.onParametric(segmentIndex, progress, action)` (and a range overload) only counts progress while the follower is on that segment of a chained path.
  - `TurtleTracerReader.registerLineEvents(tracker, lineIndex, segmentIndex)` registers one line's markers from the file.
- Fixed parametric markers registered by `TurtleTracerReader.registerEvents` running their action twice once `tracker.update()` ran (once as an event and once as a named zone). Only waits and turns, which fire through `executeEvent`, still register zones.
- `ProgressTracker.setCurrentPath()` now ends turn tracking, so a turn that was never seen finishing can't make the next path's events wait until the path ends.

## v3.0.0 - 2026-09-15

- Updated PedroPathing dependency to 3.0.0 (`com.pedropathing:core:3.0.0`).
- Updated `Pose` imports from `com.pedropathing.geometry.Pose` to `com.pedropathing.math.Pose`.
- Updated `Pose` coordinate accessors (`getX()`, `getY()`, `getHeading()` -> `x()`, `y()`, `heading()`).
- Updated `Follower` method calls for PedroPathing 3.0.0 (`getPose()` -> `pose()`, `getCurrentTValue()` -> `parametricCompletion()`, `getChainIndex()` -> `pathIndex()`, `breakFollowing()` -> `stop()`).
- Refactored `ProgressTracker`:
  - Replaced `PathChain` tracking with PedroPathing 3.0.0 `Path` tracking via `setCurrentPath(Path)`.
  - Replaced obsolete `chainProgress` variable with `totalProgress` (`getTotalProgress()`, `getCompletion()`).
  - Removed obsolete `setCurrentChain(Path)` method.
- Cleaned up `FollowPathCommand`:
  - Removed unneeded `maxPower` variable and constructors (power scaling is now configured via PedroPathing followers/drivetrains).
  - Removed obsolete no-op callback methods (`addParametricCallback`, `addTemporalCallback`).
- Introduced first-class **Events System** (`com.turtletracerlib.pathing.event`):
  - Added [`PathEvent`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/pathing/event/PathEvent.java) base class with safe exception handling so actions cannot stall the OpMode loop.
  - Added [`ParametricEvent`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/pathing/event/ParametricEvent.java) for triggering actions at specific $t$-values or progress ranges.
  - Added [`TemporalEvent`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/pathing/event/TemporalEvent.java) for triggering actions after a time duration has elapsed.
  - Added [`SpatialEvent`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/pathing/event/SpatialEvent.java) for triggering actions when within a radius of a target pose.
  - Integrated fluent event registration methods into [`ProgressTracker`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/pathing/ProgressTracker.java) (`onParametric`, `onCompletion`, `onTemporal`, `onSpatial`, `onEvent`) and added `tracker.update()`.
  - Integrated fluent event registration and execution into [`FollowPathCommand`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/command/FollowPathCommand.java).
  - Updated [`TurtleTracerReader`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/TurtleTracerReader.java) to fully support the Turtle Tracer visualizer format:
    - Parses all event marker types (`"parametric"`, `"temporal"`, `"pose"` / `"spatial"`), automatically mapping them to their respective `PathEvent` subclasses in `registerEvents(tracker)`.
    - Parses Bézier control points (accessible via `get("point_control1")`, etc.).
    - Parses full visualizer metadata: `version`, `header`, piecewise heading `segments`, `sequence` (paths, waits, rotations), and `shapes` (field obstacles).
    - Added overloaded constructors supporting `InputStream`, `Reader`, and raw JSON `String` for flexible loading outside Android assets.

## v2.1.0 - 2026-05-24

- Removed Command-Based Support
- Refactor NamedCommands to exclusively use Runnable
- Added support for NextFTC, SolversLib, and Basic java to use NamedCommands

## v2.0.0 - 2026-03-21

- **PROJECT RENAME**: Renamed project from `PedroPathingPlus` to `TurtleTracerLib`.
- Updated package names to `com.turtletracerlib`.
- Updated Gradle configurations and documentation.

## v1.0.6 - 2026-01-11

Added progress tracking for rotate events.

## v1.0.5 - 2025-12-25

Jitpack is now working!

# Changelog

All notable changes to this project will be documented in this file.

## v1.0.4 - 2025-12-25

Force .pom detection. 

