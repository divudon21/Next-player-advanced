package dev.anilbeesetti.nextplayer.navigation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * A native Compose take on Convx's liquid-glass navigation: a floating translucent capsule,
 * spring-morphing selection puck, and elastic icon/press feedback. The chrome is drawn locally so
 * the navigation does not add a rendering or UI-library dependency to Next Player.
 */
@Composable
internal fun LiquidGlassNavigationBar(
    destinations: List<TopLevelDestination>,
    isSelected: (TopLevelDestination) -> Boolean,
    onDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val capsuleShape = RoundedCornerShape(percent = 50)
    val glassTop = if (isDark) Color(0xFF263342).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.72f)
    val glassBottom = if (isDark) Color(0xFF101720).copy(alpha = 0.80f) else Color(0xFFE9F2F7).copy(alpha = 0.66f)
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.38f else 0.84f),
            colorScheme.primary.copy(alpha = 0.22f),
            Color.White.copy(alpha = if (isDark) 0.12f else 0.48f),
        ),
    )

    Box(
        modifier = modifier
            .height(68.dp)
            .graphicsLayer {
                shape = capsuleShape
                shadowElevation = 18.dp.toPx()
            }
            .clip(capsuleShape)
            .background(
                brush = Brush.verticalGradient(listOf(glassTop, glassBottom)),
                shape = capsuleShape,
            )
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.20f else 0.58f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent,
                        ),
                    ),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = colorScheme.primary.copy(alpha = if (isDark) 0.10f else 0.06f),
                    topLeft = Offset(size.width * 0.68f, size.height * 0.55f),
                    size = Size(size.width * 0.38f, size.height * 0.6f),
                    cornerRadius = radius,
                )
            }
            .border(1.dp, rimBrush, capsuleShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        LookaheadScope {
            val boundsTransform = remember {
                BoundsTransform { _, _ ->
                    spring<Rect>(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                }
            }
            val selectionPuck = remember(boundsTransform, isDark, colorScheme) {
                movableContentOf<BoxScope> { scope ->
                    with(scope) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .animateBounds(
                                    lookaheadScope = this@LookaheadScope,
                                    boundsTransform = boundsTransform,
                                )
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            colorScheme.primaryContainer.copy(alpha = if (isDark) 0.96f else 0.92f),
                                            colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.88f else 0.82f),
                                        ),
                                    ),
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (isDark) 0.34f else 0.64f),
                                            Color.White.copy(alpha = 0.04f),
                                        ),
                                    ),
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val selected = isSelected(destination)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .zIndex(if (selected) 0f else 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) selectionPuck(this)
                        LiquidGlassNavigationItem(
                            destination = destination,
                            selected = selected,
                            onClick = { onDestinationClick(destination) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassNavigationItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium),
        label = "nav_press_scale",
    )
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_selected_scale",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_icon_color",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_label_color",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                indication = null,
                interactionSource = interactionSource,
            )
            .padding(horizontal = 1.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = stringResource(destination.labelRes),
            tint = iconColor,
            modifier = Modifier
                .size(21.dp)
                .graphicsLayer {
                    scaleX = selectedScale
                    scaleY = selectedScale
                },
        )
        Text(
            text = stringResource(destination.labelRes),
            color = labelColor,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
