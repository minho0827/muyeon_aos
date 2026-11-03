package com.muyeon.app.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.lesson.LessonPlace
import com.muyeon.app.ui.lesson.LocationPreviewCard
import com.muyeon.app.ui.lesson.openLessonNaverMap
import com.muyeon.app.ui.quote.QuoteUi
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 거래·대관 화면 공용 조각(2026-11-03 무용센터 양도양수 · 공간 대관) — iOS 와 같은 모양.
 *  새 색 토큰을 만들지 않는다: 주황(MuyeonColors.primary)·회색 계단(MuyeonList)과
 *  웹 DmIcon 버튼의 연주황 원(#FDEBDD, 강사 탭 카드 '바로 견적요청' 버튼과 같은 값)만 쓴다.
 */
object MarketFormat {
    fun num(v: Int): String = String.format(Locale.KOREA, "%,d", v)

    /** 만원 단위 금액 — null 은 "협의". */
    fun manwon(v: Int?): String = v?.let { num(it) } ?: "협의"

    /** 원 단위 금액 "7,000원". */
    fun won(v: Int?): String? = v?.let { "${num(it)}원" }

    fun imageUrl(path: String?): String? = QuoteUi.imageUrl(path)
}

/** 웹 DmIcon 과 같은 연주황 원 — #FDEBDD (LessonTab 견적요청 버튼 배경). */
val DmButtonTint = Color(0xFFFDEBDD)

private fun ts(weight: FontWeight, size: Int) = TextStyle(fontFamily = customFontFamily, fontWeight = weight, fontSize = size.sp)

/** 인스타 DM풍 종이비행기 — 웹 components/muyeon/DmIcon.js 의 SVG path 그대로(24×24, stroke 1.8). */
@Composable
fun DmPlaneIcon(size: Dp = 18.dp, color: Color = MuyeonColors.primary) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val stroke = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // M22 3 L2 10.5 l7 3.5 / M22 3 l-4 18 -7 -7 / M22 3 L9 14
        drawPath(Path().apply { moveTo(22f * u, 3f * u); lineTo(2f * u, 10.5f * u); lineTo(9f * u, 14f * u) }, color, style = stroke)
        drawPath(Path().apply { moveTo(22f * u, 3f * u); lineTo(18f * u, 21f * u); lineTo(11f * u, 14f * u) }, color, style = stroke)
        drawPath(Path().apply { moveTo(22f * u, 3f * u); lineTo(9f * u, 14f * u) }, color, style = stroke)
    }
}

/** 카드의 '바로 채팅' 원형 버튼 — 강사 탭 카드 DM 버튼과 같은 모양(36dp 원 + 18dp 비행기). */
@Composable
fun DmPlaneButton(label: String = "바로 채팅", onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(DmButtonTint)
            .clickable(onClick = onClick).semantics { contentDescription = label },
        Alignment.Center,
    ) { DmPlaneIcon() }
}

/** 하트(찜) — 켜지면 주황 채움. */
@Composable
fun MarketHeart(on: Boolean, size: Dp = 22.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size + 14.dp).clip(CircleShape).clickable(onClick = onClick)
            .semantics { contentDescription = if (on) "찜 해제" else "찜하기" },
        Alignment.Center,
    ) {
        Icon(
            if (on) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null,
            tint = if (on) MuyeonColors.primary else MuyeonColors.hint, modifier = Modifier.size(size),
        )
    }
}

/** 작은 상태 배지(예약중·거래완료·결제 대기 등). */
@Composable
fun MarketBadge(text: String, fg: Color = Color.White, bg: Color = MuyeonColors.primary) {
    Text(
        text, style = ts(FontWeight.SemiBold, 12), color = fg,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** 목록 위 필터 알약 — 값이 걸려 있으면 검정 테두리·굵게. */
@Composable
fun MarketFilterPill(text: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(34.dp).clip(CircleShape)
            .border(1.dp, if (active) MuyeonColors.textHead else MuyeonList.ink200, CircleShape)
            .background(Color.White).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text, style = ts(if (active) FontWeight.SemiBold else FontWeight.Medium, 14), color = MuyeonColors.textHead, maxLines = 1)
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.textSub, modifier = Modifier.size(16.dp))
    }
}

/** 사진 갤러리 — 좌우 넘김 + "1 / N", 탭하면 전체 화면(ImageViewerActivity). 사진이 없으면 기본 이미지. */
@Composable
fun MarketGallery(images: List<String>, height: Dp = 280.dp, placeholderRes: Int? = null) {
    val ctx = LocalContext.current
    Box(Modifier.fillMaxWidth().height(height).background(MuyeonList.ink100), Alignment.Center) {
        if (images.isEmpty()) {
            // ★ 2026-11-03 기능별 기본 썸네일(양도양수 = 건물 그림)을 주면 칸을 꽉 채운다
            if (placeholderRes != null) Image(painterResource(placeholderRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Image(painterResource(R.drawable.icon_no_image), null, Modifier.size(96.dp), contentScale = ContentScale.Fit)
        } else {
            val pager = rememberPagerState { images.size }
            HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                AsyncImage(
                    MarketFormat.imageUrl(images[i]), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable {
                        ImageViewerActivity.start(ctx, JSONArray(images).toString(), pager.currentPage, false)
                    },
                )
            }
            if (images.size > 1) {
                Text(
                    "${pager.currentPage + 1} / ${images.size}", style = ts(FontWeight.Medium, 12), color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** 카드 썸네일 — 없으면 기본 이미지. */
@Composable
fun MarketThumb(path: String?, modifier: Modifier, placeholderRes: Int? = null) {
    Box(modifier.background(MuyeonList.ink100), Alignment.Center) {
        val url = MarketFormat.imageUrl(path)
        if (url == null && placeholderRes != null) Image(painterResource(placeholderRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (url == null) Image(painterResource(R.drawable.icon_no_image), null, Modifier.size(40.dp), contentScale = ContentScale.Fit)
        else AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** 정보 표 — 라벨(회색, 고정폭) | 값. 값이 빈 줄은 호출부에서 뺀다. */
@Composable
fun MarketInfoTable(rows: List<Pair<String, String>>, labelWidth: Dp = 96.dp) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, MuyeonList.ink200, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        rows.forEachIndexed { i, (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(k, style = ts(FontWeight.Medium, 14), color = MuyeonColors.textSub, modifier = Modifier.width(labelWidth))
                Text(v, style = ts(FontWeight.Medium, 14), color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
            }
            if (i < rows.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonList.ink100))
        }
    }
}

/** 섹션 제목(굵게 18). */
@Composable
fun MarketSectionTitle(text: String) {
    Text(text, style = ts(FontWeight.Bold, 18), color = MuyeonColors.textHead)
}

/**
 * 고정 하단 바 — 하트 · (전화) · 주 버튼(StageBottomButton).
 *  liked == null 이면 하트를 그리지 않는다. phone 이 null 이면 전화 버튼 숨김.
 */
@Composable
fun MarketBottomBar(
    liked: Boolean?,
    onLike: () -> Unit,
    phone: String?,
    primary: String,
    busy: Boolean = false,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    onPrimary: () -> Unit,
) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (liked != null) MarketHeart(liked, 26.dp, onLike)
            if (!phone.isNullOrBlank()) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
                        .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))) } }
                        .semantics { contentDescription = "전화하기" },
                    Alignment.Center,
                ) { Icon(Icons.Filled.Call, null, tint = MuyeonColors.textHead, modifier = Modifier.size(22.dp)) }
            }
            if (secondary != null && onSecondary != null) {
                Box(
                    Modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSecondary).padding(horizontal = 18.dp),
                    Alignment.Center,
                ) { Text(secondary, style = ts(FontWeight.Bold, 16), color = MuyeonColors.textHead) }
            }
            Box(Modifier.weight(1f)) { StageBottomButton(primary, busy = busy, onClick = onPrimary) }
        }
    }
}

/** 두 버튼 하단 바(글쓴이·호스트용 [수정] [상태 변경] 등). */
@Composable
fun MarketOwnerBar(left: String, onLeft: () -> Unit, right: String, onRight: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
                    .clickable(onClick = onLeft),
                Alignment.Center,
            ) { Text(left, style = ts(FontWeight.Bold, 17), color = MuyeonColors.textHead) }
            Box(Modifier.weight(1f)) { StageBottomButton(right, onClick = onRight) }
        }
    }
}

/**
 * 위치 카드 — 레슨 장소 미리보기(LocationPreviewCard + 네이버 미니 지도) 재사용.
 *  카드를 탭하면 길찾기 앱 선택(카카오맵·네이버지도·구글 지도). approxNote 가 있으면 카드 아래 안내.
 */
@Composable
fun MarketMapCard(name: String, address: String?, lat: Double?, lng: Double?, approxNote: String? = null) {
    val ctx = LocalContext.current
    var choose by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.clickable { choose = true }) {
            LocationPreviewCard(LessonPlace(name = name, address = address, lat = lat, lng = lng)) { choose = true }
        }
        approxNote?.let { Text(it, style = ts(FontWeight.Medium, 13), color = MuyeonColors.textSub) }
    }
    if (choose) {
        MuyeonDialog(
            title = "길찾기 앱 선택",
            actions = listOf(
                MuyeonDialogAction("카카오맵", MuyeonDialogKind.CANCEL) { openDirections(ctx, "kakao", name, lat, lng) },
                MuyeonDialogAction("네이버지도", MuyeonDialogKind.CANCEL) { openDirections(ctx, "naver", name, lat, lng) },
                MuyeonDialogAction("구글 지도", MuyeonDialogKind.CANCEL) { openDirections(ctx, "google", name, lat, lng) },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { choose = false },
        )
    }
}

/** 길찾기 — 앱이 없으면 웹 지도로. */
fun openDirections(ctx: Context, app: String, name: String, lat: Double?, lng: Double?) {
    val q = Uri.encode(name)
    fun open(uri: String): Boolean = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }.isSuccess
    when (app) {
        "kakao" -> {
            val ok = lat != null && lng != null && open("kakaomap://look?p=$lat,$lng")
            if (!ok) open(if (lat != null && lng != null) "https://map.kakao.com/link/map/$q,$lat,$lng" else "https://map.kakao.com/link/search/$q")
        }
        "naver" -> openLessonNaverMap(ctx, name, lat, lng)
        else -> {
            val ok = open(if (lat != null && lng != null) "geo:$lat,$lng?q=$lat,$lng($q)" else "geo:0,0?q=$q")
            if (!ok) open("https://www.google.com/maps/search/?api=1&query=" + (if (lat != null && lng != null) "$lat,$lng" else q))
        }
    }
}

/** 단계형 위저드 머리 — "2 / 7 · 위치" + 진행 막대(채용 위저드 스텝 인디케이터와 같은 주황·회색). */
@Composable
fun WizardProgress(titles: List<String>, step: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${step + 1}", style = ts(FontWeight.Bold, 14), color = MuyeonColors.primary)
            Text(" / ${titles.size}", style = ts(FontWeight.Medium, 14), color = MuyeonColors.chevron)
            Text("  ${titles.getOrElse(step) { "" }}", style = ts(FontWeight.SemiBold, 14), color = MuyeonColors.textHead)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            titles.indices.forEach { i ->
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if (i <= step) MuyeonColors.primary else MuyeonColors.border))
            }
        }
    }
}

/** 'yyyy-MM-dd' ↔ millis(UTC 자정 — Material DatePicker 기준). */
object MarketDates {
    private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply { timeZone = TimeZone.getTimeZone("UTC") }
    fun toMillis(ymd: String?): Long? = ymd?.takeIf { it.length >= 10 }?.let { runCatching { fmt().parse(it.take(10))?.time }.getOrNull() }
    fun ymd(millis: Long): String = fmt().format(java.util.Date(millis))

    /** 오늘(한국 시간) 'yyyy-MM-dd'. */
    fun todayKst(): String = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }.format(java.util.Date())

    /** 0(일)~6(토). */
    fun dayOfWeek(ymd: String): Int {
        val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.timeInMillis = toMillis(ymd) ?: return 0
        return c.get(java.util.Calendar.DAY_OF_WEEK) - 1
    }
}

/**
 * 날짜 고르기 — Material 달력. allow 가 false 인 날은 고를 수 없다(오늘 이전·휴무 등).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDatePickerDialog(
    initial: String?,
    allow: (String) -> Boolean = { true },
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = MarketDates.toMillis(initial),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = allow(MarketDates.ymd(utcTimeMillis))
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(MarketDates.ymd(it)) }; onDismiss() }) {
                Text("확인", fontFamily = customFontFamily, color = MuyeonColors.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", fontFamily = customFontFamily, color = MuyeonColors.textSub) } },
    ) { DatePicker(state) }
}

/**
 * 주소 → 앱 지역 코드표(/api/regions) 항목. 주소 앞부분이 코드표 이름("서울 강서구", "경기 수원시 팔달구")과
 *  가장 길게 겹치는 것. 카카오 주소가 "서울특별시 …"처럼 길게 와도 앞 두 글자 시도명으로 맞춘다. 못 찾으면 null.
 */
object MarketRegion {
    private var cache: List<com.muyeon.app.ui.quote.RegionDTO>? = null

    suspend fun match(address: String, token: String?): com.muyeon.app.ui.quote.RegionDTO? {
        val list = cache ?: com.muyeon.app.ui.quote.RegionRepo.fetchRegions(token).also { if (it.isNotEmpty()) cache = it }
        val words = address.trim().split(Regex("\\s+"))
        if (words.size < 2) return null
        val sido = shortSido(words[0])
        val rest = words.drop(1).joinToString(" ")
        return list.filter { it.sido == sido && (rest == it.sigungu || rest.startsWith(it.sigungu + " ")) }
            .maxByOrNull { it.sigungu.length }
    }

    /** "서울 강서구 공항대로…" → "서울 강서구"(앞 두 단어, 시도는 짧은 이름). */
    fun regionName(address: String): String {
        val w = address.trim().split(Regex("\\s+"))
        return if (w.size >= 2) "${shortSido(w[0])} ${w[1]}" else address.trim()
    }

    private fun shortSido(s: String): String = when {
        s.startsWith("서울") -> "서울"; s.startsWith("부산") -> "부산"; s.startsWith("대구") -> "대구"
        s.startsWith("인천") -> "인천"; s.startsWith("광주") -> "광주"; s.startsWith("대전") -> "대전"
        s.startsWith("울산") -> "울산"; s.startsWith("세종") -> "세종"; s.startsWith("경기") -> "경기"
        s.startsWith("강원") -> "강원"; s.startsWith("충청북") || s == "충북" -> "충북"; s.startsWith("충청남") || s == "충남" -> "충남"
        s.startsWith("전라북") || s.startsWith("전북") -> "전북"; s.startsWith("전라남") || s == "전남" -> "전남"
        s.startsWith("경상북") || s == "경북" -> "경북"; s.startsWith("경상남") || s == "경남" -> "경남"
        s.startsWith("제주") -> "제주"; else -> s
    }
}
