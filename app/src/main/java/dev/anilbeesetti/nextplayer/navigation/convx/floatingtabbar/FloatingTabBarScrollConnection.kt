/*
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Port of Convx's FloatingTabBarScrollConnection for the Next Player content host.
 */
package dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Controls when Convx's floating navigation folds into its compact state. */
enum class FloatingTabBarInlineBehavior {
    Never,
    OnScrollDown,
    OnScrollUp,
    ExpandedAtTopOnly,
}

class FloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float,
    private val expandThresholdPx: Float = scrollThresholdPx,
    private val inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown,
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f
    private val scrollClockNs = longArrayOf(0L)
    private val smoothedVelocityPx = floatArrayOf(0f)

    fun scrollSpeed(): Float = abs(smoothedVelocityPx[0])

    fun isScrolling(): Boolean {
        val last = scrollClockNs[0]
        return last != 0L && System.nanoTime() - last < ScrollFreezeTailNs
    }

    private fun markScrolled() {
        scrollClockNs[0] = System.nanoTime()
    }

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    fun inline() {
        isInline = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (inlineBehavior == FloatingTabBarInlineBehavior.Never) {
            markScrolled()
            return Offset.Zero
        }

        markScrolled()
        smoothedVelocityPx[0] = smoothedVelocityPx[0] * (1f - MomentumVelocityEmaAlpha) +
            available.y * MomentumVelocityEmaAlpha

        val scrollDelta = available.y
        if ((accumulatedScroll > 0f && scrollDelta < 0f) || (accumulatedScroll < 0f && scrollDelta > 0f)) {
            accumulatedScroll = 0f
        }
        accumulatedScroll += scrollDelta

        when (inlineBehavior) {
            FloatingTabBarInlineBehavior.OnScrollDown -> {
                if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
                    isInline = true
                    accumulatedScroll = 0f
                } else if (accumulatedScroll >= expandThresholdPx && isInline) {
                    isInline = false
                    accumulatedScroll = 0f
                }
            }

            FloatingTabBarInlineBehavior.OnScrollUp -> {
                if (accumulatedScroll >= scrollThresholdPx && !isInline) {
                    isInline = true
                    accumulatedScroll = 0f
                } else if (accumulatedScroll <= -scrollThresholdPx && isInline) {
                    isInline = false
                    accumulatedScroll = 0f
                }
            }

            FloatingTabBarInlineBehavior.ExpandedAtTopOnly -> Unit
            FloatingTabBarInlineBehavior.Never -> Unit
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (inlineBehavior != FloatingTabBarInlineBehavior.ExpandedAtTopOnly) return Offset.Zero

        if (available.y > AtTopSlopPx) {
            if (isInline) {
                isInline = false
                accumulatedScroll = 0f
            }
        } else if (abs(consumed.y) > AtTopSlopPx && !isInline) {
            isInline = true
            accumulatedScroll = 0f
        }
        return Offset.Zero
    }
}

@Composable
fun rememberFloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 50.dp,
    expandThreshold: Dp = 8.dp,
    inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown,
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    val expandThresholdPx = expandThreshold.toPx()
    remember(scrollThresholdPx, expandThresholdPx, inlineBehavior, initialIsInline) {
        FloatingTabBarScrollConnection(
            initialIsInline = initialIsInline,
            scrollThresholdPx = scrollThresholdPx,
            expandThresholdPx = expandThresholdPx,
            inlineBehavior = inlineBehavior,
        )
    }
}

private const val MomentumVelocityEmaAlpha = 0.35f
private const val ScrollFreezeTailNs = 120_000_000L
private const val AtTopSlopPx = 0.5f
