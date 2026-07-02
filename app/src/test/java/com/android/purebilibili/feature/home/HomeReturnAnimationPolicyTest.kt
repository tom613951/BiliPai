package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeReturnAnimationPolicyTest {

    @Test
    fun quickReturn_withTransition_usesSharedElementSoftLandingSuppressionOnPhone() {
        assertEquals(
            420L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
    }

    @Test
    fun quickReturn_withTransition_usesSharedElementSoftLandingSuppressionOnTablet() {
        assertEquals(
            540L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = true,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
    }

    @Test
    fun normalReturn_usesOriginalDurations() {
        assertEquals(
            400L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            220L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = false,
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun nonSharedReturn_usesShorterSuppressionDurations() {
        assertEquals(
            240L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            220L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = true,
                cardAnimationEnabled = true,
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun contentInteractionRestore_doesNotWaitForSharedElementSuppression() {
        assertEquals(
            0L,
            resolveHomeContentInteractionRestoreDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            0L,
            resolveHomeContentInteractionRestoreDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
        assertEquals(
            0L,
            resolveHomeContentInteractionRestoreDelayMs(
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun returnBackgroundFrame_fadesHomeBlurAndScrimToClear() {
        val start = resolveHomeVideoTransitionBackgroundFrame(
            progress = 1f,
            phase = HomeVideoTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )
        val middle = resolveHomeVideoTransitionBackgroundFrame(
            progress = 0.5f,
            phase = HomeVideoTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )
        val end = resolveHomeVideoTransitionBackgroundFrame(
            progress = 0f,
            phase = HomeVideoTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )

        assertTrue(start.blurRadiusPx > middle.blurRadiusPx)
        assertTrue(middle.blurRadiusPx > end.blurRadiusPx)
        assertTrue(start.scrimAlpha > middle.scrimAlpha)
        assertTrue(middle.scrimAlpha > end.scrimAlpha)
        assertEquals(0f, end.blurRadiusPx)
        assertEquals(0f, end.scrimAlpha)
    }

    @Test
    fun openingBackgroundFrame_scalesHomeContentBehindTappedCard() {
        val start = resolveHomeVideoTransitionBackgroundFrame(
            progress = 1f,
            phase = HomeVideoTransitionBackgroundPhase.OPENING,
            sdkInt = 35
        )
        val middle = resolveHomeVideoTransitionBackgroundFrame(
            progress = 0.5f,
            phase = HomeVideoTransitionBackgroundPhase.OPENING,
            sdkInt = 35
        )
        val end = resolveHomeVideoTransitionBackgroundFrame(
            progress = 0f,
            phase = HomeVideoTransitionBackgroundPhase.OPENING,
            sdkInt = 35
        )

        assertTrue(start.contentScale <= 0.955f)
        assertTrue(start.contentScale < middle.contentScale)
        assertTrue(middle.contentScale < end.contentScale)
        assertEquals(1f, end.contentScale)
    }

    @Test
    fun returnBackgroundFrame_keepsScrimTailAfterBlurClearsWithoutScale() {
        val tail = resolveHomeVideoTransitionBackgroundFrame(
            progress = 0.16f,
            phase = HomeVideoTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )

        assertEquals(0f, tail.blurRadiusPx)
        assertTrue(tail.scrimAlpha > 0.01f)
        assertEquals(1f, tail.contentScale)
    }

    @Test
    fun returnBackgroundFrame_neverScalesHomeContent() {
        val start = resolveHomeVideoTransitionBackgroundFrame(
            progress = 1f,
            phase = HomeVideoTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )
        val middle = resolveHomeVideoTransitionBackgroundFrame(
            progress = 0.5f,
            phase = HomeVideoTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )

        assertEquals(1f, start.contentScale)
        assertEquals(1f, middle.contentScale)
    }

    @Test
    fun returnBackgroundFrame_disablesBlurBelowAndroidSButKeepsScrim() {
        val frame = resolveHomeVideoTransitionBackgroundFrame(
            progress = 1f,
            phase = HomeVideoTransitionBackgroundPhase.RETURNING,
            sdkInt = 30
        )

        assertEquals(0f, frame.blurRadiusPx)
        assertTrue(frame.scrimAlpha > 0f)
    }
}
