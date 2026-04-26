package app.reseam.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.reseam.manager.ui.model.navigation.ManagerRoute
import app.reseam.manager.ui.platform.NoOpPermissionHandler
import app.reseam.manager.ui.platform.PermissionHandler
import app.reseam.manager.ui.screens.AppDetailScreen
import app.reseam.manager.ui.screens.BundleDetailScreen
import app.reseam.manager.ui.screens.BundlesScreen
import app.reseam.manager.ui.screens.HomeScreen
import app.reseam.manager.ui.screens.InputsScreen
import app.reseam.manager.ui.screens.PatchesScreen
import app.reseam.manager.ui.screens.PermissionsScreen
import app.reseam.manager.ui.screens.RunScreen
import app.reseam.manager.ui.screens.SettingsScreen
import app.reseam.manager.ui.theme.ReseamTheme
import app.reseam.manager.ui.viewmodel.ManagerViewModel

@Composable
fun ReseamManagerApp(
    vm: ManagerViewModel,
    permissionHandler: PermissionHandler = NoOpPermissionHandler,
) {
    val state = vm.state

    LaunchedEffect(Unit) { vm.home.load() }

    LaunchedEffect(state.settings.onboardingCompleted) {
        if (!state.settings.onboardingCompleted) {
            permissionHandler.refresh()
            if (permissionHandler.canInstallUnknownApps &&
                permissionHandler.isNotificationsEnabled &&
                permissionHandler.isBatteryOptimizationExempt
            ) {
                vm.settings.completeOnboarding()
            } else {
                vm.navigation.openPermissions()
            }
        }
    }

    ReseamTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ReseamTheme.colors.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = ReseamTheme.dimens.phoneWidth)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                ManagerRouter(vm, permissionHandler)
            }
        }
    }
}

@Composable
private fun ManagerRouter(
    vm: ManagerViewModel,
    permissionHandler: PermissionHandler,
) {
    val state = vm.state
    val route = state.route
    var copied by remember { mutableStateOf(false) }

    when (route) {
        ManagerRoute.Home -> HomeScreen(
            state = state.home,
            onNewPatch = { vm.home.startNewPatch() },
            onOpenApp = { vm.home.openPatchedApp(it) },
            onRepatch = { vm.appDetail.repatch(it) },
            onSettings = { vm.home.openSettings() },
            onBundles = { vm.home.openBundles() },
        )

        ManagerRoute.Inputs -> InputsScreen(
            state = state.flow,
            bundles = state.bundles.installed,
            onBack = { vm.navigation.back() },
            onSettings = { vm.home.openSettings() },
            onBundles = { vm.home.openBundles() },
            onSetMode = { vm.inputs.setInputMode(it) },
            onSearch = { vm.inputs.setSearchQuery(it) },
            onSelectInstalled = { vm.inputs.selectInstalledApp(it) },
            onPickFile = { /* platform file picker integration pending */ },
            onPickWebSource = { _, _ -> /* web flow pending */ },
            onContinue = { vm.inputs.continueToPatches() },
        )

        ManagerRoute.Patches -> PatchesScreen(
            state = state.flow.editor,
            appName = state.flow.editor.appName ?: state.flow.selectedInput?.displayName,
            onBack = { vm.navigation.back() },
            onTogglePatch = { name, enabled -> vm.patches.togglePatch(name, enabled) },
            onOpenOptions = { vm.patches.openOptions(it) },
            onUpdateOption = { name, key, value -> vm.patches.updateOption(name, key, value) },
            onSelectAll = { vm.patches.selectAll() },
            onResetDefaults = { vm.patches.resetDefaults() },
            onContinue = { vm.run.run() },
        )

        ManagerRoute.Run -> RunScreen(
            state = state.flow.run,
            appName = state.flow.editor.appName ?: state.flow.selectedInput?.displayName,
            appPackage = (state.flow.selectedInput as? app.reseam.manager.ui.model.PatchInput.InstalledApp)?.app?.packageName,
            patches = state.flow.editor.patches,
            onCopyLogs = { copied = true },
            copied = copied,
            onInstall = { vm.run.install() },
            onDone = { vm.navigation.openHome() },
        )

        is ManagerRoute.AppDetail -> AppDetailScreen(
            app = state.home.patchedApps.firstOrNull { it.id == route.appId },
            onBack = { vm.navigation.back() },
            onRepatch = { vm.appDetail.repatch(route.appId) },
        )

        ManagerRoute.Bundles -> BundlesScreen(
            bundles = state.bundles.installed,
            pending = state.bundles.pendingTrust,
            onBack = { vm.navigation.back() },
            onImportFromUrl = { vm.bundles.importFromUrl(it) },
            onImportFromFile = { /* platform file picker integration pending */ },
            onDecideTrust = { vm.bundles.decidePendingTrust(it) },
            onRemove = { vm.bundles.remove(it) },
            onOpen = { vm.bundleDetail.open(it) },
        )

        is ManagerRoute.BundleDetail -> BundleDetailScreen(
            detail = state.bundles.detail,
            onBack = { vm.bundleDetail.close() },
            onRemove = {
                vm.bundles.remove(it)
                vm.bundleDetail.close()
            },
        )

        ManagerRoute.Settings -> SettingsScreen(
            state = state.settings,
            onBack = { vm.navigation.back() },
            onBundles = { vm.home.openBundles() },
            onSetCheckUpdatesDaily = { vm.settings.setCheckUpdatesDaily(it) },
            onSetAnalyticsEnabled = { vm.settings.setAnalyticsEnabled(it) },
            onSetTheme = { vm.settings.setTheme(it) },
            onSetApiBaseUrl = { vm.settings.setApiBaseUrl(it) },
        )

        ManagerRoute.Permissions -> PermissionsScreen(
            canInstallUnknownApps = permissionHandler.canInstallUnknownApps,
            isNotificationsEnabled = permissionHandler.isNotificationsEnabled,
            isBatteryOptimizationExempt = permissionHandler.isBatteryOptimizationExempt,
            onRequestInstallApps = { permissionHandler.requestInstallApps() },
            onRequestNotifications = { permissionHandler.requestNotifications() },
            onRequestBatteryOptimization = { permissionHandler.requestBatteryOptimization() },
            onContinue = {
                vm.settings.completeOnboarding()
                vm.navigation.openHome()
            },
        )
    }
}
