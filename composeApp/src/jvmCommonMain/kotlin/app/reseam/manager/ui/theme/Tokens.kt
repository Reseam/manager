package app.reseam.manager.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val xl5 = 48.dp
}

object Layout {
    val margin = Space.lg
    val marginHome = Space.xxl
}

object Radius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 28.dp
}

object Sizes {
    val touchTarget = 48.dp
    val iconSm = 20.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val buttonMd = 40.dp
    val buttonLg = 56.dp
    val appIconSm = 40.dp
    val appIconMd = 48.dp
    val appIconLg = 56.dp
    val appIconXl = 96.dp
    val appIconHero = 104.dp
    val appIconPatching = 64.dp
    val topBar = 64.dp
    val searchBar = 56.dp
    val progressPatching = 120.dp
    val progressResult = 200.dp
    val aboutLogo = 80.dp
}

object Borders {
    val thin = 1.dp
    val thick = 2.dp
    val ring = 6.dp
    val indicator = 8.dp
}

object Opacity {
    const val disabledContent = 0.38f
    const val disabledContainer = 0.12f
    const val halo = 0.45f
}

object Motion {
    const val short4 = 200
    const val medium2 = 300
    const val long2 = 500
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}
