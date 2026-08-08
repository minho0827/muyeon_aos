package com.muyeon.app.ui.chat

import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 채팅 VIDEO 메시지 — iOS `VideoMessageBubble.swift` 대응.
 *  썸네일 + 재생버튼(카톡식), 탭 → 풀스크린 재생.
 *
 *  iOS 는 AVAssetImageGenerator 로 첫 프레임을 뽑는다. Android 는 MediaMetadataRetriever
 *  (원격 URL 지원, 프록시 Range 필요)로 0.5초 프레임을 추출한다 — iOS 와 같은 시각.
 *  재생은 AVKit VideoPlayer 대신 media3 ExoPlayer.
 */
@Composable
fun ChatVideoBubble(videoPath: String) {
    val fullUrl = remember(videoPath) { QuoteUi.imageUrl(videoPath) }
    var thumb by remember(videoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var playing by remember { mutableStateOf(false) }

    LaunchedEffect(fullUrl) {
        val url = fullUrl ?: return@LaunchedEffect
        thumb = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().use { r ->
                    r.setDataSource(url, HashMap())
                    // iOS 와 동일하게 0.5초 지점(검은 첫 프레임 회피).
                    r.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            }.getOrNull()
        }
    }

    Box(
        Modifier
            .size(width = 200.dp, height = 260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE5E5EA))
            .clickable { playing = true },
        contentAlignment = Alignment.Center,
    ) {
        thumb?.let {
            androidx.compose.foundation.Image(
                bitmap = it.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, "재생", tint = Color.Black, modifier = Modifier.size(30.dp))
        }
    }

    if (playing && fullUrl != null) {
        ChatVideoPlayerDialog(fullUrl) { playing = false }
    }
}

/** 풀스크린 재생 — iOS VideoPlayerCover. */
@OptIn(UnstableApi::class)
@Composable
private fun ChatVideoPlayerDialog(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    // 다이얼로그가 사라질 때 반드시 해제 — 안 하면 오디오가 계속 재생된다.
    DisposableEffect(url) { onDispose { player.release() } }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "닫기", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
