package app.reseam.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsAlertBanner
import app.reseam.manager.ui.components.RsBottomBar
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsIconTile
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun PermissionsScreen(
    canInstallUnknownApps: Boolean,
    isNotificationsEnabled: Boolean,
    isBatteryOptimizationExempt: Boolean,
    onRequestInstallApps: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val allGranted = canInstallUnknownApps && isNotificationsEnabled && isBatteryOptimizationExempt

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = "Welcome")
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        ) {
            item {
                Text(
                    text = "Reseam needs a few permissions to patch and install apps. Grant them to get started.",
                    style = ReseamTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            item {
                PermissionItem(
                    icon = ReseamIcons.ShieldCheck,
                    title = "Install apps",
                    description = "Required to install patched APKs",
                    isGranted = canInstallUnknownApps,
                    onRequest = onRequestInstallApps,
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                PermissionItem(
                    icon = ReseamIcons.Bell,
                    title = "Notifications",
                    description = "Get notified when patching completes",
                    isGranted = isNotificationsEnabled,
                    onRequest = onRequestNotifications,
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                PermissionItem(
                    icon = ReseamIcons.Zap,
                    title = "Battery optimization",
                    description = "Prevent the system from killing Reseam while patching",
                    isGranted = isBatteryOptimizationExempt,
                    onRequest = onRequestBatteryOptimization,
                )
            }
            item {
                Spacer(Modifier.height(20.dp))
                if (!canInstallUnknownApps) {
                    RsAlertBanner(
                        message = "Install apps permission is required to use Reseam.",
                    ) {
                        Icon(
                            imageVector = ReseamIcons.TriangleAlert,
                            contentDescription = null,
                            tint = colors.warningForeground,
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                        )
                    }
                }
            }
        }
        RsBottomBar {
            RsButton(
                onClick = onContinue,
                size = RsButtonSize.Large,
                fullWidth = true,
                enabled = allGranted,
            ) {
                Text("Get started")
                if (allGranted) {
                    Icon(
                        imageVector = ReseamIcons.ArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isGranted) colors.primaryHairline else colors.divider,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RsIconTile(
                icon = icon,
                size = 44.dp,
                background = if (isGranted) colors.primaryFaint else colors.mutedElevated,
                tint = if (isGranted) colors.primary else colors.mutedForeground,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.foreground,
                )
                Text(
                    text = description,
                    style = ReseamTheme.typography.captionSmall,
                    color = colors.mutedForeground,
                )
            }
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primaryFaint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ReseamIcons.Check,
                        contentDescription = "Granted",
                        tint = colors.primary,
                        modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                    )
                }
            } else {
                RsButton(onClick = onRequest, size = RsButtonSize.Small) {
                    Text("Grant")
                }
            }
        }
    }
}
