package app.reseam.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.reseam.manager.ui.components.RsAlertBanner
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.navigation.ManagerRoute
import app.reseam.manager.ui.platform.NoOpPermissionHandler
import app.reseam.manager.ui.platform.PermissionHandler
import app.reseam.manager.ui.platform.rememberPlatformFilePicker
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
import kotlinx.coroutines.launch

private object ReseamNavInfo : NavigationEventInfo()

@Composable
fun ReseamManagerApp(
    vm: ManagerViewModel,
    permissionHandler: PermissionHandler = NoOpPermissionHandler,
) {
    val state = vm.state

    val navState = rememberNavigationEventState(
        currentInfo = ReseamNavInfo,
        backInfo = emptyList(),
    )
    NavigationBackHandler(
        state = navState,
        isBackEnabled = state.navigation.canGoBack,
        onBackCompleted = { vm.navigation.back() },
    )

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
                state.error?.let { message ->
                    AppErrorBanner(
                        message = message,
                        onDismiss = { vm.clearError() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    RsAlertBanner(
        message = message,
        modifier = modifier,
        horizontalPadding = 12.dp,
        verticalPadding = 10.dp,
        leading = {
            Icon(
                imageVector = ReseamIcons.TriangleAlert,
                contentDescription = null,
                tint = colors.warningForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
        },
        trailing = {
            RsIconButton(onClick = onDismiss, size = 28.dp, tint = colors.mutedForeground) {
                Icon(
                    imageVector = ReseamIcons.Close,
                    contentDescription = "Dismiss error",
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
            }
        },
    )
}

@Composable
private fun ManagerRouter(
    vm: ManagerViewModel,
    permissionHandler: PermissionHandler,
) {
    val state = vm.state
    val route = state.route
    var copied by remember { mutableStateOf(false) }
    val filePicker = rememberPlatformFilePicker()
    val scope = rememberCoroutineScope()

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
            onPickFile = {
                scope.launch {
                    filePicker.pickApk()?.let { vm.inputs.selectApkFile(it.displayName, it.path) }
                }
            },
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
            onImportFromFile = {
                scope.launch {
                    filePicker.pickPatchBundle()?.let { vm.bundles.importFromFile(it.path) }
                }
            },
            onDecideTrust = { vm.bundles.decidePendingTrust(it) },
            onRemove = { vm.bundles.remove(it) },
            onOpen = { vm.bundleDetail.open(it) },
            onRefreshOfficial = { vm.bundles.refreshOfficial() },
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
