package com.android.purebilibili.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.adaptive.AppHingePaneLayout

/** 直播间的窗口布局。播放器、聊天与发送入口由调用方接入同一份房间状态。 */
@Composable
internal fun LiveRoomLayout(
    mode: LiveRoomLayoutMode,
    isFullscreen: Boolean,
    backgroundUrl: String,
    hasObstructingHinge: Boolean,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    player: @Composable () -> Unit,
    interaction: @Composable (overVideo: Boolean, includeFooter: Boolean) -> Unit,
    inputFooter: @Composable () -> Unit,
) {
    Box(modifier.background(Color.Black)) {
        if (!isFullscreen) LiveRoomBackground(backgroundUrl, Modifier.fillMaxSize())
        if (hasObstructingHinge) {
            AppHingePaneLayout(
                modifier = Modifier.fillMaxSize(),
                primaryContent = {
                    Column(Modifier.fillMaxSize()) {
                        if (!isFullscreen) header()
                        Box(Modifier.weight(1f).fillMaxWidth()) { player() }
                    }
                },
                secondaryContent = if (!isFullscreen) ({ interaction(false, true) }) else null,
            )
        } else {
            // 播放器固定在同一组合位置；横竖屏和全屏只改变测量及放置，不释放视频输出。
            val split = mode == LiveRoomLayoutMode.LandscapeSplit
            val vertical = mode == LiveRoomLayoutMode.PortraitVerticalOverlay
            val showInteraction = !isFullscreen && mode != LiveRoomLayoutMode.LandscapeOverlay
            val density = LocalDensity.current
            val safeInsets = WindowInsets.safeDrawing
            val splitLeft = if (split) safeInsets.getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr) else 0
            val splitRight = if (split) safeInsets.getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr) else 0
            val navigationBottom = WindowInsets.navigationBars.getBottom(density)
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    Box { if (!isFullscreen) header() }
                    Box(Modifier.clipToBounds()) { player() }
                    Box { if (showInteraction) interaction(vertical, !vertical) }
                    Box { if (vertical && !isFullscreen) inputFooter() }
                },
            ) { measurables, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val headerPlaceable = measurables[0].measure(
                    Constraints(maxWidth = width, maxHeight = height),
                )
                val top = headerPlaceable.height
                val availableHeight = (height - top).coerceAtLeast(0)
                val availableWidth = (width - splitLeft - splitRight).coerceAtLeast(0)
                val footerHeight = if (vertical && !isFullscreen) {
                    (LIVE_INPUT_HEIGHT_DP.dp.roundToPx() + navigationBottom).coerceAtMost(availableHeight)
                } else 0
                val chatWidth = if (split) {
                    resolveLiveSplitChatPanelWidthDp(
                        screenWidthDp = (availableWidth / density.density).toInt(),
                        screenHeightDp = (availableHeight / density.density).toInt(),
                    ).dp.roundToPx().coerceIn(0, availableWidth)
                } else width
                val playerWidth = if (split) availableWidth - chatWidth else width
                val playerHeight = when (mode) {
                    LiveRoomLayoutMode.PortraitPanel -> (width * 9 / 16).coerceAtMost(availableHeight)
                    LiveRoomLayoutMode.PortraitVerticalOverlay -> availableHeight - footerHeight
                    LiveRoomLayoutMode.LandscapeSplit -> (availableHeight - navigationBottom).coerceAtLeast(0)
                    LiveRoomLayoutMode.LandscapeOverlay -> availableHeight
                }
                val playerPlaceable = measurables[1].measure(Constraints.fixed(playerWidth, playerHeight))
                val chatHeight = when {
                    !showInteraction -> 0
                    split -> availableHeight
                    vertical -> (availableHeight * 0.32f).toInt()
                    else -> (availableHeight - playerHeight).coerceAtLeast(0)
                }
                val chatPlaceable = measurables[2].measure(Constraints.fixed(chatWidth, chatHeight))
                val footerPlaceable = measurables[3].measure(Constraints.fixed(width, footerHeight))
                layout(width, height) {
                    headerPlaceable.place(0, 0)
                    playerPlaceable.place(splitLeft, top)
                    val chatX = if (split) splitLeft + playerWidth else 0
                    val chatY = when {
                        split -> top
                        vertical -> (height - footerHeight - LIVE_CHAT_INPUT_GAP_DP.dp.roundToPx() - chatHeight).coerceAtLeast(top)
                        else -> top + playerHeight
                    }
                    chatPlaceable.place(chatX, chatY)
                    footerPlaceable.place(0, height - footerHeight)
                }
            }
        }
    }
}

@Composable
private fun LiveRoomBackground(imageUrl: String, modifier: Modifier = Modifier) {
    val fallback = painterResource(R.drawable.live_room_background)
    AsyncImage(
        model = if (imageUrl.isBlank()) R.drawable.live_room_background else imageUrl,
        placeholder = fallback,
        error = fallback,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.graphicsLayer(alpha = 0.6f),
    )
}
