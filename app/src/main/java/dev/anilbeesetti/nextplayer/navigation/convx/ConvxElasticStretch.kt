/*
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Port of Convx's elasticStretchLayerBlock used by its floating glass controls.
 */
package dev.anilbeesetti.nextplayer.navigation.convx

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.catalog.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

internal fun elasticStretchLayerBlock(
    interactiveHighlight: InteractiveHighlight,
    magnitude: Float = 1f,
): GraphicsLayerScope.() -> Unit = {
    val width = size.width
    val height = size.height

    val progress = interactiveHighlight.pressProgress
    val scale = lerp(1f, 1f + magnitude * 4f.dp.toPx() / size.height, progress)

    val maxOffset = size.minDimension
    val offset = interactiveHighlight.offset
    translationX = magnitude * maxOffset * tanh(0.05f * offset.x / maxOffset)
    translationY = magnitude * maxOffset * tanh(0.05f * offset.y / maxOffset)

    val maxDragScale = magnitude * 4f.dp.toPx() / size.height
    val offsetAngle = atan2(offset.y, offset.x)
    scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
        (width / height).fastCoerceAtMost(1f)
    scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
        (height / width).fastCoerceAtMost(1f)
}
