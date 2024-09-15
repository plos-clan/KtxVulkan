rootProject.name = "KtxVulkan"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.luna5ama.dev/")
    }

    plugins {
        id("com.google.devtools.ksp") version "2.0.20-1.0.24"
        id("dev.luna5ama.kmogus-struct-plugin") version "1.0-SNAPSHOT"
    }
}

include("structs")
