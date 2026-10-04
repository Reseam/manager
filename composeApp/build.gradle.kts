import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.util.zip.ZipFile
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

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
        val jvmCommonMain by getting
        jvmCommonMain.dependencies {
            implementation(libs.reseam.sdk)
            implementation(libs.okhttp)
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
            implementation(compose.desktop.common)
            runtimeOnly(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.webview)
            implementation(libs.jna)
        }
    }
}

val windowsRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    extendsFrom(*kotlin.targets.getByName("jvm").compilations.getByName("main").allKotlinSourceSets
        .map { configurations.getByName(it.implementationConfigurationName) }.toTypedArray())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
}

dependencies {
    windowsRuntimeClasspath(compose.desktop.windows_x64)
}

android {
    namespace = "app.reseam.manager"
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) { minorApiLevel = 0 }
    }

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
            linux {
                modules("jdk.security.auth")
                iconFile.set(project.file("packaging/icon.png"))
            }
        }
    }
}

val packageArch by tasks.registering(Exec::class) {
    dependsOn("createDistributable")
    val output = layout.buildDirectory.dir("compose/binaries/main/arch")
    outputs.dir(output)
    workingDir("packaging/arch")
    environment("MANAGER_VERSION", managerVersion)
    environment("APP_IMAGE", layout.buildDirectory.dir("compose/binaries/main/app/app.reseam.manager").get().asFile.path)
    environment("BUILDDIR", layout.buildDirectory.dir("tmp/makepkg").get().asFile.path)
    environment("PKGDEST", output.get().asFile.path)
    environment("PKGEXT", ".pkg.tar.zst")
    commandLine("makepkg", "--force", "--nodeps")
}

fun temurinJdk(platform: String) = layout.buildDirectory.dir("tools/$platform-jdk")
fun downloadTemurinJdk(platform: String) = tasks.register<Exec>("download${platform.replaceFirstChar(Char::uppercase)}Jdk") {
    inputs.property("version", libs.versions.packaging.jdk)
    inputs.file("packaging/download-jdk.sh")
    outputs.dir(temurinJdk(platform))
    onlyIf {
        val cached = outputs.files.singleFile
        val release = cached.resolve("release")
        !cached.resolve("jmods/java.base.jmod").isFile || !release.isFile ||
            !release.readText().contains("IMPLEMENTOR_VERSION=\"Temurin-${inputs.properties.getValue("version")}\"")
    }
    commandLine("bash", "packaging/download-jdk.sh", libs.versions.packaging.jdk.get(), platform, temurinJdk(platform).get().asFile.path)
}

// jlink only links jmods from its own exact JDK build, so it runs from a Linux Temurin of the same version as the Windows jmods.
val downloadWindowsJdk = downloadTemurinJdk("windows")
val downloadLinuxJdk = downloadTemurinJdk("linux")

val windowsRuntime = layout.buildDirectory.dir("windows/runtime")
val linkWindowsRuntime by tasks.registering(Exec::class) {
    dependsOn(downloadWindowsJdk, downloadLinuxJdk)
    inputs.dir(temurinJdk("windows").map { it.dir("jmods") })
    outputs.dir(windowsRuntime)
    doFirst { outputs.files.singleFile.deleteRecursively() }
    commandLine(
        temurinJdk("linux").get().file("bin/jlink").asFile.path,
        "--module-path", temurinJdk("windows").get().dir("jmods").asFile.path,
        "--add-modules", "java.desktop,java.net.http,java.logging,java.management,jdk.crypto.ec,jdk.security.auth,jdk.unsupported,jdk.zipfs",
        "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=zip-6",
        "--output", windowsRuntime.get().asFile.path,
    )
}

val windowsApp = layout.buildDirectory.dir("windows/app")
val nsis = providers.environmentVariable("MAKENSIS").orElse("makensis")
val windowsLauncherFile = layout.buildDirectory.file("windows/launcher/Reseam Manager.exe")
val windowsLauncher by tasks.registering(Exec::class) {
    inputs.files("packaging/windows/launcher.nsi", "packaging/windows/icon.ico")
    outputs.file(windowsLauncherFile)
    doFirst { outputs.files.singleFile.parentFile.mkdirs() }
    commandLine(nsis.get(), "-DOUTPUT_FILE=${windowsLauncherFile.get().asFile.path}", "packaging/windows/launcher.nsi")
}

abstract class VerifyWindowsSdk : DefaultTask() {
    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val native = "native/windows-x86_64/reseam_sdk_native_jni.dll"
        check(classpath.files.filter { it.extension == "jar" }.any { file -> ZipFile(file).use { it.getEntry(native) != null } }) {
            "The Reseam JVM SDK must bundle $native before a Windows installer can be released. Rebuild or update app.reseam:reseam-sdk."
        }
    }
}

val verifyWindowsSdk by tasks.registering(VerifyWindowsSdk::class) {
    classpath.from(windowsRuntimeClasspath)
}

val stageWindowsApp by tasks.registering(Sync::class) {
    dependsOn("jvmJar", linkWindowsRuntime, windowsLauncher)
    filePermissions { unix("rw-r--r--") }
    dirPermissions { unix("rwxr-xr-x") }
    from(tasks.named("jvmJar")) { into("lib") }
    from(windowsRuntimeClasspath) {
        into("lib")
        val artifacts = windowsRuntimeClasspath.incoming.artifacts.resolvedArtifacts
        eachFile {
            val component = artifacts.get().single { it.file == file }.id.componentIdentifier
            if (component is ModuleComponentIdentifier) name = "${component.group}-$name"
        }
    }
    from(windowsRuntime) { into("runtime") }
    from("packaging/windows/icon.ico")
    from(windowsLauncherFile)
    into(windowsApp)
}

val windowsPackages = layout.buildDirectory.dir("compose/binaries/main/windows")
val packageWindows by tasks.registering(Exec::class) {
    dependsOn(stageWindowsApp)
    inputs.dir(windowsApp)
    inputs.file("packaging/windows/installer.nsi")
    inputs.property("version", managerVersion)
    outputs.file(windowsPackages.map { it.file("reseam-manager-$managerVersion-windows-x64.exe") })
    doFirst { outputs.files.singleFile.parentFile.mkdirs() }
    commandLine(
        nsis.get(), "-DAPP_DIR=${windowsApp.get().asFile.path}", "-DVERSION=$managerVersion",
        "-DOUTPUT_DIR=${windowsPackages.get().asFile.path}", "packaging/windows/installer.nsi",
    )
}

val stageRelease by tasks.registering(Sync::class) {
    dependsOn("assembleRelease", "packageDistributionForCurrentOS", packageArch, packageWindows, verifyWindowsSdk)
    from(layout.buildDirectory.dir("outputs/apk/release")) { include("*.apk") }
    from(layout.buildDirectory.dir("compose/binaries/main/deb"))
    from(layout.buildDirectory.dir("compose/binaries/main/rpm"))
    from(layout.buildDirectory.dir("compose/binaries/main/arch"))
    from(windowsPackages)
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
