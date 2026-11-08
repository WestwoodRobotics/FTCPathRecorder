plugins {
    id("java-library")
    id("maven-publish")
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    compileOnly(libs.pedro.core)

    testImplementation(libs.pedro.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "pathrecorder-core"
            from(components["java"])
        }
    }
}
