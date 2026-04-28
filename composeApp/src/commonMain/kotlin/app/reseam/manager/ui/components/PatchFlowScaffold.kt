package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun PatchFlowScaffold(
    title: String,
    currentStep: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(top = 4.dp, bottom = 4.dp),
    content: LazyListScope.() -> Unit,
) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = title, onBack = onBack, actions = actions)
        RsStepper(current = currentStep, steps = PatchFlowSteps)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = contentPadding,
            content = content,
        )
        bottomBar?.invoke()
    }
}

@Composable
fun PatchFlowIntro(
    step: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier) {
        RsStepLabel(step = step)
        Text(
            text = title,
            style = ReseamTheme.typography.display,
            color = colors.foreground,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        Text(
            text = body,
            style = ReseamTheme.typography.bodySmall,
            color = colors.mutedForeground,
        )
    }
}
