plugins {
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

android {
    namespace = "com.turtletracerlib"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// Publish to mavenLocal so JitPack (and local users) can get the generated POM/JAR.
// Create a publication from the Android 'release' component. JitPack will run
// `./gradlew ... publishToMavenLocal` and expect a POM named <artifactId>-<version>.pom.
afterEvaluate {
    publishing {
        publications {
            create<org.gradle.api.publish.maven.MavenPublication>("maven") {
                artifactId = "TurtleTracerLib"
                // Use the project group and version as provided (JitPack sets -Pgroup/-Pversion when invoking)
                // Attach the AAR produced by the Android library module. The AAR is created by the "assembleRelease" task.
                val aarFile = file("$buildDir/outputs/aar/${project.name}-release.aar")
                artifact(aarFile) {
                    builtBy(tasks.named("assembleRelease"))
                }

                // Set minimal coordinates if not defined
                if (project.group.toString().isNotEmpty()) {
                    groupId = project.group.toString()
                }
                if (project.version.toString().isNotEmpty()) {
                    version = project.version.toString()
                }

                // Provide a simple POM customization (optional)
                pom {
                    name.set("${project.name}")
                    description.set("TurtleTracerLib Android library")
                    url.set("https://github.com/${project.findProperty("githubOwner") ?: "Mallen220"}/${project.findProperty("githubRepo") ?: "TurtleTracerLib"}")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("Mallen220")
                            name.set("Mallen220")
                        }
                    }
                }
            }
        }
        repositories {
            mavenLocal()
        }
    }

    // Provide a resolvable configuration specifically for Javadoc generation dependencies.
    // This keeps Javadoc dependency resolution separate from the module's compile/runtime configs.
    configurations.register("javadocDeps") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    dependencies {
        compileOnly(libs.ftc.robotcore)
        compileOnly(libs.ftc.hardware)
        compileOnly(libs.ftc.robotserver)
        compileOnly(libs.ftc.common)

        implementation(libs.pedro.pathing)

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.test.ext.junit)
        androidTestImplementation(libs.espresso.core)

        // Javadoc resolver-only dependencies (do not affect the published artifact)
        add("javadocDeps", libs.ftc.robotcore)
        add("javadocDeps", libs.ftc.hardware)
        add("javadocDeps", libs.ftc.robotserver)
        add("javadocDeps", libs.ftc.common)
        add("javadocDeps", libs.pedro.pathing)
        // Gson is used in source; include a stable gson for Javadoc generation
        add("javadocDeps", "com.google.code.gson:gson:2.10.1")
    }

    // Javadoc generation for this Android library module.
    // Run with: ./gradlew :app:generateJavadoc
    // Output is written to app/build/docs/javadoc/index.html

    tasks.register("generateJavadoc", Javadoc::class) {
        description = "Generates Javadoc API documentation for the main Java sources."
        group = "documentation"

        // Collect source dirs from the Android main source set
        val sourceDirs = android.sourceSets.getByName("main").java.srcDirs
        // Also include javadoc-only stubs so Javadoc can resolve external types without resolving full SDKs
        val javadocStubDir = file("src/javadoc-stubs/java")
        val allSource = files(sourceDirs).asFileTree.plus(files(javadocStubDir).asFileTree)
        // Convert to a FileTree to satisfy the Javadoc task's expected type
        source = allSource

        // Build a classpath with the Android boot classpath and the resolvable javadocDeps
        val androidBootClasspath = files(android.bootClasspath)
        val javadocDepsConfig = configurations.getByName("javadocDeps")
        val javadocDepsFiles = try { files(javadocDepsConfig.resolve()) } catch (e: Exception) { files() }
        classpath = files(androidBootClasspath, javadocDepsFiles)

        // Put generated documentation in a predictable location
        destinationDir = file("$buildDir/docs/javadoc")

        // Encoding and options to avoid doclint failures on older codebases
        options.encoding = "UTF-8"
        (options as org.gradle.external.javadoc.StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            // Link to Android and Java SE API docs to get external references resolved
            links("https://developer.android.com/reference/")
            links("https://docs.oracle.com/en/java/javase/11/docs/api/")
        }
    }
}

dependencies {
    compileOnly(libs.ftc.robotcore)
    compileOnly(libs.ftc.hardware)
    compileOnly(libs.ftc.robotserver)
    compileOnly(libs.ftc.common)

    implementation(libs.pedro.pathing)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
