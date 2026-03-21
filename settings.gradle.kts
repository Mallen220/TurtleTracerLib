pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://mymaven.bylazar.com/releases") }
        maven { url = uri("https://repo.dairy.foundation/releases") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TurtleTracerLib"
include(":app")
