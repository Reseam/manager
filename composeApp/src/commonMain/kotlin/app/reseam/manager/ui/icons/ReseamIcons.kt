package app.reseam.manager.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Lucide-style stroke icons. Built once per icon and cached.
// Tint is applied by the consuming Icon composable via ColorFilter.

private fun lucide(name: String, vararg paths: String, strokeWidth: Float = 1.75f): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { data ->
        builder.addPath(
            pathData = addPathNodes(data),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

object ReseamIcons {
    val ArrowLeft = lucide("ArrowLeft", "M19 12H5", "M12 19l-7-7 7-7")
    val ArrowRight = lucide("ArrowRight", "M5 12h14", "M12 5l7 7-7 7")
    val ChevronRight = lucide("ChevronRight", "M9 18l6-6-6-6")
    val ChevronDown = lucide("ChevronDown", "M6 9l6 6 6-6")
    val ChevronUp = lucide("ChevronUp", "M18 15l-6-6-6 6")
    val Check = lucide("Check", "M20 6L9 17l-5-5")
    val Close = lucide("Close", "M18 6L6 18", "M6 6l12 12")
    val Plus = lucide("Plus", "M12 5v14", "M5 12h14")
    val Search = lucide(
        "Search",
        "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z",
        "m21 21-4.35-4.35",
    )
    val Settings = lucide(
        "Settings",
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z",
    )
    val Sparkles = lucide(
        "Sparkles",
        "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
        "M20 3v4",
        "M22 5h-4",
        "M4 17v2",
        "M5 18H3",
    )
    val ShieldCheck = lucide(
        "ShieldCheck",
        "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        "m9 12 2 2 4-4",
    )
    val PackageCheck = lucide(
        "PackageCheck",
        "m16 16 2 2 4-4",
        "M21 10V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0",
        "m7.5 4.27 9 5.15",
        "M3.29 7 12 12l8.71-5",
        "M12 22V12",
    )
    val Download = lucide(
        "Download",
        "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
        "M7 10l5 5 5-5",
        "M12 15V3",
    )
    val Upload = lucide(
        "Upload",
        "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
        "M17 8l-5-5-5 5",
        "M12 3v12",
    )
    val Smartphone = lucide(
        "Smartphone",
        "M17 22H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2z",
        "M12 18h.01",
    )
    val Folder = lucide(
        "Folder",
        "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z",
    )
    val Globe = lucide(
        "Globe",
        "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z",
        "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20",
        "M2 12h20",
    )
    val Home = lucide(
        "Home",
        "m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        "M9 22V12h6v10",
    )
    val MoreVertical = lucide(
        "MoreVertical",
        "M13 12a1 1 0 1 1-2 0 1 1 0 0 1 2 0z",
        "M13 5a1 1 0 1 1-2 0 1 1 0 0 1 2 0z",
        "M13 19a1 1 0 1 1-2 0 1 1 0 0 1 2 0z",
    )
    val Info = lucide(
        "Info",
        "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z",
        "M12 16v-4",
        "M12 8h.01",
    )
    val CircleAlert = lucide(
        "CircleAlert",
        "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z",
        "M12 8v4",
        "M12 16h.01",
    )
    val TriangleAlert = lucide(
        "TriangleAlert",
        "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z",
        "M12 9v4",
        "M12 17h.01",
    )
    val Play = lucide("Play", "M6 3v18l14-9z")
    val Pause = lucide("Pause", "M6 4h4v16H6z", "M14 4h4v16h-4z")
    val Trash = lucide(
        "Trash",
        "M3 6h18",
        "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
        "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
    )
    val Refresh = lucide(
        "Refresh",
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
        "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
        "M3 21v-5h5",
    )
    val Filter = lucide("Filter", "M22 3H2l8 9.46V19l4 2v-8.54L22 3z")
    val ExternalLink = lucide(
        "ExternalLink",
        "M15 3h6v6",
        "M10 14 21 3",
        "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6",
    )
    val File = lucide(
        "File",
        "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z",
        "M14 2v4a2 2 0 0 0 2 2h4",
    )
    val Bell = lucide(
        "Bell",
        "M10.268 21a2 2 0 0 0 3.464 0",
        "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326",
    )
    val Moon = lucide("Moon", "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    val Grid = lucide(
        "Grid",
        "M10 4a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1h5a1 1 0 0 0 1-1z",
        "M21 4a1 1 0 0 0-1-1h-5a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1h5a1 1 0 0 0 1-1z",
        "M21 15a1 1 0 0 0-1-1h-5a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1h5a1 1 0 0 0 1-1z",
        "M10 15a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1h5a1 1 0 0 0 1-1z",
    )
    val Log = lucide(
        "Log",
        "M8 6h13",
        "M8 12h13",
        "M8 18h13",
        "M3 6h.01",
        "M3 12h.01",
        "M3 18h.01",
    )
    val Puzzle = lucide(
        "Puzzle",
        "M15.39 4.39a2.5 2.5 0 0 0 4.22 0A1 1 0 0 1 21 5v3a1 1 0 0 1-1 1 2.5 2.5 0 0 0 0 5 1 1 0 0 1 1 1v3a1 1 0 0 1-1.39.61 2.5 2.5 0 0 0-4.22 0A1 1 0 0 1 14 18a2.5 2.5 0 0 0-5 0 1 1 0 0 1-1.39.61A1 1 0 0 1 7 18v-3a1 1 0 0 0-1-1 2.5 2.5 0 0 1 0-5 1 1 0 0 0 1-1V5a1 1 0 0 1 1-1 2.5 2.5 0 0 0 5 0 1 1 0 0 1 1-1 1 1 0 0 1 .39.39Z",
    )
    val Lock = lucide(
        "Lock",
        "M19 22a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2z",
        "M7 11V7a5 5 0 0 1 10 0v4",
    )
    val Clock = lucide(
        "Clock",
        "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z",
        "M12 6v6l4 2",
    )
    val CheckCircle = lucide(
        "CheckCircle",
        "M22 11.08V12a10 10 0 1 1-5.93-9.14",
        "M22 4 12 14.01l-3-3",
    )
    val Loader = lucide(
        "Loader",
        "M12 2v4",
        "m16.2 7.8 2.9-2.9",
        "M18 12h4",
        "m16.2 16.2 2.9 2.9",
        "M12 18v4",
        "m4.9 19.1 2.9-2.9",
        "M2 12h4",
        "m4.9 4.9 2.9 2.9",
    )
    val GitBranch = lucide(
        "GitBranch",
        "M6 3v12",
        "M18 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
        "M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
        "M15 6a9 9 0 0 0-9 9",
    )
    val Zap = lucide(
        "Zap",
        "M13 2L3 14h9l-1 8 10-12h-9l1-8z",
    )
}
