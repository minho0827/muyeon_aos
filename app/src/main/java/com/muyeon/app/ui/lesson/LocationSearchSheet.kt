package com.muyeon.app.ui.lesson

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 장소·주소 검색 시트 — iOS `LocationSearchView.swift` 대응.
 *  검색바 → '직접입력' 행 + 검색 결과(장소명·주소) 리스트, 선택 시 onSelect(LessonPlace).
 *  끝까지 내리면 다음 페이지(15개씩, 최대 45개). 결과에 좌표가 들어 있어 다시 찾지 않는다.
 *
 * ★ 2026-10-20 플랫폼 Geocoder → 서버(/api/places/search, 카카오 로컬 키워드+주소).
 *   Geocoder 는 '주소 → 좌표' 변환이라 '사당동'을 치면 1곳만 나왔다. iOS 와 같은 서버 API 를 쓴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
    /** 편집기 텍스트필드에 치던 주소로 즉시 검색 — iOS initialQuery. */
    initialQuery: String = "",
    placeholder: String = "장소·주소 검색",
    onSelect: (LessonPlace) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ctx = LocalContext.current
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<LessonPlace>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }   // 한 번이라도 검색이 끝났는지(빈 결과 안내용)
    val scope = rememberCoroutineScope()
    val trimmed = query.trim()

    // 디바운스 300ms — iOS scheduleSearch 와 동일. query 가 바뀌면 이전 검색은 취소되고 1페이지부터.
    LaunchedEffect(trimmed) {
        if (trimmed.isEmpty()) { results = emptyList(); searched = false; hasMore = false; return@LaunchedEffect }
        delay(300)
        loading = true
        val res = searchPlaces(ctx, trimmed, 1)
        loading = false
        results = res.first; hasMore = res.second; page = 1; searched = true
    }

    // 마지막 줄이 보이면 다음 페이지 — 그사이 검색어가 바뀌었으면 버린다.
    fun loadMore() {
        if (!hasMore || loading || trimmed.isEmpty()) return
        val q = trimmed
        val next = page + 1
        loading = true
        scope.launch {
            val res = searchPlaces(ctx, q, next)
            loading = false
            if (q != query.trim()) return@launch
            val seen = results.map { "${it.name}|${it.address}" }.toSet()
            results = results + res.first.filter { "${it.name}|${it.address}" !in seen }
            hasMore = res.second; page = next
        }
    }

    fun pick(place: LessonPlace) { onSelect(place); onDismiss() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = query, onValueChange = { query = it },
                    placeholder = {
                        Text(
                            placeholder,
                            fontFamily = customFontFamily, fontSize = 16.sp, color = MuyeonColors.chevron,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Cancel, "지우기", tint = MuyeonColors.chevron,
                                modifier = Modifier.size(16.dp).clickable { query = ""; results = emptyList() },
                            )
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = customFontFamily, fontSize = 16.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F2F7),
                        unfocusedContainerColor = Color(0xFFF2F2F7),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)),
                )
            }
            HorizontalDivider(color = MuyeonColors.border)

            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                // 직접입력 — 좌표 없이 입력값 그대로(iOS 와 동일하게 항상 최상단).
                if (trimmed.isNotEmpty()) {
                    item(key = "manual") {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { pick(LessonPlace(name = trimmed)) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Search, null, tint = MuyeonColors.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                trimmed,
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                                lineHeight = 19.sp, color = MuyeonColors.textHead,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                            )
                            Text(
                                "직접입력",
                                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                                color = MuyeonColors.textSub,
                            )
                        }
                    }
                }
                items(results.size, key = { i -> "r$i-${results[i].lat}-${results[i].lng}" }) { i ->
                    val p = results[i]
                    Row(
                        Modifier.fillMaxWidth().clickable { pick(p) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Place, null, tint = MuyeonColors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                p.name,
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                                lineHeight = 19.sp, color = MuyeonColors.textHead,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            p.address?.takeIf { it.isNotEmpty() && it != p.name }?.let {
                                Text(
                                    it,
                                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                                    color = MuyeonColors.textSub,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (i == results.lastIndex) LaunchedEffect(results.size) { loadMore() }
                }
                if (loading) {
                    item(key = "loading") {
                        Text(
                            "찾는 중…", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                } else if (searched && results.isEmpty() && trimmed.isNotEmpty()) {
                    item(key = "empty") {
                        Text(
                            "검색 결과가 없어요. 동 이름이나 장소명으로 다시 찾아보세요.",
                            fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * 선택 장소 미리보기 카드 — iOS `LocationMapPreview` 대응.
 *  iOS 는 좌측에 96×72 MapKit 미니맵을 띄우지만, 안드로이드 지도 미리보기는
 *  Maps SDK + API 키가 필요해 여기선 핀 아이콘으로 대체한다(이름·주소는 동일).
 */
@Composable
fun LocationPreviewCard(place: LessonPlace, onOpenMap: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F7)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(width = 96.dp, height = 72.dp).clip(RoundedCornerShape(10.dp))
                    .background(MuyeonColors.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Place, null, tint = MuyeonColors.primary, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    place.name,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                place.address?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                        color = MuyeonColors.textSub, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0xFF05C63A))    // 네이버 그린 — iOS 와 같은 값
                    .clickable(onClick = onOpenMap)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Place, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text(
                    "네이버지도로 보기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    lineHeight = 15.sp, color = Color.White,
                )
            }
        }
    }
}

private val placeClient = OkHttpClient()

/**
 * 서버 장소·주소 검색(카카오 로컬) — (결과, 다음 페이지 있음).
 *  주소 결과는 장소명이 없어 주소를 제목으로 쓴다(iOS PlaceItem.title).
 *  실패해도 '직접입력'은 남으므로 조용히 비운다.
 */
private suspend fun searchPlaces(ctx: Context, q: String, page: Int): Pair<List<LessonPlace>, Boolean> =
    withContext(Dispatchers.IO) {
        runCatching {
            val url = (BuildConfig.API_BASE_URL + "/api/places/search").toHttpUrl().newBuilder()
                .addQueryParameter("query", q)
                .addQueryParameter("page", page.toString())
                .build()
            val token = TokenManager.getAccessToken(ctx)
            val req = Request.Builder().url(url)
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            placeClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use emptyList<LessonPlace>() to false
                val o = JSONObject(res.body?.string().orEmpty())
                val arr = o.optJSONArray("places")
                val list = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
                    val p = arr!!.getJSONObject(i)
                    val road = p.optString("roadAddress")
                    val addr = road.ifEmpty { p.optString("jibunAddress") }
                    val name = p.optString("name").ifEmpty { addr }
                    if (name.isEmpty()) null
                    else LessonPlace(name = name, address = addr.ifEmpty { null },
                        lat = p.optDouble("latitude"), lng = p.optDouble("longitude"))
                }
                list to o.optBoolean("hasMore", false)
            }
        }.getOrDefault(emptyList<LessonPlace>() to false)
    }

/** 네이버지도 — 앱(nmap 스킴) 우선, 없으면 웹 폴백. iOS openNaverMap 과 동일. */
fun openLessonNaverMap(ctx: Context, name: String, lat: Double?, lng: Double?) {
    val q = java.net.URLEncoder.encode(name, "UTF-8")
    val web = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("https://map.naver.com/v5/search/$q"),
    )
    if (lat == null || lng == null) { runCatching { ctx.startActivity(web) }; return }
    val app = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        // appname 은 패키지명과 같아야 네이버지도가 되돌아올 앱을 찾는다.
        android.net.Uri.parse("nmap://place?lat=$lat&lng=$lng&name=$q&appname=com.muyeon.app"),
    )
    runCatching { ctx.startActivity(app) }.onFailure { runCatching { ctx.startActivity(web) } }
}
