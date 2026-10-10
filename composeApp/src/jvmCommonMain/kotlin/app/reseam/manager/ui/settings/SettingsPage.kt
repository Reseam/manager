package app.reseam.manager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import app.reseam.manager.ui.components.BottomBarLayout
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.TopBar
import app.reseam.manager.ui.nav.LocalPane
import app.reseam.manager.ui.nav.Pane
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsPage(title: String, onBack: () -> Unit, footer: (@Composable RowScope.() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(title, onBack = onBack.takeUnless { LocalPane.current == Pane.Detail })
        if (footer == null) {
            PageContent(WindowInsets.navigationBars.asPaddingValues(), Modifier.weight(1f), content)
        } else {
            BottomBarLayout(footer, Modifier.weight(1f)) { padding -> PageContent(padding, Modifier.fillMaxSize(), content) }
        }
    }
}

@Composable
private fun PageContent(padding: PaddingValues, modifier: Modifier, content: LazyListScope.() -> Unit) {
    val direction = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = padding.calculateStartPadding(direction) + Layout.margin,
            end = padding.calculateEndPadding(direction) + Layout.margin,
            top = padding.calculateTopPadding() + Space.sm,
            bottom = padding.calculateBottomPadding() + Space.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        content = content,
    )
}

fun LazyListScope.pageSection(title: StringResource) {
    item { SectionLabel(stringResource(title), Modifier.padding(top = Space.xl)) }
}
