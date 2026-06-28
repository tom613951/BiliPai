package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarGlassMaterialPolicyTest {

    @Test
    fun `unified material spec combines shell lens and scroll rim glow`() {
        val idle = resolveBottomBarGlassMaterialSpec(
            isDarkTheme = false,
            isScrolling = false,
            glassEnabled = true,
            motionProgress = 0f,
            pressProgress = 0f
        )
        val scrolling = resolveBottomBarGlassMaterialSpec(
            isDarkTheme = false,
            isScrolling = true,
            glassEnabled = true,
            motionProgress = 0.5f,
            pressProgress = 0.2f
        )

        assertEquals(4f, idle.blurRadiusDp)
        assertTrue(idle.vibrancy)
        assertEquals(24f, idle.shellRefractionHeightDp)
        assertEquals(24f, idle.shellRefractionAmountDp)
        assertEquals(0f, idle.shellChromaticAberration)
        assertEquals(Color.Transparent, idle.foregroundTint)
        assertEquals(BottomBarInnerRimGlowSpec(radiusDp = 5f, alpha = 0.09f), idle.innerRimGlow)
        assertEquals(null, idle.shellShader)

        assertTrue(scrolling.innerRimGlow!!.alpha > idle.innerRimGlow!!.alpha)
        assertEquals(idle.shellRefractionHeightDp, scrolling.shellRefractionHeightDp)
        assertEquals(idle.foregroundTint, scrolling.foregroundTint)
    }

    @Test
    fun `scroll progress lifts rim glow monotonically`() {
        fun specAt(scrollProgress: Float) = resolveBottomBarGlassMaterialSpec(
            isDarkTheme = false,
            isScrolling = false,
            scrollProgress = scrollProgress,
            glassEnabled = true,
            motionProgress = 0f,
            pressProgress = 0f
        )
        val idle = specAt(0f)
        val mid = specAt(0.5f)
        val full = specAt(1f)

        assertEquals(0.09f, idle.innerRimGlow!!.alpha, 0.001f)
        assertTrue(mid.innerRimGlow!!.alpha > idle.innerRimGlow!!.alpha)
        assertTrue(full.innerRimGlow!!.alpha > mid.innerRimGlow!!.alpha)
        assertEquals(0.16f, full.innerRimGlow!!.alpha, 0.001f)
    }

    @Test
    fun `scroll material exits slower than it enters`() {
        assertEquals(140, resolveBottomBarMaterialScrollAnimationDurationMillis(isScrolling = true))
        assertEquals(420, resolveBottomBarMaterialScrollAnimationDurationMillis(isScrolling = false))
    }

    @Test
    fun `container alpha uses adaptive light and dark surfaces`() {
        val light = resolveBottomBarGlassMaterialContainerColor(
            surfaceColor = Color.White,
            glassEnabled = true,
            fallbackAlpha = 1f
        )
        val dark = resolveBottomBarGlassMaterialContainerColor(
            surfaceColor = Color.Black,
            glassEnabled = true,
            fallbackAlpha = 1f
        )

        assertEquals(0.38f, light.alpha, 0.005f)
        assertEquals(0.30f, dark.alpha, 0.005f)
    }

    @Test
    fun `indicator backdrop policies are fixed defaults`() {
        assertTrue(shouldUseBottomBarCombinedIndicatorBackdrop())
        assertFalse(shouldRenderBottomBarForegroundAboveIndicator())
        assertTrue(shouldUseBottomBarIndicatorLens())
    }

    @Test
    fun `shader uniforms map center to padding plus half size`() {
        val u = resolveLiquidGlassShaderUniforms(
            widthPx = 600f,
            heightPx = 160f,
            paddingPx = 20f,
            cornerRadiusPx = 80f,
            thicknessPx = 33f,
            refractIndex = 1.5f,
            refractIntensity = 0.70f,
            intensityScale = 1f
        )

        assertEquals(20f + 300f, u.centerX, 0.001f)
        assertEquals(20f + 80f, u.centerY, 0.001f)
        assertEquals(300f, u.halfWidth, 0.001f)
        assertEquals(80f, u.halfHeight, 0.001f)
        assertEquals(640f, u.resolutionX, 0.001f)
        assertEquals(200f, u.resolutionY, 0.001f)
    }
}