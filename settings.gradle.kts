rootProject.name = "ReseamManager"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://git.reseam.app/api/packages/reseam/maven") {
            mavenContent { includeGroup("app.reseam") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val reseamWorkspace: String? = (settings.providers.gradleProperty("reseam.workspace").orNull
    ?: System.getenv("RESEAM_WORKSPACE"))?.takeIf { it.isNotBlank() }

if (reseamWorkspace != null) {
    val workspaceDir = file(reseamWorkspace)
    require(workspaceDir.isDirectory) {
        "reseam.workspace points to a missing directory: $workspaceDir"
    }
    includeBuild(workspaceDir.resolve("sdk-android"))
    includeBuild(workspaceDir.resolve("sdk-jvm"))
}

include(":composeApp")