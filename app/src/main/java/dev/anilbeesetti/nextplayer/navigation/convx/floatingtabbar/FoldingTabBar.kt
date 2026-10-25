/*
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Port of Convx's FoldingTabBar. The video app keeps Search as a real top-level tab, so the
 * inline search-field and music-player accessory branches are omitted; the fold, detached
 * Search capsule, puck, and interaction choreography are retained.
 */
package dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.backdrops.LayerBackdrop
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.backdrops.layerBackdrop
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.backdrops.rememberLayerBackdrop
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.catalog.utils.InteractiveHighlight
import dev.anilbeesetti.nextplayer.navigation.convx.elasticStretchLayerBlock
import kotlin.math.roundToInt

@Composable
fun FoldingTabBar(
    selectedTabKey: Any?,
    scrollConnection: FloatingTabBarScrollConnection,
    rowHeight: Dp,
    tabBarContentModifier: Modifier,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    expandedTabs: @Composable (tabs: List<FoldingTab>, surfaceBackdrop: LayerBackdrop) -> Unit,
    modifier: Modifier = Modifier,
    content: FloatingTabBarScope.() -> Unit,
) {
    val scope = FloatingTabBarScopeImpl().apply(content)
    val isInline = scrollConnection.isInline
    val fold = remember { Animatable(if (isInline) 1f else 0f) }

    LaunchedEffect(isInline) {
        fold.animateTo(
            targetValue = if (isInline) 1f else 0f,
            animationSpec = if (isInline) {
                tween(CollapseMorphMs, easing = FoldCollapseSpring)
            } else {
                tween(ExpandMorphMs, easing = BarMotion.BarSurfaceExpandSpring)
            },
        )
    }

    val selected = scope.getInlineTab(selectedTabKey)
    val searchTab = scope.tabs.lastOrNull()
    val searchIsSelected = searchTab?.key == selectedTabKey
    val selectedIndex = scope.tabs.indexOf(selected).coerceAtLeast(0)
    val tabsCount = scope.tabs.size.coerceAtLeast(1)
    val surfaceBackdrop = rememberLayerBackdrop()
    val noTap = remember { MutableInteractionSource() }
    val glowScope = rememberCoroutineScope()
    val selectedGlow = remember(glowScope) { InteractiveHighlight(animationScope = glowScope) }
    val searchGlow = remember(glowScope) { InteractiveHighlight(animationScope = glowScope) }

    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            // 0: The full liquid-glass surface; its width is morphed by foldGeometry.
            Box(
                Modifier
                    .graphicsLayer(elasticStretchLayerBlock(selectedGlow))
                    .layerBackdrop(surfaceBackdrop)
                    .background(colors.backgroundColor, shapes.tabBarShape)
                    .clip(shapes.tabBarShape)
                    .then(tabBarContentModifier)
                    .then(selectedGlow.modifier),
            )

            // 1: The expanded tabs and draggable Convx selection puck.
            Box(
                Modifier
                    .graphicsLayer {
                        alpha = (1f - fold.value.coerceIn(0f, 1f) * 3f).coerceIn(0f, 1f)
                    }
                    .drawWithContent {
                        val progress = fold.value.coerceIn(0f, 1f)
                        if (progress == 0f) {
                            drawContent()
                        } else {
                            val width = lerp(size.width, size.height, progress)
                            val outline = Path().apply {
                                addRoundRect(
                                    RoundRect(0f, 0f, width, size.height, CornerRadius(size.height / 2f)),
                                )
                            }
                            clipPath(outline) { this@drawWithContent.drawContent() }
                        }
                    }
                    .then(if (isInline) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                expandedTabs(
                    scope.tabs.map { FoldingTab(it.key, it.title, it.icon, it.onClick, it.onLongClick) },
                    surfaceBackdrop,
                )
            }

            // 2: Selected tab, folded into its own capsule. Tapping it unfolds the bar.
            Box(
                Modifier
                    .graphicsLayer(elasticStretchLayerBlock(selectedGlow))
                    .graphicsLayer { alpha = fold.value.coerceIn(0f, 1f) }
                    .then(
                        if (isInline) {
                            Modifier.then(selectedGlow.gestureModifier).clickable(noTap, null) {
                                scrollConnection.expand()
                            }
                        } else {
                            Modifier.clearAndSetSemantics {}
                        },
                    ),
            ) {
                FoldingTabContent(
                    icon = { selected?.icon?.invoke() },
                    title = { selected?.title?.invoke() },
                    titleFraction = { 1f - fold.value.coerceIn(0f, 1f) },
                )
            }

            // 3: Search's independent glass capsule, visible only when detached.
            Box(
                Modifier
                    .graphicsLayer(elasticStretchLayerBlock(searchGlow))
                    .graphicsLayer {
                        alpha = if (searchIsSelected) 0f else fold.value.coerceIn(0f, 1f)
                    }
                    .background(colors.backgroundColor, shapes.standaloneTabShape)
                    .clip(shapes.standaloneTabShape)
                    .then(tabBarContentModifier)
                    .then(searchGlow.modifier),
            )

            // 4: Search control. If Search itself is selected, its selected capsule occupies this slot.
            Box(
                Modifier
                    .graphicsLayer(elasticStretchLayerBlock(searchGlow))
                    .graphicsLayer {
                        alpha = if (searchIsSelected) 0f else fold.value.coerceIn(0f, 1f)
                    }
                    .then(
                        if (isInline && !searchIsSelected) {
                            Modifier.then(searchGlow.gestureModifier).clickable(noTap, null) {
                                searchTab?.onClick?.invoke()
                            }
                        } else {
                            Modifier.clearAndSetSemantics {}
                        },
                    ),
            ) {
                FoldingTabContent(
                    icon = { searchTab?.icon?.invoke() },
                    title = { searchTab?.title?.invoke() },
                    titleFraction = { 1f - fold.value.coerceIn(0f, 1f) },
                )
            }
        },
    ) { children, constraints ->
        val width = constraints.maxWidth
        val expandedHeight = rowHeight.roundToPx()
        val compactHeight = expandedHeight
        val spacing = sizes.componentSpacing.roundToPx()
        val progress = fold.value
        val height = lerp(expandedHeight.toFloat(), compactHeight.toFloat(), progress)
            .roundToInt()
            .coerceAtLeast(1)
        val circle = height.coerceAtMost(width)
        val geometry = foldGeometry(
            width = width,
            expandedRowHeight = expandedHeight,
            collapsedRowHeight = compactHeight,
            spacing = spacing,
            playerHeight = 0,
            hasPlayer = false,
            fold = progress,
            search = 0f,
            tabsCount = tabsCount,
            selectedIndex = selectedIndex,
            inset = 4.dp.toPx(),
        )
        val surface = children[0].measure(Constraints.fixed(geometry.navWidth, height))
        val tabs = children[1].measure(Constraints.fixed(width, height))
        val selectedButton = children[2].measure(Constraints.fixed(circle, height))
        val searchSurface = children[3].measure(Constraints.fixed(geometry.searchWidth, height))
        val searchControl = children[4].measure(Constraints.fixed(circle, height))

        layout(width, geometry.height) {
            surface.placeRelative(0, geometry.rowY)
            tabs.placeRelative(0, geometry.rowY)
            selectedButton.placeRelative(geometry.selectedX, geometry.rowY)
            searchSurface.placeRelative(geometry.searchX, geometry.rowY)
            searchControl.placeRelative(geometry.searchX, geometry.rowY)
        }
    }
}

class FoldingTab(
    val key: Any,
    val title: @Composable () -> Unit,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val onLongClick: ((androidx.compose.ui.geometry.Rect) -> Unit)?,
)

private const val CollapseMorphMs = 560
private const val ExpandMorphMs = 470
private val FoldCollapseSpring = SpringEasing(zeta = 0.55f)
private val FoldingIconSize = 32.dp

@Composable
private fun FoldingTabContent(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    titleFraction: () -> Float,
) {
    Layout(
        content = {
            Box(Modifier.size(FoldingIconSize), contentAlignment = Alignment.Center) { icon() }
            Box(Modifier.graphicsLayer { alpha = titleFraction() }) { title() }
        },
    ) { children, constraints ->
        val iconSize = FoldingIconSize.roundToPx()
        val iconPlaceable = children[0].measure(Constraints.fixed(iconSize, iconSize))
        val titlePlaceable = children[1].measure(Constraints(maxWidth = constraints.maxWidth))
        val gap = 2.dp.roundToPx()
        val fullTop = (constraints.maxHeight - iconPlaceable.height - gap - titlePlaceable.height) / 2f
        val top = lerp(
            (constraints.maxHeight - iconPlaceable.height) / 2f,
            fullTop,
            titleFraction(),
        ).roundToInt()
        layout(constraints.maxWidth, constraints.maxHeight) {
            iconPlaceable.placeRelative((constraints.maxWidth - iconPlaceable.width) / 2, top)
            titlePlaceable.placeRelative(
                (constraints.maxWidth - titlePlaceable.width) / 2,
                top + iconPlaceable.height + gap,
            )
        }
    }
}
