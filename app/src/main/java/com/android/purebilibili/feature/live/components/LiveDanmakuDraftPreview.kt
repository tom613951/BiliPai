package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.repository.LiveEmoticonPackage
import com.android.purebilibili.feature.live.liveDraftEmoteResources
import com.android.purebilibili.feature.live.liveDraftPreviewSegments

/** 编辑器之外的发送内容预览，不改变发送标记、选区及输入法组合范围。 */
@Composable
internal fun LiveDanmakuDraftPreview(
    text: String,
    packages: List<LiveEmoticonPackage>,
    modifier: Modifier = Modifier,
) {
    val resources = remember(packages) { liveDraftEmoteResources(packages) }
    val segments = remember(text, resources) { liveDraftPreviewSegments(text, resources) }
    if (segments.none { it.second }) return
    val content = remember(segments) {
        buildAnnotatedString {
            append("预览：")
            segments.forEach { (part, emote) ->
                if (emote) appendInlineContent(part, part) else append(part)
            }
        }
    }
    val usedResources = remember(segments, resources) {
        val keys = segments.filter { it.second }.mapTo(mutableSetOf()) { it.first }
        resources.filterKeys { it in keys }
    }
    val inlineContent = usedResources.mapValues { (key, emote) ->
            val ratio = (emote.width.coerceAtLeast(1).toFloat() / emote.height.coerceAtLeast(1)).coerceIn(0.25f, 4f)
            InlineTextContent(
                Placeholder((1.4f * ratio).em, 1.4.em, PlaceholderVerticalAlign.TextCenter),
            ) {
                var failed by remember(emote.url) { mutableStateOf(false) }
                Box {
                    if (failed) AppText(key, style = MaterialTheme.typography.bodySmall)
                    else AsyncImage(
                        model = emote.url,
                        contentDescription = key,
                        contentScale = ContentScale.Fit,
                        onError = { failed = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    AppText(
        text = content,
        inlineContent = inlineContent,
        color = AppContentColors.secondaryText(),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}
