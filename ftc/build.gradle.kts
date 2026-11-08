plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.westwoodrobotics.hungamunga.pathrecorder.ftc"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { it.useJUnitPlatform() }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":core"))
    compileOnly(libs.pedro.core)
    compileOnly(libs.bundles.ftc)

    testImplementation(libs.pedro.core)
    testImplementation(libs.bundles.ftc)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.launcher)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                artifactId = "pathrecorder-ftc"
                from(components["release"])
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-options")
}
