package app.reseam.manager.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Acknowledgement
import app.reseam.manager.data.BundledAssets
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.ExternalMark
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.itemGroup
import org.jetbrains.compose.resources.stringResource

@Composable
fun LicensesScreen(viewModel: LicensesViewModel, onBack: () -> Unit) {
    val libraries by viewModel.libraries.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val open = { url: String -> uriHandler.openUri(url) }
    SettingsPage(stringResource(Res.string.licenses_title), onBack) {
        pageSection(Res.string.licenses_assets)
        acknowledgements(BundledAssets, Icons.Package, open)
        pageSection(Res.string.licenses_libraries)
        val loaded = libraries
        if (loaded == null) item { Loading() } else acknowledgements(loaded, Icons.Scale, open)
    }
}

private fun LazyListScope.acknowledgements(entries: List<Acknowledgement>, icon: ImageVector, onOpen: (String) -> Unit) {
    itemGroup {
        entries.forEach { entry ->
            item(key = entry.name) { shape ->
                val licenses = entry.licenses.joinToString().ifEmpty { stringResource(Res.string.licenses_unknown) }
                GroupedItem(
                    title = entry.name,
                    supporting = entry.version?.let { stringResource(Res.string.licenses_item, it, licenses) } ?: licenses,
                    shape = shape,
                    icon = icon,
                    onClick = entry.url?.let { url -> { onOpen(url) } },
                    trailing = entry.url?.let { { ExternalMark() } },
                )
            }
        }
    }
}
