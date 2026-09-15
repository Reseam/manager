package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reseam.manager.ui.theme.ReseamTheme
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A bottom sheet on phones and a centered dialog everywhere wider. Content lays out the same in both. */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = ReseamTheme.colors
    if (ReseamTheme.layout.compact) {
        BottomSheet(onDismiss, content)
        return
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.padding(24.dp).widthIn(max = 480.dp).fillMaxWidth(),
            background = colors.surface,
            borderColor = colors.borderStrong,
            shape = ReseamTheme.shapes.dialog,
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(content = content)
        }
    }
}

@Composable
expect fun SheetWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit)

private enum class SheetValue { Hidden, Expanded }

@Composable
private fun BottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val scope = rememberCoroutineScope()
    val state = remember { AnchoredDraggableState(SheetValue.Hidden) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val hide: () -> Unit = { scope.launch { state.animateTo(SheetValue.Hidden, motion.tweenBase()) } }
    val fling = AnchoredDraggableDefaults.flingBehavior(state, animationSpec = motion.tweenBase())
    val scrollConnection = remember(state, fling) { SheetScrollConnection(state, fling) }

    LaunchedEffect(state) {
        snapshotFlow { state.anchors.size }.first { it > 0 }
        state.animateTo(SheetValue.Expanded, motion.tweenBase())
    }
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.dropWhile { it == SheetValue.Hidden }.first { it == SheetValue.Hidden }
        currentOnDismiss()
    }

    SheetWindow(onDismissRequest = hide) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(colors.scrim, alpha = state.scrimAlpha()) }
                    .clickable(interactionSource = null, indication = null, onClickLabel = "Close sheet", onClick = hide),
            )
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)), contentAlignment = Alignment.BottomCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                val offset = state.offset.takeUnless { it.isNaN() } ?: placeable.height.toFloat()
                                placeable.place(0, offset.roundToInt())
                            }
                        }
                        .onSizeChanged { size ->
                            state.updateAnchors(DraggableAnchors { SheetValue.Hidden at size.height.toFloat(); SheetValue.Expanded at 0f })
                        }
                        .semantics { dismiss { hide(); true } }
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Vertical,
                            flingBehavior = fling,
                        )
                        .nestedScroll(scrollConnection)
                        .clip(ReseamTheme.shapes.sheet)
                        .background(colors.surface)
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                ) {
                    DragHandle()
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun DragHandle() {
    Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 32.dp, height = 4.dp).background(ReseamTheme.colors.mutedForeground.copy(alpha = 0.4f), CircleShape))
    }
}

private fun AnchoredDraggableState<SheetValue>.scrimAlpha(): Float {
    val hidden = anchors.positionOf(SheetValue.Hidden)
    if (offset.isNaN() || hidden.isNaN() || hidden == 0f) return 0f
    val gone = (offset / hidden).coerceIn(0f, 1f)
    return 1f - gone * gone
}

private class SheetScrollConnection(
    private val state: AnchoredDraggableState<SheetValue>,
    private val fling: TargetedFlingBehavior,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0 && source == NestedScrollSource.UserInput) Offset(0f, state.dispatchRawDelta(available.y)) else Offset.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.UserInput) Offset(0f, state.dispatchRawDelta(available.y)) else Offset.Zero

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (available.y >= 0 || state.offset <= state.anchors.minPosition()) return Velocity.Zero
        settle(available.y)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        settle(available.y)
        return available
    }

    private suspend fun settle(velocity: Float) {
        state.anchoredDrag {
            val scroll = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    val from = state.requireOffset()
                    dragTo(from + pixels)
                    return state.requireOffset() - from
                }
            }
            with(fling) { scroll.performFling(velocity) }
        }
    }
}

@Composable
fun SheetHeader(title: String, body: String? = null, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = ReseamTheme.typography.title, color = colors.foreground)
        if (body != null) Text(body, style = ReseamTheme.typography.bodySmall, color = colors.mutedForeground)
    }
}
