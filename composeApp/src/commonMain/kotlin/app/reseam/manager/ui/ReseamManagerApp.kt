package app.reseam.manager.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.reseam.manager.ui.components.RsAlertBanner
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.AppView
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
        isBackEnabled = state.canGoBack,
        onBackCompleted = { vm.navigation.back() },
    )

    LaunchedEffect(Unit) { vm.home.load() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionHandler.refresh()
    }

    LaunchedEffect(
        state.settings.onboardingCompleted,
        permissionHandler.canInstallUnknownApps,
        permissionHandler.isNotificationsEnabled,
        permissionHandler.isBatteryOptimizationExempt,
    ) {
        if (state.settings.onboardingCompleted) return@LaunchedEffect
        if (permissionHandler.canInstallUnknownApps &&
            permissionHandler.isNotificationsEnabled &&
            permissionHandler.isBatteryOptimizationExempt
        ) {
            vm.settings.completeOnboarding()
        } else if (state.view !is AppView.Permissions) {
            vm.navigation.openPermissions()
        }
    }

    ReseamTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ReseamTheme.colors.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ReseamTheme.dimens.phoneWidth)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                val motion = ReseamTheme.motion
                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn(animationSpec = tween(motion.durationBase, easing = motion.easeOut)) +
                        slideInVertically(
                            animationSpec = tween(motion.durationBase, easing = motion.easeOut),
                            initialOffsetY = { -it },
                        ),
                    exit = fadeOut(animationSpec = tween(motion.durationFast, easing = motion.easeOut)) +
                        slideOutVertically(
                            animationSpec = tween(motion.durationFast, easing = motion.easeOut),
                            targetOffsetY = { -it },
                        ),
                ) {
                    val message = state.error ?: ""
                    AppErrorBanner(
                        message = message,
                        onDismiss = { vm.clearError() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ManagerRouter(vm, permissionHandler)
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

private fun AppView.depth(): Int = when (this) {
    AppView.Home -> 0
    AppView.Permissions -> 0
    AppView.Settings -> 1
    AppView.Bundles -> 1
    is AppView.AppDetail -> 1
    is AppView.BundleDetail -> 2
    is AppView.Flow.Choosing -> 1
    is AppView.Flow.Editing -> 2
    is AppView.Flow.Running -> 3
}

private fun AppView.routeKey(): String = when (this) {
    AppView.Home -> "home"
    AppView.Permissions -> "permissions"
    AppView.Settings -> "settings"
    AppView.Bundles -> "bundles"
    is AppView.AppDetail -> "appDetail"
    is AppView.BundleDetail -> "bundleDetail"
    is AppView.Flow.Choosing -> "flow.choosing"
    is AppView.Flow.Editing -> "flow.editing"
    is AppView.Flow.Running -> "flow.running"
}

@Composable
private fun ManagerRouter(
    vm: ManagerViewModel,
    permissionHandler: PermissionHandler,
) {
    val state = vm.state
    var copied by remember { mutableStateOf(false) }
    val filePicker = rememberPlatformFilePicker()
    val scope = rememberCoroutineScope()
    val motion = ReseamTheme.motion

    AnimatedContent(
        targetState = state.view,
        contentKey = { it.routeKey() },
        transitionSpec = {
            val forward = targetState.depth() >= initialState.depth()
            val enterDuration = motion.durationBase
            val exitDuration = motion.durationFast
            val enter = if (forward) {
                slideInHorizontally(
                    animationSpec = tween(enterDuration, easing = motion.easeOut),
                    initialOffsetX = { it / 8 },
                ) + fadeIn(animationSpec = tween(enterDuration, easing = motion.easeOut))
            } else {
                slideInHorizontally(
                    animationSpec = tween(enterDuration, easing = motion.easeOut),
                    initialOffsetX = { -it / 12 },
                ) + fadeIn(animationSpec = tween(enterDuration, easing = motion.easeOut))
            }
            val exit = if (forward) {
                slideOutHorizontally(
                    animationSpec = tween(exitDuration, easing = motion.easeOut),
                    targetOffsetX = { -it / 12 },
                ) + fadeOut(animationSpec = tween(exitDuration, easing = motion.easeOut))
            } else {
                slideOutHorizontally(
                    animationSpec = tween(exitDuration, easing = motion.easeOut),
                    targetOffsetX = { it / 8 },
                ) + fadeOut(animationSpec = tween(exitDuration, easing = motion.easeOut))
            }
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        label = "rs-route",
    ) { view ->
        when (view) {
            AppView.Home -> HomeScreen(
                state = state.home,
                setupInProgress = state.busy && state.bundles.installed.isEmpty(),
                onNewPatch = { vm.inputs.startNewPatch() },
                onOpenApp = { vm.navigation.openAppDetail(it) },
                onRepatch = { vm.inputs.repatch(it) },
                onSettings = { vm.navigation.openSettings() },
                onBundles = { vm.navigation.openBundles() },
            )

            is AppView.Flow.Choosing -> InputsScreen(
                view = view,
                installedApps = state.home.installedApps,
                bundles = state.bundles.installed,
                onBack = { vm.navigation.back() },
                onSettings = { vm.navigation.openSettings() },
                onBundles = { vm.navigation.openBundles() },
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

            is AppView.Flow.Editing -> PatchesScreen(
                state = view.editor,
                appName = view.input.displayName,
                onBack = { vm.navigation.back() },
                onTogglePatch = { name, enabled -> vm.patches.togglePatch(name, enabled) },
                onOpenOptions = { vm.patches.openOptions(it) },
                onUpdateOption = { name, key, value -> vm.patches.updateOption(name, key, value) },
                onSelectAll = { vm.patches.selectAll() },
                onResetDefaults = { vm.patches.resetDefaults() },
                onContinue = { vm.run.run() },
            )

            is AppView.Flow.Running -> RunScreen(
                state = view.run,
                appName = view.input.displayName,
                appPackage = (view.input as? app.reseam.manager.ui.model.PatchInput.InstalledApp)?.app?.packageName
                    ?: view.editor.inspect?.apk?.packageName,
                patches = view.editor.patches,
                onCopyLogs = { copied = true },
                copied = copied,
                onInstall = { vm.run.install() },
                onDone = { vm.navigation.openHome() },
            )

            is AppView.AppDetail -> AppDetailScreen(
                app = state.home.patchedApps.firstOrNull { it.id == view.appId },
                onBack = { vm.navigation.back() },
                onRepatch = { vm.inputs.repatch(view.appId) },
            )

            AppView.Bundles -> BundlesScreen(
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

            is AppView.BundleDetail -> BundleDetailScreen(
                detail = state.bundles.detail,
                onBack = { vm.bundleDetail.close() },
                onRemove = {
                    vm.bundles.remove(it)
                    vm.bundleDetail.close()
                },
            )

            AppView.Settings -> SettingsScreen(
                state = state.settings,
                onBack = { vm.navigation.back() },
                onBundles = { vm.navigation.openBundles() },
                onSetCheckUpdatesDaily = { vm.settings.setCheckUpdatesDaily(it) },
                onSetAnalyticsEnabled = { vm.settings.setAnalyticsEnabled(it) },
                onSetTheme = { vm.settings.setTheme(it) },
                onSetApiBaseUrl = { vm.settings.setApiBaseUrl(it) },
            )

            AppView.Permissions -> PermissionsScreen(
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
}
