import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val managerVersion: String = providers.gradleProperty("managerVersion").get().removePrefix("v")
val managerVersionCode: Int = managerVersion.split('.').map(String::toInt).let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }

abstract class GenerateVersion : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        outputDir.get().file("app/reseam/manager/Version.kt").asFile.apply {
            parentFile.mkdirs()
            writeText("package app.reseam.manager\n\nconst val ManagerVersion = \"${version.get()}\"\n")
        }
    }
}

val generateVersion by tasks.registering(GenerateVersion::class) {
    version.set(managerVersion)
    outputDir.set(layout.buildDirectory.dir("generated/version/kotlin"))
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmCommon") {
                withAndroidTarget()
                withJvm()
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateVersion)
        }
        commonMain.dependencies {
            implementation(libs.reseam.sdk)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<Test>().configureEach {
    listOf("reseamTestBundle", "reseamTestOtherBundle").forEach { key ->
        providers.gradleProperty(key).orNull?.let { systemProperty(key, it) }
    }
}

android {
    namespace = "app.reseam.manager"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.reseam.manager"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = managerVersionCode
        versionName = managerVersion
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    val keystore = providers.environmentVariable("RESEAM_KEYSTORE").orNull
    if (keystore != null) {
        signingConfigs.create("release") {
            storeFile = file(keystore)
            storePassword = providers.environmentVariable("RESEAM_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("RESEAM_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("RESEAM_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.reseam.manager.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "app.reseam.manager"
            packageVersion = managerVersion
            linux { modules("jdk.security.auth") }
        }
    }
}

val stageRelease by tasks.registering(Sync::class) {
    dependsOn("assembleRelease", "packageDistributionForCurrentOS")
    from(layout.buildDirectory.dir("outputs/apk/release")) { include("*.apk") }
    from(layout.buildDirectory.dir("compose/binaries/main/deb"))
    from(layout.buildDirectory.dir("compose/binaries/main/rpm"))
    into(layout.buildDirectory.dir("release"))
}

tasks.register<Exec>("publishManagerIndex") {
    dependsOn(stageRelease)
    val index = layout.buildDirectory.file("release/manager.json").get().asFile
    outputs.file(index)
    commandLine(
        providers.environmentVariable("RESEAM_BIN").orElse("reseam").get(),
        "publish", "manager",
        "--name", "Reseam Manager",
        "--author", "Reseam",
        "--version", managerVersion,
        "--url", "https://git.reseam.app/reseam/manager/releases/tag/v$managerVersion",
        "--out", index.path,
    )
}
