plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("reseamNativeArtifacts") {
            id = "app.reseam.manager.native-artifacts"
            implementationClass = "app.reseam.manager.gradle.ReseamNativeArtifactsPlugin"
        }
    }
}
