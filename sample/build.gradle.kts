plugins {
    id("com.android.library")
}

android {
    namespace = "org.firstinspires.ftc.teamcode"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    implementation(project(":ftc"))
    implementation(project(":ivy"))
    implementation(project(":panels"))
    compileOnly(libs.pedro.core)
    compileOnly(libs.ivy.core)
    compileOnly(libs.ivy.pedro)
    compileOnly(libs.bundles.ftc)
    compileOnly(libs.panels.field)
}
