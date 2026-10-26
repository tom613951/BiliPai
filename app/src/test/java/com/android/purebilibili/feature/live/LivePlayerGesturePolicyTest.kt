package com.android.purebilibili.feature.live

import kotlin.test.Test
import kotlin.test.assertTrue

class LivePlayerGesturePolicyTest {
    @Test
    fun `all live layouts keep playback and fullscreen gestures`() {
        val policy = LivePlayerGesturePolicy.Shared
        assertTrue(policy.doubleTapPlayback)
        assertTrue(policy.centerDragFullscreen)
    }
}
