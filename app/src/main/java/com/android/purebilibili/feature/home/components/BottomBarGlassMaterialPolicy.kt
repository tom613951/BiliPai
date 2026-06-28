package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.util.lerp

internal data class BottomBarInnerRimGlowSpec(
    val radiusDp: Float,
    val alpha: Float
)

internal data class BottomBarShellShaderSpec(
    val thicknessDp: Float,
    val refractIndex: Float,
    val refractIntensity: Float
)

internal data class BottomBarGlassMaterialSpec(
    val blurRadiusDp: Float?,
    val vibrancy: Boolean,
    val shellRefractionHeightDp: Float,
    val shellRefractionAmountDp: Float,
    val shellChromaticAberration: Float,
    val foregroundTint: Color,
    val highlightWidthScale: Float,
    val shadowAlphaScale: Float,
    val innerRimGlow: BottomBarInnerRimGlowSpec?,
    val shellShader: BottomBarShellShaderSpec?
)

internal fun resolveBottomBarGlassMaterialSpec(
    isDarkTheme: Boolean,
    isScrolling: Boolean,
    scrollProgress: Float = if (isScrolling) 1f else 0f,
    glassEnabled: Boolean,
    motionProgress: Float,
    pressProgress: Float
): BottomBarGlassMaterialSpec {
    if (!glassEnabled) {
        return BottomBarGlassMaterialSpec(
            blurRadiusDp = null,
            vibrancy = false,
            shellRefractionHeightDp = 0f,
            shellRefractionAmountDp = 0f,
            shellChromaticAberration = 0f,
            foregroundTint = Color.Transparent,
            highlightWidthScale = 1f,
            shadowAlphaScale = 1f,
            innerRimGlow = null,
            shellShader = null
        )
    }
    val scrollLift = scrollProgress.coerceIn(0f, 1f)
    return BottomBarGlassMaterialSpec(
        blurRadiusDp = 4f,
        vibrancy = true,
        shellRefractionHeightDp = 24f,
        shellRefractionAmountDp = 24f,
        shellChromaticAberration = 0f,
        foregroundTint = Color.Transparent,
        highlightWidthScale = 1f,
        shadowAlphaScale = 1f,
        innerRimGlow = BottomBarInnerRimGlowSpec(
            radiusDp = 5f,
            alpha = lerp(0.09f, 0.16f, scrollLift)
        ),
        shellShader = null
    )
}

internal fun resolveBottomBarGlassMaterialContainerColor(
    surfaceColor: Color,
    glassEnabled: Boolean,
    fallbackAlpha: Float
): Color {
    if (!glassEnabled) return surfaceColor.copy(alpha = fallbackAlpha)
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val alpha = if (isDarkSurface) 0.30f else 0.38f
    return surfaceColor.copy(alpha = alpha)
}

internal fun resolveBottomBarMaterialScrollAnimationDurationMillis(
    isScrolling: Boolean
): Int = if (isScrolling) 140 else 420

internal data class LiquidGlassShaderUniforms(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadiusPx: Float,
    val thicknessPx: Float,
    val refractIndex: Float,
    val refractIntensity: Float,
    val resolutionX: Float,
    val resolutionY: Float
)

internal fun resolveLiquidGlassShaderUniforms(
    widthPx: Float,
    heightPx: Float,
    paddingPx: Float,
    cornerRadiusPx: Float,
    thicknessPx: Float,
    refractIndex: Float,
    refractIntensity: Float,
    intensityScale: Float
): LiquidGlassShaderUniforms {
    val halfWidth = widthPx / 2f
    val halfHeight = heightPx / 2f
    val maxRadius = minOf(halfWidth, halfHeight)
    return LiquidGlassShaderUniforms(
        centerX = paddingPx + halfWidth,
        centerY = paddingPx + halfHeight,
        halfWidth = halfWidth,
        halfHeight = halfHeight,
        cornerRadiusPx = cornerRadiusPx.coerceIn(0f, maxRadius),
        thicknessPx = thicknessPx,
        refractIndex = refractIndex,
        refractIntensity = (refractIntensity * intensityScale).coerceAtLeast(0f),
        resolutionX = widthPx + paddingPx * 2f,
        resolutionY = heightPx + paddingPx * 2f
    )
}