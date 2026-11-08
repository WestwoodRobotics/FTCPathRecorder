plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
}

val libraryGroup = property("group") as String
val libraryVersion = property("version") as String

subprojects {
    group = libraryGroup
    version = libraryVersion
}
