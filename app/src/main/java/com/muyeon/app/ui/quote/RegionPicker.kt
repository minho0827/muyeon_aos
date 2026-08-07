package com.muyeon.app.ui.quote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * 지역 2단계(시/도 → 시군구) 아코디언 선택 — iOS `QuoteRegionPickerView.swift` 1:1 이식.
 *  복수선택 + 시/도 '전체' 지원. 반환값은 표시명 ", " 조인 / 코드 "," 조인(iOS 동일 계약).
 *  데이터: GET /api/regions, 배지: GET /api/regions/pro-counts (실패 시 폴백/배지 생략).
 */

data class RegionDTO(val code: String, val sido: String, val sigungu: String, val name: String)
data class RegionSidoGroup(val sido: String, val items: List<RegionDTO>)

object RegionRepo {
    /** 시/도 노출 순서 — 서울 최상단(iOS sidoOrder 동일). */
    private val sidoOrder = listOf(
        "서울", "경기", "인천", "부산", "대구", "광주", "대전", "울산", "세종",
        "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
    )

    /** 백엔드 실패 시 최소 폴백 — iOS RegionService.fallback 동일. */
    val fallback = listOf(
        RegionDTO("11680", "서울", "강남구", "서울 강남구"),
        RegionDTO("11740", "서울", "강동구", "서울 강동구"),
        RegionDTO("11440", "서울", "마포구", "서울 마포구"),
        RegionDTO("41115", "경기", "수원시 팔달구", "경기 수원시 팔달구"),
        RegionDTO("41135", "경기", "성남시 분당구", "경기 성남시 분당구"),
        RegionDTO("28200", "인천", "남동구", "인천 남동구"),
        RegionDTO("26350", "부산", "해운대구", "부산 해운대구"),
    )

    /** 시/도 그룹핑 — 시/도는 sidoOrder, 시군구는 가나다순(iOS group 동일). */
    fun group(regions: List<RegionDTO>): List<RegionSidoGroup> =
        regions.groupBy { it.sido }
            .toList()
            .sortedWith(compareBy({ sidoOrder.indexOf(it.first).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it.first }))
            .map { (sido, items) -> RegionSidoGroup(sido, items.sortedBy { it.sigungu }) }

    suspend fun fetchRegions(token: String?): List<RegionDTO> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}/api/regions")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            OkHttpClient().newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful || body.isEmpty()) return@use emptyList()
                val arr = JSONArray(body)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    RegionDTO(
                        code = o.optString("code"),
                        sido = o.optString("sido"),
                        sigungu = o.optString("sigungu"),
                        name = o.optString("name"),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchProCounts(token: String?): Map<String, Int> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}/api/regions/pro-counts")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            OkHttpClient().newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful || body.isEmpty()) return@use emptyMap()
                val arr = JSONArray(body)
                (0 until arr.length()).associate { i ->
                    val o = arr.getJSONObject(i)
                    o.optString("code") to o.optInt("count")
                }
            }
        }.getOrDefault(emptyMap())
    }
}

private val cAFAFAF = Color(0xFFAFAFAF)
private val cF7F7F7 = Color(0xFFF7F7F7)

@Composable
fun QuoteRegionPicker(
    token: String?,
    onSelect: (names: String, codes: String) -> Unit,
    onClose: () -> Unit,
) {
    var groups by remember { mutableStateOf<List<RegionSidoGroup>>(emptyList()) }
    var expandedSido by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    // 선택 상태: 키(code 또는 "ALL:시도") → 표시명 (iOS selected 동일)
    val selected = remember { mutableStateMapOf<String, String>() }
    var proCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val regions = RegionRepo.fetchRegions(token)
        groups = RegionRepo.group(regions.ifEmpty { RegionRepo.fallback })
        isLoading = false
        proCounts = RegionRepo.fetchProCounts(token)
    }

    fun allKey(sido: String) = "ALL:$sido"

    Column(
        Modifier
            .fillMaxSize()
            .background(QuoteColors.white)
    ) {
        // 헤더 — 제목 18sp bold, X 18. padding h20 v16 (iOS 동일)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "지역 선택 (복수 가능)",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = QuoteColors.c101116,
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, "닫기", tint = QuoteColors.c37383B, modifier = Modifier.size(18.dp))
            }
        }
        Divider(color = QuoteColors.cEAEAEA)

        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QuoteColors.f58232)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                groups.forEach { group ->
                    val expanded = expandedSido == group.sido
                    item(key = "sido-${group.sido}") {
                        SidoRow(
                            sido = group.sido,
                            expanded = expanded,
                            selectedCount = if (selected[allKey(group.sido)] != null) 1
                            else group.items.count { selected[it.code] != null },
                            onClick = { expandedSido = if (expanded) null else group.sido },
                        )
                    }
                    if (expanded) {
                        item(key = "all-${group.sido}") {
                            val key = allKey(group.sido)
                            val isOn = selected[key] != null
                            CheckboxRow(
                                text = "${group.sido} 전체",
                                isOn = isOn,
                                emphasized = true,
                                onClick = {
                                    if (isOn) selected.remove(key)
                                    else {
                                        group.items.forEach { selected.remove(it.code) } // 전체 켜면 개별 해제
                                        selected[key] = "${group.sido} 전체"
                                    }
                                },
                            )
                        }
                        items(group.items, key = { "r-${it.code}" }) { region ->
                            val isOn = selected[region.code] != null
                            CheckboxRow(
                                text = region.sigungu,
                                isOn = isOn,
                                emphasized = false,
                                proCount = proCounts[region.code],
                                onClick = {
                                    if (isOn) selected.remove(region.code)
                                    else {
                                        selected.remove(allKey(group.sido)) // 개별 선택 시 '전체' 해제
                                        selected[region.code] = region.name
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // 하단바 — 선택 없으면 회색+비활성(iOS 동일)
            Column {
                Divider(color = QuoteColors.cEAEAEA)
                val empty = selected.isEmpty()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (empty) cAFAFAF else QuoteColors.f58232)
                        .clickable(enabled = !empty) {
                            // 선택을 groups 순서대로 정렬해 이름/코드 조립(iOS submit 동일)
                            val names = mutableListOf<String>()
                            val codes = mutableListOf<String>()
                            groups.forEach { g ->
                                if (selected[allKey(g.sido)] != null) {
                                    names.add("${g.sido} 전체")
                                } else {
                                    g.items.filter { selected[it.code] != null }.forEach {
                                        names.add(it.name); codes.add(it.code)
                                    }
                                }
                            }
                            if (names.isNotEmpty()) onSelect(names.joinToString(", "), codes.joinToString(","))
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (empty) "지역을 선택해주세요" else "선택 완료 (${selected.size})",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = QuoteColors.white,
                    )
                }
            }
        }
    }
}

/** 시/도 행 — 높이 56, 펼침 시 오렌지, 선택 수 배지(18 원). iOS sidoRow. */
@Composable
private fun SidoRow(sido: String, expanded: Boolean, selectedCount: Int, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(QuoteColors.white)
                .clickable { onClick() }
                .padding(horizontal = 20.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                sido,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                color = if (expanded) QuoteColors.f58232 else QuoteColors.c101116,
            )
            if (selectedCount > 0) {
                Box(
                    Modifier
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .clip(CircleShape)
                        .background(QuoteColors.f58232),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$selectedCount",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        color = QuoteColors.white,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = cAFAFAF,
                modifier = Modifier.size(14.dp),
            )
        }
        Divider(color = QuoteColors.cF4F4F4)
    }
}

/** 시군구/전체 행 — 높이 50, 배경 F7F7F7, leading 32 / trailing 20. iOS checkboxRow. */
@Composable
private fun CheckboxRow(
    text: String,
    isOn: Boolean,
    emphasized: Boolean,
    proCount: Int? = null,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(cF7F7F7)
                .clickable { onClick() }
                .padding(start = 32.dp, end = 20.dp)
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (isOn) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isOn) QuoteColors.f58232 else cAFAFAF,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text,
                fontFamily = customFontFamily,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 15.sp,
                color = QuoteColors.c37383B,
            )
            // 0명은 미표시(빈 지역 부정 인상 방지 — iOS 동일)
            if (proCount != null && proCount > 0) {
                Text(
                    "강사 ${proCount}명",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    color = cAFAFAF,
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Divider(color = QuoteColors.cF4F4F4)
    }
}
