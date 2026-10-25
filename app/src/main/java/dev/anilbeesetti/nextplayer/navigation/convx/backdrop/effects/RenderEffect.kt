/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package dev.anilbeesetti.nextplayer.navigation.convx.backdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.BackdropEffectScope
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.RuntimeShader
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.internal.RuntimeShaderEffect
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.internal.chain
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.isRenderEffectSupported
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
