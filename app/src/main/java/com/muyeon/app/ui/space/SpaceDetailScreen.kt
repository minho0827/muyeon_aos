package com.muyeon.app.ui.space

import androidx.compose.runtime.saveable.rememberSaveable
import com.muyeon.app.ui.common.market.loginRequired
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.space.components.*
import com.muyeon.app.ui.space.reserve.SpaceReserveSheet
import kotlinx.coroutines.launch

/**
 * 상세가 그릴 섹션 — 없는 섹션은 null. 이 매핑 한 곳에서만 "보일지"를 정한다.
 */
private data class SpaceDetailView(
    val images: List<String>?,
    val hourlyOptions: List<SpacePriceOption>?,
    /** 좌표가 있으면 지도 카드, 없으면 주소 칸. */
    val mapLat: Double?,
    val mapLng: Double?,
    val otherSpaces: List<SpaceListItem>?,
    val phone: String?,
)

private fun SpaceDetail.toView() = SpaceDetailView(
    images = images?.takeIf { it.isNotEmpty() },
    hourlyOptions = hourlyOptions.takeIf { it.isNotEmpty() },
    mapLat = lat?.takeIf { lng != null },
    mapLng = lng?.takeIf { lat != null },
    otherSpaces = otherSpaces.takeIf { it.isNotEmpty() },
    phone = phone?.takeIf { it.isNotBlank() },
)

/**
 * 공간 상세 — iOS `SpaceDetailView.swift`(+Summary/+Sections) 1:1.
 *  Figma "MO 01_예약_숙박" 시안 구조를 그대로 두고 슬롯만 공간 데이터로 바꾼 화면.
 *  섹션은 components/, 예약 시트는 reserve/. 이 화면은 불러오기·찜·채팅 연결과 조립만 한다.
 *  하단 바는 [SpaceOwnerActions](본인 공간 — 삭제·수정·예약 관리) / [SpaceChatOnlyBar](예약 내역에서 진입) / [SpaceBookingBar] 중 하나.
 */
@Composable
fun SpaceDetailScreen(
    api: SpaceApi,
    spaceId: Int,
    initialDate: String = "",
    /** 예약내역에서 진입(웹 fromReservation) — 예약 버튼을 숨긴다. */
    hideReserve: Boolean = false,
    /** ★ 로그인 후 이어서 할 일(reserve·chat·like) — 상세를 다 불러온 뒤 한 번만 한다(LoginResume). */
    resumeAction: String? = null,
    onClose: () -> Unit,
    onChat: (Int) -> Unit,
    /** 예약 요청 접수 — 부모가 결과 키 RESERVATIONS 를 싣는다(웹 예약내역 재조회). */
    onReserved: () -> Unit = {},
    /** 찜 변경 — 부모가 결과 키 SPACE 를 싣는다(웹 찜 목록 재조회). */
    onScrapChanged: () -> Unit = {},
    /** 본인 공간 — [수정] / [예약 관리]. */
    onEdit: (Int) -> Unit = {},
    onManage: () -> Unit = {},
    /** 본인 공간 [삭제] 뒤 — 부모가 결과 키를 싣고 닫는다. */
    onDeleted: () -> Unit = onClose,
    /** 호스트의 다른 공간 카드. */
    onOpenSpace: (Int) -> Unit = {},
    /** 결제 완료 — 부모가 내 예약을 연다. */
    onPaid: () -> Unit = {},
) {
    var detail by remember { mutableStateOf<SpaceDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var scrapped by remember { mutableStateOf(false) }
    var showReserve by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteOpen by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(spaceId, com.muyeon.app.auth.accountEpoch()) {
        scrapped = false
        loading = true
        api.detail(spaceId).onSuccess { detail = it }.onFailure { detail = null }
        scrapped = api.scrappedSpaceIds().contains(spaceId)
        loading = false
    }

    fun openMap(space: SpaceDetail) {
        if (space.lat != null && space.lng != null) {
            openDirections(ctx, "naver", space.name, space.lat, space.lng)
            return
        }
        val query = Uri.encode(space.addressText ?: space.name)
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
        }.onFailure {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://map.naver.com/v5/search/$query"))) }
        }
    }

    fun toggleScrap() {
        if (!api.isLoggedIn) { message = loginRequired("openSpaceDetail", "spaceId" to spaceId, "then" to "like"); return }
        val next = !scrapped
        scrapped = next
        scope.launch {
            api.setScrap(spaceId, next).onSuccess { onScrapChanged() }.onFailure {
                scrapped = !next
                message = "찜 처리에 실패했어요. 잠시 후 다시 시도해 주세요."
            }
        }
    }

    fun chat(space: SpaceDetail) {
        when {
            !api.isLoggedIn -> message = loginRequired("openSpaceDetail", "spaceId" to spaceId, "then" to "chat")
            (space.ownerId ?: 0) <= 0 -> message = "공간 소유자 정보가 없어 채팅을 열 수 없어요."
            else -> scope.launch {
                api.createDirectRoom(space.ownerId!!, space.id)
                    .onSuccess { if (it > 0) onChat(it) else message = "채팅방을 열지 못했어요. 잠시 후 다시 시도해 주세요." }
                    .onFailure { message = it.message ?: "채팅방을 열지 못했어요. 잠시 후 다시 시도해 주세요." }
            }
        }
    }

    // 로그인 후 다시 연 화면 — 상세를 받은 뒤(이벤트) 하려던 동작을 한 번만 이어서 한다. 본인 공간·예약 숨김이면 하지 않는다.
    var resumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(detail, resumeAction) {
        val space = detail ?: return@LaunchedEffect
        if (resumed || resumeAction.isNullOrEmpty() || !api.isLoggedIn) return@LaunchedEffect
        resumed = true
        when (resumeAction) {
            "reserve" -> if (!space.isOwner && !hideReserve) showReserve = true
            "chat" -> if (!space.isOwner) chat(space)
            "like" -> if (!scrapped) toggleScrap()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            SpaceDetailTopBar(scrapped, onClose = onClose, onScrap = { toggleScrap() })
            val space = detail
            val phase = when { space != null -> DetailPhase.Content; loading -> DetailPhase.Loading; else -> DetailPhase.Error }
            // 본문 자리는 늘 같은 크기(weight) — 자리(스켈레톤) → 내용은 겹쳐 바꾸기만.
            StableSwap(phase, Modifier.weight(1f).fillMaxWidth()) { p ->
                when (p) {
                    DetailPhase.Loading -> SpaceDetailSkeleton()
                    DetailPhase.Error -> SpaceNotFound()
                    DetailPhase.Content -> detail?.let { sp ->
                        SpaceDetailBody(
                            sp, sp.toView(), selectedOption,
                            onSelectOption = { selectedOption = it }, onOpenMap = { openMap(sp) }, onOpenSpace = onOpenSpace,
                        )
                    }
                }
            }
            // 하단 바 자리 — 불러오는 중엔 같은 높이의 자리, 못 찾으면 같은 높이의 빈칸.
            when {
                space == null && loading -> MarketBottomBarSkeleton()
                space == null -> BottomBarSpace()
                space.isOwner -> SpaceOwnerActions(onDelete = { deleteOpen = true }, onEdit = { onEdit(space.id) }, onManage = onManage)
                hideReserve -> SpaceChatOnlyBar(space.toView().phone) { chat(space) }
                else -> SpaceBookingBar(space.toView().phone, onChat = { chat(space) }) {
                    if (!api.isLoggedIn) message = loginRequired("openSpaceDetail", "spaceId" to spaceId, "then" to "reserve") else showReserve = true
                }
            }
        }
    }

    if (showReserve) {
        detail?.let { space ->
            SpaceReserveSheet(
                api = api, space = space, initialDate = initialDate,
                initialOptionId = selectedOption,
                onFinish = { result -> showReserve = false; message = result },
                onReserved = onReserved,
                onPaid = { showReserve = false; onPaid() },
            )
        }
    }
    // 내 공간 삭제 — '내 공간'과 같은 확인창 · 결제된 예약이 있으면 서버 문구 그대로.
    if (deleteOpen) {
        com.muyeon.app.ui.space.booking.DeleteSpaceDialog(
            onConfirm = {
                scope.launch {
                    api.remove(spaceId)
                        .onSuccess { android.widget.Toast.makeText(ctx, com.muyeon.app.R.string.owner_space_deleted, android.widget.Toast.LENGTH_SHORT).show(); onDeleted() }
                        .onFailure { message = it.message ?: ctx.getString(com.muyeon.app.R.string.owner_delete_failed) }
                }
            },
            onDismiss = { deleteOpen = false },
        )
    }
    message?.let { msg -> com.muyeon.app.ui.common.market.MarketMessageDialog(msg) { message = null } }
}

/** 본문 — 섹션 조립만(보일지는 [toView] 가 정했다). */
@Composable
private fun SpaceDetailBody(
    space: SpaceDetail,
    view: SpaceDetailView,
    selectedOption: Int?,
    onSelectOption: (Int) -> Unit,
    onOpenMap: () -> Unit,
    onOpenSpace: (Int) -> Unit,
) {
    // ★ 2026-11-06 목록·화면 바탕 흰색(웹과 같게, 사용자 결정) — SpaceDesign.ink100 → 흰색. 티켓 카드는 그림자로 구분된다.
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        view.images?.let { SpaceGallery(it) }
        SpaceTicketCard(space)
        SpaceSectionHeadline("이 공간의 요금 안내")
        SpaceFeeCard(space, space.refund)
        SpaceMapBanner(onOpenMap)
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            view.hourlyOptions?.let { SpacePriceOptions(it, selectedOption ?: it.first().id, onSelectOption) }
            SpaceIntroSection(space)
            SpaceFacilitySection(space)
            SpaceCautionSection(space)
            if (view.mapLat != null && view.mapLng != null) SpaceMapLocationSection(space.name, space.addressText, view.mapLat, view.mapLng)
            else SpaceAddressLocationSection(space.addressText, onOpenMap)
            view.otherSpaces?.let { HostOtherSpacesRow(space.hostName, it, onOpenSpace) }
        }
        SpaceAgreementNote(Modifier.padding(top = 20.dp))
        Spacer(Modifier.height(24.dp))
    }
}
