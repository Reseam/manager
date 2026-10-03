<p align="center">
  <img src="https://reseam.app/logo.svg" alt="Reseam logo" width="96">
</p>

<h1 align="center">Reseam Manager</h1>

<p align="center">
  <a href="https://reseam.app/download/">Download</a> ·
  <a href="https://reseam.app/patches/">Patches</a> ·
  <a href="https://reseam.app">Website</a>
</p>

Reseam Manager patches Android apps. Pick an app, choose the patches you want, and Reseam Manager builds a patched copy on your device. On Android it installs it for you. On Linux and Windows it saves the file so you can move it to your phone.

Get it from [reseam.app/download](https://reseam.app/download/). The rest of this README is for people working on Reseam Manager.

## Layout

One Kotlin Multiplatform Compose codebase for Android, Linux, and Windows.

```text
composeApp/src/jvmCommonMain   shared code: UI, data, SDK calls, platform contracts
  app/reseam/manager/data      bundles, patched apps, settings, signing keys, trust
  app/reseam/manager/sdk       Reseam SDK calls and their adapters
  app/reseam/manager/ui        screens, view models, theme, routes
  app/reseam/manager/platform  contracts for HTTP, installed apps, installing
composeApp/src/androidMain     Android entry point and platform code
composeApp/src/jvmMain         desktop entry point and platform code
```

- `App.kt` holds the app shell and routes. `AppGraph` builds the repositories every screen shares.
- View models own screen state. Repositories in `data/` store state as JSON files.
- Platform code handles HTTP, file picking, app icons, and installing. Android and desktop differences stay there.
- Patching is three steps: pick an app, choose patches, run.

**Trust.** The official bundle is accepted only when it is signed by Reseam's key, which is built into the app. Any other signer has to be approved by the user once.

**Engine.** The engine comes from one Maven dependency, `app.reseam:reseam-sdk`, pinned in `gradle/libs.versions.toml`. Patch metadata is read from the installed bundle files at launch, so it always matches the running engine. A bundle the engine can no longer load is removed.

## Build

Needs Java 21.

```shell
./gradlew :composeApp:assembleDebug   # Android APK
./gradlew :composeApp:run             # desktop app
./gradlew :composeApp:jvmTest         # tests
```

To use a local engine checkout instead of the published SDK, set `RESEAM_WORKSPACE` to it. Build its native parts first, from the engine repository:

```shell
cargo xtask regen all
cargo xtask runtime
cargo xtask pack-sdk
```

### Windows installer

Builds on Linux, with no Wine or Windows machine. Install `nsis` and `unzip`, then:

```shell
./gradlew :composeApp:packageWindows
```

The installer lands in `composeApp/build/compose/binaries/main/windows/`. It bundles its own Java runtime and installs for the current user under `%LOCALAPPDATA%\Programs\Reseam Manager`. Set `MAKENSIS` if `makensis` isn't on `PATH`.

- Data, settings, and signing keys: `%LOCALAPPDATA%\app.reseam.manager\data`. Uninstalling keeps them.
- Log of the last launch: `%LOCALAPPDATA%\app.reseam.manager\logs\manager.log`.
- Silent install: `reseam-manager-<version>-windows-x64.exe /S /D=D:\folder` (`/D=` must come last). Silent uninstall: `Uninstall.exe /S`.

On desktop, a download source that asks for a human check opens in the system webview: WebView2 on Windows, WebKitGTK 4.1 on Linux.

## Release

Set `managerVersion` in `gradle.properties` and push a `vX.Y.Z` tag. CI builds the Android APKs (one per ABI), the Linux DEB, RPM, and Arch packages, and the Windows installer. It then writes `manager.json` and uploads everything. Installed copies check that index at launch and offer the update.
