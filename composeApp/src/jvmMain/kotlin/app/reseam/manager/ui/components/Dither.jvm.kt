package app.reseam.manager.ui.components

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint

internal actual fun Paint.dithered(): Paint = apply { skiaPaint.isDither = true }
