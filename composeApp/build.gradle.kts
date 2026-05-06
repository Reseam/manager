import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Locale

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

fun reseamHostClassifier(): String {
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    val osArch = System.getProperty("os.arch").lowercase(Locale.ROOT)
    return when {
        osName.contains("linux") && (osArch == "amd64" || osArch == "x86_64") -> "linux-x86_64"
        osName.contains("linux") && (osArch == "aarch64" || osArch == "arm64") -> "linux-aarch64"
        (osName.contains("mac") || osName.contains("darwin")) &&
            (osArch == "aarch64" || osArch == "arm64") -> "darwin-arm64"
        (osName.contains("mac") || osName.contains("darwin")) && osArch == "x86_64" -> "darwin-x86_64"
        else -> throw GradleException("unsupported desktop host: $osName $osArch")
    }
}

val reseamSdkVersion = libs.versions.reseam.get()
val reseamHostClassifier = reseamHostClassifier()

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.reseam.sdk.android)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.navigationevent.compose)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutinesExtensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.sqliteDriver)
            implementation("app.reseam:reseam-sdk-jvm:${reseamSdkVersion}:${reseamHostClassifier}")
        }
    }
}

android {
    namespace = "app.reseam.manager"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.reseam.manager"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

sqldelight {
    databases {
        create("ReseamDatabase") {
            packageName.set("app.reseam.manager.data.db")
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.reseam.manager.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "app.reseam.manager"
            packageVersion = "1.0.0"
        }
    }
}
