package app.reseam.manager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 window size classes on width: compact below 600dp, expanded from 840dp. */
enum class WidthClass { Compact, Medium, Expanded }

@Immutable
data class ReseamLayout(
    val widthClass: WidthClass,
    /** Horizontal inset of every screen's content, top bar, and bottom bar. */
    val pageMargin: Dp,
    /** Space between columns of a grid or two-column body. */
    val gutter: Dp,
    /** Widest a single reading column gets before it is centered. */
    val contentMaxWidth: Dp,
    /** Widest a two-column body gets before it is centered. */
    val wideContentMaxWidth: Dp,
    /** Width of the list pane when a list and its detail share the window. */
    val listPaneWidth: Dp,
    /** Columns for card grids of the same item kind. */
    val gridColumns: Int,
) {
    val compact: Boolean get() = widthClass == WidthClass.Compact

    /** Persistent navigation beside the content instead of actions in the top bar. */
    val rail: Boolean get() = !compact

    /** A list and the selected item's detail share the window. */
    val twoPane: Boolean get() = widthClass == WidthClass.Expanded
}

fun layoutFor(windowWidth: Dp): ReseamLayout = when {
    windowWidth < 600.dp -> ReseamLayout(
        widthClass = WidthClass.Compact,
        pageMargin = 16.dp,
        gutter = 12.dp,
        contentMaxWidth = Dp.Infinity,
        wideContentMaxWidth = Dp.Infinity,
        listPaneWidth = Dp.Infinity,
        gridColumns = 1,
    )
    windowWidth < 840.dp -> ReseamLayout(
        widthClass = WidthClass.Medium,
        pageMargin = 24.dp,
        gutter = 16.dp,
        contentMaxWidth = 720.dp,
        wideContentMaxWidth = 720.dp,
        listPaneWidth = Dp.Infinity,
        gridColumns = 2,
    )
    else -> ReseamLayout(
        widthClass = WidthClass.Expanded,
        pageMargin = 32.dp,
        gutter = 20.dp,
        contentMaxWidth = 760.dp,
        wideContentMaxWidth = 1200.dp,
        listPaneWidth = 400.dp,
        gridColumns = 2,
    )
}
