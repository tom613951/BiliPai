package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class PlayerEpisodeMenuUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun longPartsMenu_initiallyRevealsCurrentAndCanSelectLastThenFirstAcrossPopupHosts() {
        val nativePopup = mutableStateOf(false)
        val visible = mutableStateOf(false)
        val selected = mutableListOf<Int>()
        val pages = List(60) { index ->
            Page(cid = index.toLong() + 1, page = index + 1, part = "很长的第${index + 1}段标题，在窄菜单中最多展示两行", duration = 65)
        }
        composeTestRule.setContent {
            EpisodeMenuTheme(nativePopup.value) {
                Box(Modifier.fillMaxSize()) {
                    if (visible.value) {
                        PlayerEpisodeMenu(
                            pages = pages, currentPageIndex = 58, ugcSeason = null,
                            currentBvid = "BVparts", currentCid = 59,
                            isFullscreen = true,
                            onDismiss = { visible.value = false },
                            onPageSelect = { assertFalse(visible.value); selected.add(it) },
                            onEpisodeSelect = { error("Parts must not invoke collection callback") },
                        )
                    }
                }
            }
        }
        for (native in listOf(false, true)) {
            composeTestRule.runOnIdle { nativePopup.value = native; visible.value = true; selected.clear() }
            composeTestRule.onNodeWithText(pages[58].part).assertIsSelected().assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("跳至底部").performClick()
            composeTestRule.onNodeWithText(pages.last().part).assertIsDisplayed()
                .performTouchInput { click() }
            composeTestRule.runOnIdle { visible.value = true }
            composeTestRule.onNodeWithContentDescription("跳至顶部").performClick()
            composeTestRule.onNodeWithText(pages.first().part).assertIsDisplayed()
                .performTouchInput { click() }
            composeTestRule.runOnIdle { assertEquals(listOf(59, 0), selected) }
        }
    }

    @Test
    fun collectionMenu_opensCurrentSectionAndSectionSwitchPreservesMultipartBvidAndCid() {
        val nativePopup = mutableStateOf(false)
        val visible = mutableStateOf(false)
        val multipart = UgcEpisode(
            id = 10, aid = 100, bvid = "BVcourse", cid = 101, title = "课程",
            pages = listOf(Page(cid = 101, page = 1, part = "前半"), Page(cid = 102, page = 2, part = "后半")),
        )
        val season = UgcSeason(sections = listOf(
            UgcSection(title = "上篇", episodes = listOf(multipart)),
            UgcSection(title = "下篇", episodes = listOf(UgcEpisode(bvid = "BVcurrent", cid = 200, title = "当前播放"))),
        ))
        var selected: UgcEpisode? = null
        composeTestRule.setContent {
            EpisodeMenuTheme(nativePopup.value) {
                Box(Modifier.fillMaxSize()) {
                    if (visible.value) {
                        PlayerEpisodeMenu(
                            pages = emptyList(), currentPageIndex = 0, ugcSeason = season,
                            currentBvid = "BVcurrent", currentCid = 200,
                            isFullscreen = true,
                            onDismiss = { visible.value = false },
                            onPageSelect = { error("Collection must not invoke page callback") },
                            onEpisodeSelect = { assertFalse(visible.value); selected = it },
                        )
                    }
                }
            }
        }
        for (native in listOf(false, true)) {
            composeTestRule.runOnIdle { nativePopup.value = native; visible.value = true; selected = null }
            composeTestRule.onNodeWithText("当前播放").assertIsSelected().assertIsDisplayed()
            composeTestRule.onNodeWithText("上篇").performClick()
            composeTestRule.onNodeWithText("后半").performClick()
            composeTestRule.runOnIdle { assertEquals(multipart.copy(cid = 102), selected) }
        }
    }


    @Composable
    private fun EpisodeMenuTheme(nativePopup: Boolean, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalAppUiStyle provides AppUiStyle.MIUIX,
            LocalAppThemeConfig provides AppThemeConfig(nativeMiuixPopupsEnabled = nativePopup),
            LocalDensity provides Density(density.density, 1.4f),
        ) {
            MaterialTheme { MiuixTheme { content() } }
        }
    }
}
