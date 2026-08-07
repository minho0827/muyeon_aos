package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * 이탈시트 추천 콘텐츠 — iOS `QuoteWizardExitSheet.recommendSection` 1:1.
 *  "당장 강사가 필요하지 않다면?\n이렇게 해결해 보세요" + [전체보기] + 가로 카드(150).
 */

data class LessonContentItem(
    val id: Int,
    val title: String,
    val region: String?,
    val thumbnail: String?,
)

object LessonContentRepo {
    /**
     * 장르+지역 → 없으면 장르만 → 그래도 없으면 전체(iOS 폴백 규칙 동일). 상위 10건.
     * ⚠️ 이미지 경로는 절대/상대 혼재 — 절대(http)면 그대로, 상대면 baseURL 접두(iOS 실사고 대응).
     */
    suspend fun loadForExit(token: String?, genre: String, regionCode: String?): List<LessonContentItem> {
        var items = fetch(token, genre, regionCode)
        if (items.isEmpty() && regionCode != null) items = fetch(token, genre, null)
        if (items.isEmpty()) items = fetch(token, null, null)
        return items.take(10)
    }

    fun imageUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        return if (path.startsWith("http")) path else BuildConfig.API_BASE_URL + path
    }

    private suspend fun fetch(token: String?, genre: String?, regionCode: String?): List<LessonContentItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = StringBuilder("browse=1")
                if (!genre.isNullOrEmpty()) q.append("&genre=").append(java.net.URLEncoder.encode(genre, "UTF-8"))
                if (!regionCode.isNullOrEmpty()) q.append("&regionCode=").append(regionCode)
                val req = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}/api/lesson-products?$q")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                OkHttpClient().newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful || body.isEmpty()) return@use emptyList()
                    val arr = JSONArray(body)
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        val images = o.optJSONArray("images")
                        LessonContentItem(
                            id = o.optInt("id"),
                            title = o.optString("title"),
                            region = o.optString("region").ifEmpty { null },
                            thumbnail = if (images != null && images.length() > 0) images.optString(0) else null,
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
}

@Composable
fun ExitRecommendSection(
    lessons: List<LessonContentItem>,
    onSeeAll: () -> Unit,
    onSelectLesson: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                "당장 강사가 필요하지 않다면?\n이렇게 해결해 보세요",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = QuoteColors.c101116,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier.clickable { onSeeAll() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "전체보기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = QuoteColors.f58232,
                )
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = QuoteColors.f58232, modifier = Modifier.size(11.dp))
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            lessons.forEach { item -> RecommendCard(item, onClick = { onSelectLesson(item.id) }) }
        }
    }
}

/** 카드 — 이미지 150 정사각(크롭), 제목 14sp semibold 2줄, 지역 12sp regular. iOS recommendCard. */
@Composable
private fun RecommendCard(item: LessonContentItem, onClick: () -> Unit) {
    Column(
        Modifier.width(150.dp).clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(QuoteColors.cEAEAEA)
        ) {
            val url = LessonContentRepo.imageUrl(item.thumbnail)
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            item.title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            color = QuoteColors.c101116, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(
            item.region.orEmpty(),
            fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
            color = QuoteColors.c6D6E71, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
