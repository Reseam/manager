package app.reseam.manager.ui.components

import androidx.compose.ui.graphics.Paint

internal actual fun Paint.dithered(): Paint = apply { asFrameworkPaint().isDither = true }
