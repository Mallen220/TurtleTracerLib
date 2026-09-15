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
  - Added direct event binding in [`TurtleTracerReader`](file:///Users/matthew/Documents/GitHub/TurtleTracerLib/app/src/main/java/com/turtletracerlib/TurtleTracerReader.java) (`reader.onEvent("MarkerName", action)`) to seamlessly execute actions placed via the visualizer app.

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

