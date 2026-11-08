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

val ivyPedroAar: Configuration by configurations.creating

dependencies {
    api(project(":core"))
    compileOnly(libs.pedro.core)
    compileOnly(libs.ivy.core)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.pedro.core)
    testImplementation(libs.ivy.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    ivyPedroAar("${libs.ivy.pedro.get()}@aar")
}

// ivy's pedro module ships as an aar, the compile test needs its classes.jar
val extractIvyPedro by tasks.registering(Copy::class) {
    from({ ivyPedroAar.map { zipTree(it) } }) { include("classes.jar") }
    into(layout.buildDirectory.dir("ivy-pedro"))
}

tasks.test {
    useJUnitPlatform()
    dependsOn(extractIvyPedro)
    systemProperty("ivyPedroJar", layout.buildDirectory.file("ivy-pedro/classes.jar").get().asFile.absolutePath)
    systemProperty("goldenDir", file("src/test/resources/golden").absolutePath)
    System.getProperty("updateGolden")?.let { systemProperty("updateGolden", it) }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "pathrecorder-ivy"
            from(components["java"])
        }
    }
}
