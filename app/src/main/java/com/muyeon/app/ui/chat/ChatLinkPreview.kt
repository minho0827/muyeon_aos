package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 채팅 텍스트 안의 URL 미리보기 — iOS `LinkPreviewView.swift` 대응.
 *
 *  iOS 는 LinkPresentation(LPMetadataProvider)이 OG 메타를 자동으로 가져오지만 Android 엔
 *  대응물이 없다. HTML `<meta property="og:*">` 를 직접 파싱한다(백엔드 변경 없음).
 *  jsoup 같은 파서를 새로 넣지 않고 정규식으로 필요한 3개 값만 뽑는다.
 */

/** 메시지 텍스트에서 첫 http/https URL 추출 — iOS ChatLinkDetector. */
object ChatLinkDetector {
    private val REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    fun firstUrl(text: String): String? = REGEX.find(text)?.value?.trimEnd('.', ',', ')', ']')
}

data class LinkMeta(val title: String?, val description: String?, val image: String?, val host: String)

/** URL 기준 메모리 캐시 — iOS LinkMetaCache. */
private val linkMetaCache = ConcurrentHashMap<String, LinkMeta>()

/** 실패한 URL 은 재시도하지 않는다(스크롤마다 네트워크 두들기지 않게). */
private val linkMetaFailed = ConcurrentHashMap<String, Boolean>()

private val linkClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .build()

private fun ogValue(html: String, property: String): String? {
    // property / name 양쪽 표기, content 앞뒤 순서 모두 허용.
    val patterns = listOf(
        """<meta[^>]+(?:property|name)=["']$property["'][^>]*content=["']([^"']+)["']""",
        """<meta[^>]+content=["']([^"']+)["'][^>]*(?:property|name)=["']$property["']""",
    )
    for (p in patterns) {
        Regex(p, RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)?.let { return it }
    }
    return null
}

private suspend fun fetchLinkMeta(url: String): LinkMeta? = withContext(Dispatchers.IO) {
    runCatching {
        val req = Request.Builder().url(url)
            // 봇 차단 사이트가 많아 일반 브라우저 UA 로 요청.
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            .build()
        linkClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@use null
            // HTML head 만 필요 — 전체를 읽지 않고 앞부분만(대형 페이지 방어).
            val html = res.body?.source()?.let { src ->
                src.request(200_000)
                src.buffer.snapshot().utf8().take(200_000)
            } ?: return@use null
            val host = runCatching { java.net.URI(url).host }.getOrNull().orEmpty()
            LinkMeta(
                title = ogValue(html, "og:title")
                    ?: Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1),
                description = ogValue(html, "og:description"),
                image = ogValue(html, "og:image"),
                host = host,
            )
        }
    }.getOrNull()
}

/** 미리보기 카드 — 메타 실패 시 도메인 칩으로 폴백(iOS fallbackChip 동일). */
@Composable
fun LinkPreviewCard(url: String, onOpen: (String) -> Unit) {
    var meta by remember(url) { mutableStateOf(linkMetaCache[url]) }
    var failed by remember(url) { mutableStateOf(linkMetaFailed[url] == true) }

    LaunchedEffect(url) {
        if (meta != null || failed) return@LaunchedEffect
        val m = fetchLinkMeta(url)
        if (m?.title != null) {
            linkMetaCache[url] = m
            meta = m
        } else {
            linkMetaFailed[url] = true
            failed = true
        }
    }

    val m = meta
    if (m != null) {
        Column(
            Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF2F2F7))
                .clickable { onOpen(url) },
        ) {
            m.image?.let { img ->
                AsyncImage(
                    img, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                )
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    m.title.orEmpty(),
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textHead,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                m.description?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                        color = MuyeonColors.textSub, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    m.host,
                    fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp,
                    color = MuyeonColors.secondary, maxLines = 1,
                )
            }
        }
    } else if (failed) {
        val host = remember(url) { runCatching { java.net.URI(url).host }.getOrNull().orEmpty() }
        Text(
            host.ifEmpty { url },
            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp, color = MuyeonColors.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MuyeonColors.primary.copy(alpha = 0.08f))
                .clickable { onOpen(url) }
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
