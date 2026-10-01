This is the Kotlin Multiplatform manager for Reseam: one Compose codebase for Android, Linux desktop, and Windows x64.

```text
composeApp/src/jvmCommonMain/kotlin/app/reseam/manager/data      bundles, patched apps, settings, trust
composeApp/src/jvmCommonMain/kotlin/app/reseam/manager/sdk       typed SDK calls, persistence adapters, and presentation helpers
composeApp/src/jvmCommonMain/kotlin/app/reseam/manager/ui        Compose UI, theme, routing, and viewmodels
composeApp/src/jvmCommonMain/kotlin/app/reseam/manager/platform  platform contracts (HTTP, installed apps, app presentation, installing)
composeApp/src/jvmCommonMain                                  implementations shared by Android and desktop
composeApp/src/androidMain                                    Android application and platform adapters
composeApp/src/jvmMain                                        desktop entry point and platform adapters
```

## Mental Model

1. `App.kt` owns the app shell and route table. `AppGraph` builds the repositories every screen shares.
2. Viewmodels own UI state transitions. They should not render UI and should expose one obvious path for each user action.
3. Repositories in `data/` persist state as JSON files and decide what is trusted: the official bundle is pinned to Reseam's signing key, anything else is confirmed by the user once per signer.
4. Platform adapters do HTTP, file picking, app presentation, and installation. Android and desktop differences stay there.

A picked file is identified by the SDK, not its file name: the engine reads the label and icon from the archive's resources, Android's package manager localizes and renders them, and desktop composites adaptive icons itself. The icon is kept as a file so the patch flow and the library can show it.

Patch creation is a three-step flow: Pick app -> Patches -> Run.

## Reseam SDK

The engine comes in as one Maven dependency, `app.reseam:reseam-sdk`, pinned in `gradle/libs.versions.toml` to the engine version it was built against. Android gets `libreseam-sdk-native.so` for every ABI; Linux and Windows get the same engine as a JVM resource and run it inside the app's own JVM. The Windows resource is `native/windows-x86_64/reseam_sdk_native_jni.dll`.

Manager requests automatic output for APK, APKM, and XAPK inputs. The SDK resolves the component set and returns the concrete artifact in `PatchOutcome.output`; Manager saves and installs that artifact without inferring its layout from the input filename or inspection state. The SDK owns the generated request, result, error, option, and icon types. Regenerate and rebuild native and Kotlin artifacts together when that contract changes. Manager persists only state it owns. Patch metadata is read from the installed bundle files at launch, so it always matches the running engine; a bundle the engine can no longer load is uninstalled. Navigation state carries SDK types through the SDK’s serde codecs.

To build the SDK from a local engine checkout, from `../reseam`:

```shell
cargo xtask regen all
cargo xtask runtime
cargo xtask pack-sdk
./gradlew publishToMavenLocal -PreseamSdkVersion=0.15.0
```

Alternatively, set `RESEAM_WORKSPACE` to the engine checkout when running Gradle here; the composite build substitutes the local SDK. Native artifacts must already be generated. Shared app code lives in `jvmCommonMain` because BoltFFI’s supported Kotlin backend targets JVM/Android.

### Build and Run Android Application

```shell
./gradlew :composeApp:assembleDebug
```

### Build and Run Desktop (JVM) Application

```shell
./gradlew :composeApp:run
```

Desktop patching does not require adb or an Android SDK installation. Pick an APK, APKM, or XAPK; after patching, use **Show in folder** to transfer the output to a phone. When a download source asks for a human check, Manager shows it in the system webview: WebView2 on Windows, WebKitGTK 4.1 on Linux.

### Build the Windows installer on Linux

Install the distribution's `nsis` and `unzip` packages and use Java 21.

```shell
./gradlew :composeApp:packageWindows
```

This downloads and checksums the Windows and Linux x64 Temurin JDKs pinned as `packaging-jdk` in `gradle/libs.versions.toml`, resolves Compose's `windows_x64` artifacts separately from the host desktop artifacts, links a Windows runtime from the Windows `jmods` with the Linux `jlink`, and runs `makensis`. No Wine or Windows runner is needed. Set `MAKENSIS` to a portable compiler's path when it is not on `PATH`. The installer is `composeApp/build/compose/binaries/main/windows/reseam-manager-<version>-windows-x64.exe`.

Patching requires the SDK's bundled Windows JNI library. Release staging checks for it and rejects an SDK without that resource; a development installer can still be built to validate setup and the app window while the engine's Windows artifacts are being prepared.

Setup installs for the current user under `%LOCALAPPDATA%\Programs\Reseam Manager`, creates Start menu shortcuts, offers an optional desktop shortcut, and registers an uninstaller. The launcher runs the bundled `runtime\bin\javaw.exe` with `lib\*` and `app.reseam.manager.MainKt`; no system Java is required. Close Manager before upgrading or uninstalling. An upgrade replaces the runtime and jars, while uninstalling preserves app data and signing keys.

Windows stores data and settings in `%LOCALAPPDATA%\app.reseam.manager\data`, cache files in `cache`, and launch output in `logs\manager.log` (with one previous launch retained). The launcher sets `java.io.tmpdir` to `cache\tmp` before starting the JVM; Manager clears this directory under its instance lock before loading native libraries. Linux retains its existing FileKit directories. Only one desktop instance runs per data directory; launching Manager again brings the open window to the front.

For unattended installation, run `reseam-manager-<version>-windows-x64.exe /S /D=D:\chosen\folder` (`/D=` must be last). `/NoShortcuts` suppresses shortcuts for automated validation in an isolated directory. Run `Uninstall.exe /S` to uninstall.

### Tests

```shell
./gradlew :composeApp:jvmTest
```

## Release

The app version is `managerVersion` in `gradle.properties`. Tag `vX.Y.Z` to release: the Linux CI runner builds the Android ABI APKs, Linux DEB, RPM and Arch packages, and the Windows x64 EXE installer, writes `manager.json`, and uploads everything to the CDN and Forgejo release. Installed apps check that index on launch and offer the download.

CI caches both pinned Temurin JDKs.
