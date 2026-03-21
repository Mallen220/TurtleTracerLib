# TurtleTracerLib

**TurtleTracerLib** is an advanced pathing library for the FIRST Tech Challenge (FTC), built on top of the powerful [Pedro Pathing](https://github.com/Pedro-Pathing/PedroPathing) library and integrating robust command-based structures.

> [!IMPORTANT]
> **STAY TUNED!**
> This repository is currently undergoing **rapid and constant updates**.
> Major improvements are planned for the coming weeks, including the ability to **run entire autonomous routines directly from `.turt` files**.
> Please watch this repository to stay up-to-date with the latest features and changes.

---

## 🎨 Turtle Tracer

This library is designed to work hand-in-hand with the **Turtle Tracer**, a powerful desktop application for planning, simulating, and exporting your autonomous routines.

**[Download Turtle Tracer](https://github.com/Mallen220/TurtleTracer/releases)**

The Visualizer powers TurtleTracerLib by providing:

- **Visual Path Editing:** Intuitive drag-and-drop interface for Bezier curves and path chains.
- **Simulation:** Real-time physics simulation to verify your paths before they run on the robot.
- **Local File Management:** Save and organize `.turt` or `.pp` files directly on your machine.
- **Code & File Export:** Seamlessly export to Java code or `.turt` files for the upcoming execution engine.

---

## 📥 Installation

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
    implementation 'com.pedropathing:ftc:2.0.0'
    implementation 'org.solverslib:core:0.3.3' // Will be replaced with PedproPathingPlus-specific version in future
    implementation 'org.solverslib:pedroPathing:0.3.3'
}
```

---

## 🚀 Upcoming Features

We are working hard to bring you:

- **Direct `.pp` Execution:** Run autonomous routines defined in `.pp` files without writing boilerplate Java code.
- **Enhanced Command Integration:** Tighter integration with the command-based paradigm.
- **Improved Documentation:** Comprehensive guides and examples.

---

## 📚 Generating Javadoc

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

**Built by [Mallen220](https://github.com/Mallen220) & Contributors**
