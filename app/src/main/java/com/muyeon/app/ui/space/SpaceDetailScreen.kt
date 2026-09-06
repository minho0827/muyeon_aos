package com.muyeon.app.ui.space

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * 공간 상세 — iOS `SpaceDetailView.swift`(+Summary/+Sections) 1:1.
 *  Figma "MO 01_예약_숙박" 시안 구조를 그대로 두고 슬롯만 공간 데이터로 바꾼 화면.
 *
 *   시안 → 화면 매핑
 *     MO_header/top             → 헤더(뒤로/제목/찜)
 *     info_reservation 티켓카드  → 공간 요약(이름·유형 / 영업시간·수용인원 패널 / 대관료)
 *     benefit_member_… 카드      → 요금 안내 카드(시간요금·최저요금·패키지 + 환불규정 내부 박스)
 *     banner_h51_member         → 지도 배너
 *     예약자/투숙객 정보 폼      → 공간소개·시설안내 읽기 필드
 *     "알려드립니다" 박스        → 예약 시 유의사항
 *     동의 안내 + 하단 CTA       → 유의사항 동의 문구 + 채팅 / 바로 예약하기
 */
@Composable
fun SpaceDetailScreen(
    api: SpaceApi,
    spaceId: Int,
    initialDate: String = "",
    /** 예약내역에서 진입(웹 fromReservation) — 예약 버튼을 숨긴다. */
    hideReserve: Boolean = false,
    onClose: () -> Unit,
    onChat: (Int) -> Unit,
) {
    var detail by remember { mutableStateOf<SpaceDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var scrapped by remember { mutableStateOf(false) }
    var showReserve by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(spaceId) {
        loading = true
        api.detail(spaceId).onSuccess { detail = it }.onFailure { detail = null }
        scrapped = api.scrappedSpaceIds().contains(spaceId)
        loading = false
    }

    fun openMap(space: SpaceDetail) {
        val query = Uri.encode(space.addressText ?: space.name)
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
        }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://map.naver.com/v5/search/$query")),
                )
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            // 헤더 (MO_header/top — h56, 타이틀 Bold 18, ic_x24_back)
            Row(
                Modifier.fillMaxWidth().height(SpaceDesign.headerHeight).background(Color.White),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.width(44.dp).fillMaxHeight().padding(start = 12.dp)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { SpaceBackArrow() }
                Text(
                    "공간 상세",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 26.sp, color = SpaceDesign.ink900, maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Box(
                    Modifier.width(48.dp).fillMaxHeight().clickable {
                        if (!api.isLoggedIn) {
                            message = "로그인 후 이용할 수 있어요."
                        } else {
                            val next = !scrapped
                            scrapped = next
                            scope.launch {
                                api.setScrap(spaceId, next).onFailure {
                                    scrapped = !next
                                    message = "찜 처리에 실패했어요. 잠시 후 다시 시도해 주세요."
                                }
                            }
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (scrapped) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "찜",
                        tint = if (scrapped) SpaceDesign.primary else SpaceDesign.ink900,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            val space = detail
            when {
                space != null -> Column(
                    Modifier.weight(1f).background(SpaceDesign.ink100).verticalScroll(rememberScrollState()),
                ) {
                    SpaceGallery(space)
                    SpaceTicketCard(space)
                    SpaceSectionHeadline("이 공간의 요금 안내")
                    SpaceFeeCard(space)
                    SpaceMapBanner { openMap(space) }
                    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpaceIntroSection(space)
                        SpaceFacilitySection(space)
                        SpaceCautionSection(space)
                        SpaceLocationSection(space) { openMap(space) }
                    }
                    SpaceAgreementNote(Modifier.padding(top = 20.dp))
                    Spacer(Modifier.height(24.dp))
                }
                loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = SpaceDesign.primary)
                }
                else -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Text(
                        "공간을 찾을 수 없습니다.",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                        color = SpaceDesign.ink500,
                    )
                }
            }

            detail?.let { space ->
                // 하단 고정 버튼 (시안 '결제 진행하기' 자리)
                Column(Modifier.fillMaxWidth().background(Color.White)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SpaceDesign.ink200))
                    Row(
                        Modifier.padding(horizontal = SpaceDesign.gutter)
                            .padding(top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "채팅",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            lineHeight = 20.sp, color = SpaceDesign.ink900, textAlign = TextAlign.Center,
                            modifier = Modifier.width(100.dp).height(52.dp)
                                .clip(RoundedCornerShape(8.dp)).background(Color.White)
                                .border(1.dp, SpaceDesign.ink300, RoundedCornerShape(8.dp))
                                .clickable {
                                    when {
                                        !api.isLoggedIn -> message = "로그인 후 이용할 수 있어요."
                                        (space.ownerId ?: 0) <= 0 ->
                                            message = "공간 소유자 정보가 없어 채팅을 열 수 없습니다."
                                        else -> scope.launch {
                                            api.createDirectRoom(space.ownerId!!, space.id)
                                                .onSuccess { onChat(it) }
                                                .onFailure {
                                                    message = "채팅방을 열지 못했습니다. 잠시 후 다시 시도해 주세요."
                                                }
                                        }
                                    }
                                }
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                        if (!hideReserve) {
                            Text(
                                "바로 예약하기",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                                lineHeight = 20.sp, color = Color.White, textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).height(52.dp)
                                    .clip(RoundedCornerShape(8.dp)).background(SpaceDesign.primary)
                                    .clickable { showReserve = true }
                                    .wrapContentHeight(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReserve) {
        detail?.let { space ->
            SpaceReserveSheet(
                api = api, space = space, initialDate = initialDate,
                onFinish = { result -> showReserve = false; message = result },
            )
        }
    }
    message?.let { msg ->
        QuoteDialog(msg, "", "확인", onConfirm = { message = null }, onDismiss = { message = null })
    }
}

/** 대표 이미지 캐러셀 — 탭 시 풀스크린 뷰어(ImageViewerActivity) 재사용. */
@Composable
private fun SpaceGallery(space: SpaceDetail) {
    val images = space.images ?: emptyList()
    if (images.isEmpty()) return
    val ctx = LocalContext.current
    val pager = rememberPagerState { images.size }

    Box(Modifier.fillMaxWidth().height(240.dp).background(Color(0xFFE5E5EA))) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            AsyncImage(
                QuoteUi.imageUrl(images[i]), null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clickable {
                    ImageViewerActivity.start(ctx, JSONArray(images).toString(), pager.currentPage, false)
                },
            )
        }
        Text(
            "${(pager.currentPage + 1).coerceAtMost(images.size)} / ${images.size}",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
            lineHeight = 15.sp, color = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** 섹션 제목 ("…님을 위한 혜택" 자리). */
@Composable
internal fun SpaceSectionHeadline(text: String) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
        lineHeight = 22.sp, color = SpaceDesign.ink900,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 21.dp)
            .padding(top = 32.dp, bottom = 15.dp),
    )
}

/** banner_h51_member → 지도 배너. */
@Composable
private fun SpaceMapBanner(onClick: () -> Unit) {
    // 시안은 51pt 박스 안 40pt 막대(y=11) 구조지만, 일러스트가 위아래로 삐져나오는 용도라
    //  아이콘 1개만 쓰는 여기서는 막대(40dp)를 그대로 컨테이너로 쓴다
    //  → 아이콘·텍스트·꺾쇠가 모두 막대 기준 세로 중앙에 정렬된다.
    Row(
        Modifier.padding(horizontal = SpaceDesign.gutter).padding(top = 16.dp)
            .fillMaxWidth().height(40.dp)
            .clip(RoundedCornerShape(8.dp)).background(SpaceDesign.bannerBg)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(start = 8.dp).width(61.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Map, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append("지도 앱에서 ") }
                withStyle(SpanStyle(color = SpaceDesign.bannerAccent)) { append("위치·길찾기") }
                withStyle(SpanStyle(color = Color.White)) { append(" 확인") }
            },
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        )
        Box(Modifier.padding(end = 15.dp)) { SpaceChevronRight() }
    }
}

/** 시안의 동의 안내 박스 자리. */
@Composable
private fun SpaceAgreementNote(modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = SpaceDesign.gutter).fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)).background(SpaceDesign.ink100).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.Info, null, tint = SpaceDesign.ink600,
            modifier = Modifier.padding(top = 1.dp).size(14.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append("유의사항 및 환불 규정")
                    }
                    append("에 동의하신다면")
                },
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = SpaceDesign.ink700,
            )
            Text(
                "'바로 예약하기'를 선택해주세요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = SpaceDesign.ink700,
            )
        }
    }
}

/** 그리드 열 수 — 시설 아이콘 4열(시안). */
internal val FACILITY_COLUMNS = GridCells.Fixed(4)
