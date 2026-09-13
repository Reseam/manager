This is the Kotlin Multiplatform manager for Reseam: one Compose codebase for Android and desktop.

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

The engine comes in as one Maven dependency, `app.reseam:reseam-sdk`, pinned in `gradle/libs.versions.toml` to the engine version it was built against. Android gets `libreseam-sdk-native.so` for every ABI; desktop gets the same engine as a JVM resource and runs it inside the app's own JVM.

Manager requests automatic output for APK, APKM, and XAPK inputs. The SDK resolves the component set and returns the concrete artifact in `PatchOutcome.output`; Manager saves and installs that artifact without inferring its layout from the input filename or inspection state. The SDK owns the generated request, result, error, option, and icon types. Regenerate and rebuild native and Kotlin artifacts together when that contract changes. JSON is only used for persisted state, through the SDK’s serde codecs.

To build the SDK from a local engine checkout, from `../reseam`:

```shell
cargo xtask regen all
./gradlew publishToMavenLocal -PreseamSdkVersion=0.9.0
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

### Tests

```shell
./gradlew :composeApp:jvmTest
```

## Release

The app version is `managerVersion` in `gradle.properties`. Tag `vX.Y.Z` to release: CI builds the APKs and desktop packages, writes `manager.json`, and uploads everything to the Forgejo release. Installed apps check that index on launch and offer the download.
