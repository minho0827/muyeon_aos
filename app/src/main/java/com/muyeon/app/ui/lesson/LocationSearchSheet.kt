package com.muyeon.app.ui.lesson

import android.content.Context
import android.location.Address
import android.location.Geocoder
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
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 장소 검색 시트 — iOS `LocationSearchView.swift` 대응.
 *  검색바 → '직접입력' 행 + 검색 결과(핀·주소) 리스트, 선택 시 onSelect(LessonPlace).
 *
 * ⚠️ 검색 엔진이 iOS 와 다르다 —
 *   iOS 는 MKLocalSearch(무료·키 없음)를 쓰지만 안드로이드엔 대응물이 없다.
 *   Play 서비스의 Places SDK 는 API 키 + 과금이 붙고, 네이버/카카오 로컬 API 는
 *   서버 크리덴셜이 필요하다(백엔드에 프록시 엔드포인트도 아직 없다).
 *   그래서 키 없이 쓸 수 있는 플랫폼 내장 Geocoder 로 대신한다.
 *   결과가 POI 이름 대신 주소 위주로 나오는 차이는 있으나, 반환 데이터
 *   (name·address·lat·lng)와 '직접입력' 폴백은 iOS 와 동일해 저장 형식은 1:1 이다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
    /** 편집기 텍스트필드에 치던 주소로 즉시 검색 — iOS initialQuery. */
    initialQuery: String = "",
    onSelect: (LessonPlace) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ctx = LocalContext.current
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<LessonPlace>>(emptyList()) }
    val trimmed = query.trim()

    // 디바운스 300ms — iOS scheduleSearch 와 동일. query 가 바뀌면 이전 검색은 취소된다.
    LaunchedEffect(trimmed) {
        if (trimmed.isEmpty()) { results = emptyList(); return@LaunchedEffect }
        delay(300)
        results = geocode(ctx, trimmed)
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
                            "장소·주소 검색",
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

/** 지오코딩 — 최대 15건(iOS prefix(15)와 동일). 실패해도 '직접입력'은 남으므로 조용히 비운다. */
private suspend fun geocode(ctx: Context, q: String): List<LessonPlace> = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext emptyList()
    runCatching {
        @Suppress("DEPRECATION")
        Geocoder(ctx, java.util.Locale.KOREA).getFromLocationName(q, 15).orEmpty().map { a ->
            LessonPlace(
                name = a.featureName?.takeIf { it.isNotBlank() && it != a.thoroughfare } ?: addressLine(a).ifEmpty { q },
                address = addressLine(a).ifEmpty { null },
                lat = a.latitude, lng = a.longitude,
            )
        }
    }.getOrDefault(emptyList())
}

/** 표시 주소 — iOS address(item) 과 같은 순서(시도·시군구·읍면동·도로·번지). */
private fun addressLine(a: Address): String {
    val full = a.getAddressLine(0).orEmpty()
    if (full.isNotEmpty()) return full.removePrefix("대한민국 ")
    return listOfNotNull(a.adminArea, a.locality, a.subLocality, a.thoroughfare, a.subThoroughfare)
        .joinToString(" ")
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
