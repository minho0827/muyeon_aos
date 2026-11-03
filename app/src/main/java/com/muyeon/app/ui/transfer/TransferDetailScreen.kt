package com.muyeon.app.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageExpandableText
import com.muyeon.app.ui.common.MarketBadge
import com.muyeon.app.ui.common.MarketBottomBar
import com.muyeon.app.ui.common.MarketFormat
import com.muyeon.app.ui.common.MarketGallery
import com.muyeon.app.ui.common.MarketInfoTable
import com.muyeon.app.ui.common.MarketMapCard
import com.muyeon.app.ui.common.MarketOwnerBar
import com.muyeon.app.ui.common.MarketSectionTitle
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.common.MuyeonEmptyState
import com.muyeon.app.ui.common.MuyeonList
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 양도 글 상세 — 사진 갤러리 → 칩·제목·지역 → 기본정보 표 → 옵션 → 접는 소개 → 지도 → 담당자 → 고지 → 신고,
 *  고정 하단 바(하트·전화·채팅하기). 글쓴이 본인이면 하단 바 대신 [수정] [상태 변경].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransferDetailScreen(
    api: TransferApi,
    id: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onChatRoom: (Int, String) -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var detail by remember { mutableStateOf<TransferDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var chatBusy by remember { mutableStateOf(false) }
    var statusOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.detail(id).onSuccess { detail = it; error = null }.onFailure { error = it.message }
        loading = false
    }
    LaunchedEffect(id) { load() }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onBack).semantics { contentDescription = "뒤로" }, Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead)
            }
            Text(
                "양도 글", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
        }
        val d = detail
        when {
            d != null -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    MarketGallery(d.images, placeholderRes = com.muyeon.app.R.drawable.transfer_noimage)
                    TransferBody(d, onReport = {
                        if (!api.isLoggedIn) onMessage("로그인 후 이용할 수 있어요.") else reportOpen = true
                    })
                }
                if (d.isAuthor) {
                    MarketOwnerBar("수정", { onEdit(d.card.id) }, "상태 변경", { statusOpen = true })
                } else {
                    MarketBottomBar(
                        liked = d.card.scrapped,
                        onLike = {
                            if (!api.isLoggedIn) { onMessage("로그인 후 이용할 수 있어요."); return@MarketBottomBar }
                            val next = !d.card.scrapped
                            detail = d.copy(card = d.card.copy(scrapped = next))
                            scope.launch {
                                api.setScrap(d.card.id, next).onSuccess { onChanged() }.onFailure { e ->
                                    detail = detail?.let { it.copy(card = it.card.copy(scrapped = !next)) }
                                    onMessage(e.message ?: "찜 처리에 실패했어요.")
                                }
                            }
                        },
                        phone = d.contactPhone,
                        primary = "채팅하기",
                        busy = chatBusy,
                    ) {
                        if (!api.isLoggedIn) { onMessage("로그인 후 이용할 수 있어요."); return@MarketBottomBar }
                        chatBusy = true
                        scope.launch {
                            api.directRoom(d.card.authorId, d.card.id)
                                .onSuccess { onChatRoom(it, d.card.authorName) }
                                .onFailure { onMessage(it.message ?: "채팅방을 열지 못했어요.") }
                            chatBusy = false
                        }
                    }
                }
            }
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary) }
            else -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                MuyeonEmptyState(error ?: "양도 글을 찾을 수 없어요.", actionTitle = "다시 시도", onAction = { loading = true; scope.launch { load() } })
            }
        }
    }

    if (statusOpen) {
        val d = detail
        TransferStatusDialog(
            current = d?.card?.status,
            onPick = { s ->
                if (d != null) scope.launch {
                    api.setStatus(d.card.id, s).onSuccess { detail = d.copy(card = d.card.copy(status = s)); onChanged() }
                        .onFailure { onMessage(it.message ?: "상태를 바꾸지 못했어요.") }
                }
            },
            onDismiss = { statusOpen = false },
        )
    }
    if (reportOpen) {
        MuyeonDialog(
            title = "허위 매물로 신고할까요?",
            message = "운영팀이 확인 후 조치해요.",
            actions = listOf(
                MuyeonDialogAction("신고하기", MuyeonDialogKind.DESTRUCTIVE) {
                    scope.launch {
                        api.report(id).onSuccess { onMessage("신고가 접수됐어요.") }.onFailure { onMessage(it.message ?: "신고하지 못했어요.") }
                    }
                },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { reportOpen = false },
        )
    }
}

/** 상태 변경 — 판매중 / 예약중 / 거래완료. */
@Composable
fun TransferStatusDialog(current: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "상태 변경",
        actions = TransferLabels.status.map { (code, label) ->
            MuyeonDialogAction(if (code == current) "$label (현재)" else label, if (code == current) MuyeonDialogKind.PRIMARY else MuyeonDialogKind.CANCEL) {
                if (code != current) onPick(code)
            }
        } + MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransferBody(d: TransferDetail, onReport: () -> Unit) {
    val c = d.card
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MarketBadge(
                    TransferLabels.status(c.status),
                    bg = when (c.status) { "OPEN" -> MuyeonColors.primary; "HOLD" -> MuyeonColors.primaryDeep; else -> MuyeonColors.textSub },
                )
                TransferLabels.facility(c.facilityType)?.let { MarketBadge(it, fg = MuyeonColors.textHead, bg = MuyeonList.ink100) }
                TransferLabels.operating(c.operatingStatus)?.let { MarketBadge(it, fg = MuyeonColors.textHead, bg = MuyeonList.ink100) }
            }
            Text(c.title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, color = MuyeonColors.textHead)
            Text(
                listOfNotNull(c.region, c.createdAt?.take(10)?.replace('-', '.'), "조회 ${c.viewCount}").joinToString(" · "),
                fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MarketSectionTitle("기본 정보")
            MarketInfoTable(infoRows(d))
        }

        if (c.options.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MarketSectionTitle("옵션")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    c.options.forEach {
                        Text(
                            TransferLabels.option(it), fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            color = MuyeonColors.textHead,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(MuyeonList.ink100).padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        d.description?.takeIf { it.isNotBlank() }?.let {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MarketSectionTitle("상세 소개")
                StageExpandableText(it, lines = 5)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MarketSectionTitle("위치")
            val addr = listOfNotNull(d.roadAddress, d.addressDetail).joinToString(" ").ifBlank { null }
            MarketMapCard(
                name = d.roadAddress ?: c.region ?: "위치",
                address = addr ?: c.region,
                lat = d.lat, lng = d.lng,
                approxNote = if (d.locationApprox) "대략적인 위치예요. 정확한 주소는 채팅으로 확인해 주세요." else null,
            )
        }

        d.contactName?.let {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MarketSectionTitle("담당자")
                Text(it, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MuyeonColors.textHead)
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonList.ink100).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Info, null, tint = MuyeonColors.textSub, modifier = Modifier.padding(top = 2.dp).size(16.dp))
            Text(
                "무용연은 거래를 중개하지 않아요. 계약 전 등기부등본·건물 상태를 꼭 확인하세요.",
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 20.sp, color = MuyeonColors.body,
            )
        }

        if (!d.isAuthor) {
            Text(
                "허위 매물 신고", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
                textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onReport).padding(vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** 기본정보 표 — 빈 값 줄은 숨기고, 금액 null 은 "협의". 금액 단위는 만원(표 라벨에 표기). */
private fun infoRows(d: TransferDetail): List<Pair<String, String>> {
    val c = d.card
    val rows = mutableListOf<Pair<String, String>>()
    val loc = listOfNotNull(d.roadAddress, d.addressDetail).joinToString(" ").ifBlank { c.region }
    loc?.let { rows += "소재지" to it }
    c.areaPyeong?.let { rows += "총면적" to "${pyeongText(it)}평 (약 ${(it * 3.3058).roundToInt()}㎡)" }
    val floor = c.floor?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("층")) it else "${it}층" }
    val elev = d.hasElevator?.let { if (it) "엘리베이터 있음" else "엘리베이터 없음" }
    listOfNotNull(floor, elev).takeIf { it.isNotEmpty() }?.let { rows += "층/엘리베이터" to it.joinToString(" · ") }
    rows += "보증금(만원)" to MarketFormat.manwon(c.deposit)
    rows += "월세(만원)" to MarketFormat.manwon(c.monthlyRent)
    c.maintenanceFee?.let { rows += "관리비(만원)" to MarketFormat.num(it) }
    rows += "권리금(만원)" to (MarketFormat.manwon(c.premium) + if (c.premiumNegotiable && c.premium != null) " (협의가능)" else "")
    if (d.vatSeparate) rows += "부가세" to "별도"
    c.hourlyPrice?.let { rows += "시간당 대관" to (MarketFormat.won(it) ?: "") }
    d.parkingCount?.let { rows += "주차" to if (it > 0) "${it}대" else "불가" }
    d.availableFrom?.let { rows += "입주 가능일" to it.replace('-', '.') }
    d.studentCount?.let { rows += "원생 수" to "${MarketFormat.num(it)}명" }
    d.monthlyRevenue?.let { rows += "월매출(만원)" to MarketFormat.num(it) }
    return rows
}
