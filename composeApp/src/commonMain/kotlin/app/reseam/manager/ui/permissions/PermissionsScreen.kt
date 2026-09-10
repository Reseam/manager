package app.reseam.manager.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.reseam.manager.platform.Permission
import app.reseam.manager.platform.Permissions
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.CardPadding
import app.reseam.manager.ui.components.IconTile
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.theme.ReseamTheme

private data class PermissionCopy(val title: String, val description: String, val icon: ImageVector)

private val Copy = mapOf(
    Permission.InstallApps to PermissionCopy("Install apps", "Required to install patched APKs", Icons.ShieldCheck),
    Permission.Notifications to PermissionCopy("Notifications", "Lets Reseam report when patching finishes", Icons.Bell),
    Permission.BatteryOptimization to PermissionCopy("Battery optimization", "Stops the system killing Reseam mid-patch", Icons.Zap),
)

/** [onBack] is null on the first-run gate, where the only way forward is the install grant. */
@Composable
fun PermissionsScreen(permissions: Permissions, onBack: (() -> Unit)?, onContinue: () -> Unit) {
    val colors = ReseamTheme.colors
    val canInstall = Permission.InstallApps in permissions.granted
    Screen(
        title = if (onBack == null) "Welcome" else "Permissions",
        onBack = onBack,
        bottomBar = {
            BottomBar { fill ->
                Button(onClick = onContinue, modifier = fill, size = ButtonSize.Large, enabled = onBack != null || canInstall) {
                    Text(if (onBack == null) "Get started" else "Done")
                }
            }
        },
    ) {
        item {
            Text(
                text = "Reseam needs a few system grants to patch and install apps.",
                style = ReseamTheme.typography.bodySmall,
                color = colors.mutedForeground,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        items(Permission.entries) { permission ->
            PermissionCard(
                copy = Copy.getValue(permission),
                granted = permission in permissions.granted,
                onGrant = { permissions.request(permission) },
            )
        }
        if (!canInstall) {
            item { Banner("Reseam cannot install patched apps without the install grant.", modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun PermissionCard(copy: PermissionCopy, granted: Boolean, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (granted) colors.primaryHairline else colors.border,
        contentPadding = CardPadding,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconTile(
                icon = copy.icon,
                background = if (granted) colors.primaryFaint else colors.mutedElevated,
                tint = if (granted) colors.primary else colors.mutedForeground,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(copy.title, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
                Text(copy.description, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
            }
            if (granted) {
                Box(Modifier.size(36.dp).background(colors.primaryFaint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Check, "Granted", tint = colors.primary, modifier = Modifier.size(20.dp))
                }
            } else {
                Button(onClick = onGrant, size = ButtonSize.Small) { Text("Grant") }
            }
        }
    }
}
