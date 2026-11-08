pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://mymaven.bylazar.com/releases")
    }
}

rootProject.name = "PathRecorder"
include(":core", ":ivy", ":ftc", ":panels", ":sample")
