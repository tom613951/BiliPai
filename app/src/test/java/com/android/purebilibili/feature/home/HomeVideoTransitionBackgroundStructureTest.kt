package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HomeVideoTransitionBackgroundStructureTest {

    @Test
    fun homeRootOwnsVideoTransitionBackgroundBlurAndScrim() {
        val source = homeScreenSource()
        val driverBlock = source
            .substringAfter("val homeVideoTransitionBackgroundProgress = remember")
            .substringBefore("// Navigation 返回不一定触发首页 Lifecycle.ON_START")
        val rootBlock = source
            .substringAfter("// 指示器位置逻辑也移入 graphicsLayer")
            .substringBefore("val video = pendingNotInterestedVideo")

        assertTrue(driverBlock.contains("isOpeningVideoDetailBackgroundTransition"))
        assertTrue(driverBlock.contains("!isReturningFromVideoDetail"))
        assertTrue(driverBlock.contains("LaunchedEffect(\n        isOpeningVideoDetailBackgroundTransition"))
        assertTrue(driverBlock.contains("hideTopTabsForForwardDetailNav"))
        assertTrue(driverBlock.contains("isReturningFromVideoDetail"))
        assertTrue(driverBlock.contains("homeVideoTransitionBackgroundPhase = HomeVideoTransitionBackgroundPhase.RETURNING"))
        assertTrue(driverBlock.contains("homeVideoTransitionBackgroundPhase = HomeVideoTransitionBackgroundPhase.OPENING"))
        assertTrue(driverBlock.contains("homeVideoTransitionBackgroundProgress.snapTo(1f)"))
        assertTrue(driverBlock.contains("targetValue = HOME_VIDEO_TRANSITION_BACKGROUND_RETURN_SETTLE_PROGRESS"))
        assertTrue(rootBlock.contains(".homeVideoTransitionBackgroundEffect"))
        assertTrue(rootBlock.contains("homeVideoTransitionBackgroundProgress.value"))
        assertTrue(rootBlock.contains("homeVideoTransitionBackgroundPhase"))
    }

    @Test
    fun homeRootAppliesDirectionalBackgroundContentScaleInGraphicsLayer() {
        val source = homeScreenSource()
        val effectBlock = source
            .substringAfter("private fun Modifier.homeVideoTransitionBackgroundEffect")
            .substringBefore("private fun Modifier.homeFeedTopVideoFadeMask")

        assertTrue(effectBlock.contains("phaseProvider: () -> HomeVideoTransitionBackgroundPhase"))
        assertTrue(effectBlock.contains("phaseProvider()"))
        assertTrue(effectBlock.contains("scaleX = frame.contentScale"))
        assertTrue(effectBlock.contains("scaleY = frame.contentScale"))
    }

    private fun homeScreenSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        ).first { it.exists() }.readText()
    }
}
