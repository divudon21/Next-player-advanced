/*
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * App adapter for Convx's AppFloatingNavBar / FoldingTabBar / FoldingTabGroup.
 * The target app keeps its own destinations, labels, and route/back-stack state.
 */
package dev.anilbeesetti.nextplayer.navigation.convx

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.navigation.TopLevelDestination
import dev.anilbeesetti.nextplayer.navigation.TopLevelNavState
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.backdrops.LayerBackdrop
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.drawBackdrop
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.effects.blur
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.effects.colorControls
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.effects.lens
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.highlight.Highlight
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.highlight.HighlightStyle
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.isRenderEffectSupported
import dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar.FloatingTabBarDefaults
import dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar.FoldingTabBar
import dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar.FoldingTabCell
import dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar.FoldingTabGroup
import dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar.FloatingTabBarScrollConnection
import dev.anilbeesetti.nextplayer.navigation.convx.shapes.ContinuousRoundedRectangle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val NavBarShape: Shape = ContinuousRoundedRectangle(percent = 50)
private val NavBarRowHeight = 68.dp

@Composable
internal fun ConvxPrimaryNavigationBar(
    state: TopLevelNavState,
    scrollConnection: FloatingTabBarScrollConnection,
    appBackdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val selectedRoute = state.topLevelRoute
    val selectedIconColor = MaterialTheme.colorScheme.primary
    val unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val glassModifier = convxNavigationGlassModifier(appBackdrop, NavBarShape, scrollConnection)

    FoldingTabBar(
        selectedTabKey = selectedRoute,
        scrollConnection = scrollConnection,
        rowHeight = NavBarRowHeight,
        tabBarContentModifier = glassModifier,
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = Color.Transparent,
            accessoryBackgroundColor = Color.Transparent,
        ),
        shapes = FloatingTabBarDefaults.shapes(
            tabBarShape = NavBarShape,
            standaloneTabShape = NavBarShape,
        ),
        sizes = FloatingTabBarDefaults.sizes(),
        modifier = modifier,
        expandedTabs = { tabs, surfaceBackdrop ->
            val selectedIndex = tabs.indexOfFirst { it.key == selectedRoute }.coerceAtLeast(0)
            val selectedIndexState = rememberUpdatedState(selectedIndex)
            FoldingTabGroup(
                selectedTabIndex = { selectedIndexState.value },
                onTabSelected = { index ->
                    state.destinations.getOrNull(index)?.let { state.switchTo(it.route) }
                },
                backdrop = appBackdrop,
                surfaceBackdrop = surfaceBackdrop,
                tabsCount = tabs.size,
                accentColor = selectedIconColor,
                containerColor = Color.Transparent,
                height = NavBarRowHeight,
                indicatorVisible = tabs.any { it.key == selectedRoute },
            ) {
                tabs.forEach { tab ->
                    FoldingTabCell(onClick = tab.onClick, onLongClick = tab.onLongClick) {
                        tab.icon()
                        tab.title()
                    }
                }
            }
        },
    ) {
        state.destinations.forEach { destination ->
            val selected = destination.route == selectedRoute
            val tint = if (selected) selectedIconColor else unselectedIconColor
            tab(
                key = destination.route,
                title = {
                    Text(
                        text = stringResource(destination.labelRes),
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                        tint = tint,
                        modifier = Modifier.size(if (destination == TopLevelDestination.EXPLORE) 28.dp else 32.dp),
                    )
                },
                onClick = { state.switchTo(destination.route) },
            )
        }
    }
}

@Composable
private fun convxNavigationGlassModifier(
    backdrop: LayerBackdrop,
    shape: Shape,
    scrollConnection: FloatingTabBarScrollConnection,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    val baseColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF5F5F7).copy(alpha = 0.40f)
    val neutralTint = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0f) else Color(0xFF3C3C43).copy(alpha = 0.05f)
    val rimAlpha = if (isDark) 0.30f else 0.40f

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !isRenderEffectSupported()) {
        return Modifier
            .clip(shape)
            .background(baseColor)
            .background(neutralTint)
    }

    return Modifier.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = 2f)
            blur(3.dp.toPx())
            lens(
                refractionHeight = (0.60f * NavLensMaxDp).dp.toPx(),
                refractionAmount = (0.69f * NavLensMaxDp).dp.toPx(),
            )
        },
        highlight = {
            Highlight(
                width = 0.8.dp,
                style = HighlightStyle.Default(color = Color.White.copy(alpha = rimAlpha), angle = 45f),
            )
        },
        shadow = null,
        frozen = { scrollConnection.isScrolling() },
        onDrawSurface = {
            drawRect(baseColor)
            if (neutralTint.alpha > 0f) drawRect(neutralTint)
        },
        backdropScale = 0.3f,
    ).convxOuterDarkRim(shape)
}

/** Convx's two-axis black edge paired with the liquid-glass specular highlight. */
private fun Modifier.convxOuterDarkRim(shape: Shape): Modifier = drawWithCache {
    val width = 1.6.dp.toPx()
    val outline = shape.createOutline(Size(size.width, size.height), layoutDirection, this)
    val stroke = Stroke(width = width)
    val center = Offset(size.width / 2f, size.height / 2f)

    fun flankBrush(axisDeg: Float): Brush {
        val angle = axisDeg * (PI / 180f).toFloat()
        val direction = Offset(cos(angle), sin(angle))
        val reach = direction * (abs(direction.x) * size.width + abs(direction.y) * size.height) / 2f
        return Brush.linearGradient(
            0f to Color.Black.copy(alpha = 0.32f),
            0.5f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.32f),
            start = center - reach,
            end = center + reach,
        )
    }

    val firstBrush = flankBrush(45f)
    val secondBrush = flankBrush(135f)
    onDrawWithContent {
        drawContent()
        drawOutline(outline = outline, brush = firstBrush, style = stroke)
        drawOutline(outline = outline, brush = secondBrush, style = stroke)
    }
}

private const val NavLensMaxDp = 48f
