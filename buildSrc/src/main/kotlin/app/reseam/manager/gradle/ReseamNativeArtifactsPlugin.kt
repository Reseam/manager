package app.reseam.manager.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.util.Locale
import javax.inject.Inject

class ReseamNativeArtifactsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "reseamNative",
            ReseamNativeArtifactsExtension::class.java,
        )

        extension.workspaceDir.convention(
            project.providers.gradleProperty("reseam.workspace")
                .orElse(project.providers.environmentVariable("RESEAM_WORKSPACE"))
                .map { project.rootProject.layout.projectDirectory.dir(it) },
        )
        extension.generatedSourcesDir.convention(extension.workspaceDir.dir("sdk/generated"))
        extension.androidJniLibsDir.convention(project.layout.buildDirectory.dir("generated/reseamNative/android"))
        extension.desktopResourcesDir.convention(project.layout.buildDirectory.dir("generated/reseamNative/desktop"))

        val syncAndroid = project.tasks.register(
            "syncReseamAndroidNativeLibraries",
            SyncReseamAndroidNativeLibraries::class.java,
        ) {
            workspaceDir.set(extension.workspaceDir)
            sdkJniLibsDir.set(extension.workspaceDir.dir("sdk/jniLibs"))
            rustTargetDir.set(extension.workspaceDir.dir("target"))
            outputDir.set(extension.androidJniLibsDir)
            targets.set(defaultAndroidTargets())
        }

        val buildDesktopRust = project.tasks.register(
            "buildReseamManagerFfiForDesktop",
            BuildReseamManagerFfiForDesktop::class.java,
        ) {
            workspaceDir.set(extension.workspaceDir)
        }

        val buildDesktopNative = project.tasks.register(
            "buildReseamDesktopNativeLibrary",
            BuildReseamDesktopNativeLibrary::class.java,
        ) {
            dependsOn(buildDesktopRust)
            workspaceDir.set(extension.workspaceDir)
            outputDir.set(extension.desktopResourcesDir)
        }

        project.tasks.matching { it.name == "preBuild" }.configureEach {
            dependsOn(syncAndroid)
        }
        project.tasks.matching { it.name == "jvmProcessResources" }.configureEach {
            dependsOn(buildDesktopNative)
        }
    }

    private fun defaultAndroidTargets(): List<AndroidNativeTarget> =
        listOf(
            AndroidNativeTarget("arm64-v8a", "aarch64-linux-android"),
            AndroidNativeTarget("armeabi-v7a", "armv7-linux-androideabi"),
            AndroidNativeTarget("x86", "i686-linux-android"),
            AndroidNativeTarget("x86_64", "x86_64-linux-android"),
        )
}

abstract class ReseamNativeArtifactsExtension @Inject constructor() {
    abstract val workspaceDir: DirectoryProperty
    abstract val generatedSourcesDir: DirectoryProperty
    abstract val androidJniLibsDir: DirectoryProperty
    abstract val desktopResourcesDir: DirectoryProperty
}

data class AndroidNativeTarget(
    @get:Input val abi: String,
    @get:Input val rustTriple: String,
)

abstract class SyncReseamAndroidNativeLibraries : DefaultTask() {
    @get:Internal
    abstract val workspaceDir: DirectoryProperty

    @get:InputDirectory
    abstract val sdkJniLibsDir: DirectoryProperty

    @get:InputDirectory
    abstract val rustTargetDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Nested
    abstract val targets: ListProperty<AndroidNativeTarget>

    @TaskAction
    fun sync() {
        val output = outputDir.get().asFile
        val sdkJniLibs = sdkJniLibsDir.get().asFile
        val rustTarget = rustTargetDir.get().asFile
        val missing = mutableListOf<File>()

        targets.get().forEach { target ->
            val managerLibrary = sdkJniLibs.resolve("${target.abi}/libreseam-sdk.so")
            val patcherLibrary = rustTarget.resolve("${target.rustTriple}/release/deps/libreseam_patcher.so")
            if (!managerLibrary.isFile) missing += managerLibrary
            if (!patcherLibrary.isFile) missing += patcherLibrary
        }

        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing Reseam Android native libraries:\n" +
                    missing.joinToString(separator = "\n") { " - $it" } +
                    "\nPrepare the Reseam SDK first. For local development, set " +
                    "`-Preseam.workspace=/path/to/reseam` or `RESEAM_WORKSPACE=/path/to/reseam`.",
            )
        }

        output.deleteRecursively()
        targets.get().forEach { target ->
            val abiOutput = output.resolve(target.abi).also { it.mkdirs() }
            sdkJniLibs
                .resolve("${target.abi}/libreseam-sdk.so")
                .copyTo(abiOutput.resolve("libreseam_sdk.so"), overwrite = true)
            rustTarget
                .resolve("${target.rustTriple}/release/deps/libreseam_patcher.so")
                .copyTo(abiOutput.resolve("libreseam_patcher.so"), overwrite = true)
        }
    }
}

abstract class BuildReseamManagerFfiForDesktop @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:InputDirectory
    abstract val workspaceDir: DirectoryProperty

    @TaskAction
    fun build() {
        execOperations.exec {
            workingDir = workspaceDir.get().asFile
            commandLine("cargo", "build", "-p", "reseam-sdk", "--release")
        }
    }
}

abstract class BuildReseamDesktopNativeLibrary @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:InputDirectory
    abstract val workspaceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    val hostTarget: DesktopNativeTarget = currentDesktopNativeTarget()

    @TaskAction
    fun build() {
        val workspace = workspaceDir.get().asFile
        val javaHome = javaHomeDirectory()
        val nativeOutput = outputDir.get().asFile
            .resolve("native/${hostTarget.resourceDirectory}/${hostTarget.libraryFileName}")
        nativeOutput.parentFile.mkdirs()

        execOperations.exec {
            commandLine(
                "cc",
                "-shared",
                "-fPIC",
                "-I${javaHome.resolve("include")}",
                "-I${javaHome.resolve("include/${hostTarget.jniIncludeDirectory}")}",
                "-I${workspace.resolve("sdk/dist/android/include")}",
                workspace.resolve("sdk/generated/jni/jni_glue.c").absolutePath,
                workspace.resolve("target/release/libreseam_sdk.a").absolutePath,
                "-ldl",
                "-lpthread",
                "-lm",
                "-o",
                nativeOutput.absolutePath,
            )
        }
    }
}

data class DesktopNativeTarget(
    val resourceDirectory: String,
    val libraryFileName: String,
    val jniIncludeDirectory: String,
)

private fun currentDesktopNativeTarget(): DesktopNativeTarget {
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    val osArch = System.getProperty("os.arch").lowercase(Locale.ROOT)

    return when {
        osName.contains("linux") && (osArch == "x86_64" || osArch == "amd64") ->
            DesktopNativeTarget("linux-x86_64", "libreseam_sdk_jni.so", "linux")

        osName.contains("linux") && (osArch == "aarch64" || osArch == "arm64") ->
            DesktopNativeTarget("linux-aarch64", "libreseam_sdk_jni.so", "linux")

        (osName.contains("mac") || osName.contains("darwin")) && (osArch == "aarch64" || osArch == "arm64") ->
            DesktopNativeTarget("darwin-arm64", "libreseam_sdk_jni.dylib", "darwin")

        (osName.contains("mac") || osName.contains("darwin")) && osArch == "x86_64" ->
            DesktopNativeTarget("darwin-x86_64", "libreseam_sdk_jni.dylib", "darwin")

        else -> throw GradleException("Unsupported desktop native target: $osName/$osArch")
    }
}

private fun javaHomeDirectory(): File {
    val envJavaHome = System.getenv("JAVA_HOME")
    if (!envJavaHome.isNullOrBlank()) {
        return File(envJavaHome)
    }

    val runtimeHome = File(System.getProperty("java.home"))
    return if (runtimeHome.resolve("include").isDirectory) {
        runtimeHome
    } else {
        runtimeHome.parentFile
    }
}
