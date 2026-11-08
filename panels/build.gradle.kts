plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.westwoodrobotics.hungamunga.pathrecorder.panels"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":ftc"))
    compileOnly(libs.pedro.core)
    compileOnly(libs.bundles.ftc)
    compileOnly(libs.panels.field)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                artifactId = "pathrecorder-panels"
                from(components["release"])
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-options")
}
