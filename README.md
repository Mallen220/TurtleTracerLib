# TurtleTracerLib

**TurtleTracerLib** is an advanced pathing library for the FIRST Tech Challenge (FTC), built on top of the powerful [Pedro Pathing](https://github.com/Pedro-Pathing/PedroPathing) library and integrating robust command-based structures. It is not affiliated with FIRST or the developers of Pedro Pathing.

> [!IMPORTANT]
> **STAY TUNED!**
> This repository is currently undergoing **rapid and constant updates**.
> Major improvements are planned for the coming weeks, including the ability to **run entire autonomous routines directly from `.turt` files**.
> Please watch this repository to stay up-to-date with the latest features and changes.

---

## Turtle Tracer

This library is designed to work hand-in-hand with the **Turtle Tracer**, a powerful desktop application for planning, simulating, and exporting your autonomous routines.

**[Download Turtle Tracer](https://github.com/Mallen220/TurtleTracer/releases)**

The Visualizer powers TurtleTracerLib by providing:

- **Visual Path Editing:** Intuitive drag-and-drop interface for Bezier curves and path chains.
- **Simulation:** Real-time physics simulation to verify your paths before they run on the robot.
- **Local File Management:** Save and organize `.turt` or `.pp` files directly on your machine.
- **Code & File Export:** Seamlessly export to Java code or `.turt` files for the upcoming execution engine.

---

## Installation

To use TurtleTracerLib in your FTC project, follow these steps:

### 1. Add Repositories

Add the following repositories to your `build.gradle` (Module: app) or `settings.gradle` file:

```groovy
maven { url "https://repo.dairy.foundation/releases" }

maven { url = 'https://mymaven.bylazar.com/releases' }

maven { url 'https://jitpack.io' }
```

### 2. Add Dependencies

Add the dependencies to your `build.gradle` (Module: app) dependencies block:

```groovy
dependencies {
    // TurtleTracerLib
    implementation 'com.github.Mallen220:TurtleTracerLib:master-SNAPSHOT' // or use a specific tag

    // Core Dependencies
    implementation 'com.pedropathing:core:3.0.1'
    implementation 'com.pedropathing:revhub:3.0.1'
}
```

---

## Reading Poses

Code exported from Turtle Tracer without "Embed Pose Data" reads its poses from the `.turt` file at runtime, so you can edit the path in Turtle Tracer without regenerating code:

```java
TurtleTracerReader reader = new TurtleTracerReader("AutoRoutine.turt", hardwareMap.appContext);
Pose start = reader.get("startPoint");
Pose score = reader.get("Score");
```

Poses are Pedro field coordinates, the same numbers Turtle Tracer writes into embedded code. They come from the start point and lines already in the file, named the way the generated code asks for them.

---

## Events System

Turtle Tracer provides an intuitive Events system that allows triggering robot mechanism actions during path following—without the need for complex state machines.

### 1. Using Events with `ProgressTracker` (LinearOpMode)
```java
ProgressTracker tracker = new ProgressTracker(follower, telemetry);
tracker.setCurrentPath(myPath);

// Register events fluently:
tracker.onParametric(0.75, () -> intake.start())       // 75% along curve
       .onTemporal(500, () -> arm.lift())              // 500ms after start
       .onSpatial(targetPose, 2.0, () -> claw.open()); // Within 2" of pose

// In your OpMode loop:
while (opModeIsActive()) {
    follower.update();
    tracker.update(); // Evaluates and executes any triggered events safely
}
```

### 2. Using Events with `FollowPathCommand` (Command-Based)
```java
Command autoPath = new FollowPathCommand(follower, myPath)
    .onParametric(0.8, () -> intake.start())
    .onTemporal(400, () -> arm.toIntake())
    .onSpatial(depositPose, 1.5, () -> outtake.deposit());
```

### 3. Binding Events to Visualizer `.turt` Event Markers
```java
TurtleTracerReader reader = new TurtleTracerReader("AutoRoutine.turt", hardwareMap.appContext);
reader.onEvent("IntakeMarker", () -> intake.start())
      .onEvent("ScoreMarker", () -> outtake.score());

// Registers markers and binds their actions to the tracker automatically:
reader.registerEvents(tracker);
```

---

## Generating Javadoc

This project provides a Gradle task to generate HTML Javadoc for the `app` Android library module.

To generate the Javadoc run from the repository root:

```bash
# Generate Javadoc for the app module
./gradlew :app:generateJavadoc
```

When the task completes successfully the HTML files will be written to:

```
app/build/docs/javadoc/index.html
```

Open that file in your browser to view the generated API documentation.

Notes:
- The task collects Java sources from the `main` source set. If you add Java files in other source sets update the `generateJavadoc` task in `app/build.gradle.kts`.
- The task uses a relaxed doclint config to avoid failures on older code. If you have doclint issues, consider cleaning up Javadoc comments or enabling stricter linting.

---

This project includes third-party components that are licensed separately.
See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)

---

**Built by [Mallen220](https://github.com/Mallen220) & Contributors**
Not officially affiliated with FIRST® or Pedro Pathing.

