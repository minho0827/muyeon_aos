package com.muyeon.app.ui.resume

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.review.ReviewApi
import com.muyeon.app.ui.review.ReviewList
import com.muyeon.app.ui.review.ReviewOptions
import com.muyeon.app.webview.ActiveRole
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

/**
 * 공개 프로필(강사 상세) — iOS `PublicProfileView.swift` 이식.
 *  히어로(사진·이름·평점) + 섹션(소개/전문분야/활동지역/레슨시간/경력/학력/자격증/공연/수상/포트폴리오/후기)
 *  + 하단 CTA(문의하기·견적요청) + 찜/신고.
 *
 *  preview=true 는 **본인이 일반회원 시점으로 확인**하는 모드(공개범위 화면의 미리보기).
 */
@Composable
fun PublicProfileScreen(
    api: ResumeApi,
    reviewApi: ReviewApi,
    userId: Int,
    preview: Boolean = false,
    src: String? = null,
    // 학원·공연팀(채용) 시점 — 하단 CTA 가 무료 [채팅하기] 하나로 바뀐다(iOS recruitMode)
    recruitMode: Boolean = false,
    // 다른 화면 안에 끼워 넣은 경우 — 자체 뒤로가기 버튼을 숨긴다(iOS embedded)
    embedded: Boolean = false,
    // 열람 전용 맥락 — 하단 CTA 전체 숨김(iOS hideCta)
    hideCta: Boolean = false,
    // 이미 받아둔 프로필로 시작(지원자 이력서 등) — 네트워크 재조회를 하지 않는다
    initialProfile: PublicProfile? = null,
    onClose: () -> Unit,
    onOpenChat: (Int) -> Unit,
    /** 레슨 문의 — 강사의 대표 장르(첫 장르)의 견적 종류 id 를 넘긴다(웹 Home·LessonTab 과 같이 종류 고르기를 건너뛴다). */
    onRequestQuote: (category: String?) -> Unit,
    onOpenReviews: () -> Unit,
    // 이미 이 강사에게 견적요청했고 응답이 있으면 [받은 견적 보기] → 받은 견적(quoteId) (iOS onReceivedQuotes)
    onOpenReceivedQuotes: (Int) -> Unit = {},
    /** 내 후기 [수정] → 작성 화면 수정 모드. 비우면 [수정]·[삭제] 를 그리지 않는다(미리보기·지원자 화면). */
    onEditMyReview: (() -> Unit)? = null,
    /** 내 후기 삭제 뒤(이 화면 후기는 여기서 다시 읽는다). */
    onDeletedMyReview: () -> Unit = {},
) {
    var profile by remember { mutableStateOf<PublicProfile?>(null) }
    var reviews by remember { mutableStateOf<ReviewList?>(null) }
    var loading by remember { mutableStateOf(true) }
    var scrapped by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    // 이 화면은 사용자 단위라 이력서 id 가 없다. '구직'에 노출되는 건 기본 이력서이므로 그걸 관리 대상으로 삼는다.
    var myResumeId by remember { mutableStateOf<Int?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    // 무용수 활동유형은 레슨(견적) 요청 불가(2026-09-26 정책) — 버튼은 보이고, 누르면 안내 후 일반 전환 시 이어서 진행(iOS attemptQuote).
    var dancerGate by remember { mutableStateOf(false) }

    // 본인 프로필 → [수정하기](별도 액티비티)에서 돌아오면 다시 읽는다(결과 키 RESUME/PROFILE).
    var reloadTick by rememberSaveable { mutableIntStateOf(0) }
    val editLauncher = com.muyeon.app.result.rememberResultLauncher { keys ->
        if (com.muyeon.app.result.ResultKeys.RESUME in keys || com.muyeon.app.result.ResultKeys.PROFILE in keys) {
            reloadTick++
        }
    }

    LaunchedEffect(userId, preview, initialProfile, reloadTick, com.muyeon.app.auth.accountEpoch()) {
        if (initialProfile != null) {
            profile = initialProfile
            scrapped = initialProfile.scrapped == true
        } else {
            api.publicProfile(userId, preview, src).onSuccess {
                profile = it
                scrapped = it.scrapped == true
                if (it.selfView == true) {
                    api.list().onSuccess { items ->
                        myResumeId = (items.firstOrNull { r -> r.isDefault } ?: items.firstOrNull())?.id
                    }
                }
            }
        }
        reviewApi.list(userId).onSuccess { reviews = it }
        loading = false
    }

    // 내 프로필(본인 열람/미리보기) — 자기 자신에게 채팅/문의 CTA는 무의미하므로 하단 CTA 전체 숨김(iOS isSelfProfile).
    val isSelf = preview || profile?.selfView == true

    Box(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        if (loading) {
            CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        val p = profile
        if (p == null) {
            Text(
                "프로필을 불러오지 못했어요",
                fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.secondary,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ProfileHero(p)
                // 본문 — iOS content(p): 이름/배지/칩/확인 기관/한 줄 소개/예시 안내/본인 안내 → 섹션
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    // 이름 + 찜(iOS nameHeader — 하트는 이름 줄 오른쪽)
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            p.name ?: "강사",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                            lineHeight = 26.sp, color = MuyeonColors.textHead,
                        )
                        if (p.isDemoProfile == true) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "프로필 예시",
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                                color = MuyeonColors.primary,
                                modifier = Modifier.clip(RoundedCornerShape(99.dp))
                                    .background(MuyeonColors.primary.copy(alpha = 0.10f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (!preview) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).clickable {
                                    val next = !scrapped
                                    scrapped = next
                                    scope.launch { api.setScrap(userId, next).onFailure { scrapped = !next } }
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (scrapped) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "찜",
                                    tint = if (scrapped) MuyeonColors.primary else MuyeonColors.hint,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                    CredentialBadgeRow(p.badges) // ★ 2026-10-12
                    ChipRow(p, onOpenReviews)
                    // ★ 2026-11-04 확인 기관 로고(이름·분야 바로 아래) — 비어 있으면 문구·자리 모두 그리지 않는다.
                    p.badges?.logos?.takeIf { it.isNotEmpty() }?.let {
                        VerifiedLogoRow(it, Modifier.padding(top = 10.dp))
                    }
                    p.oneLiner?.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            it,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            lineHeight = 20.sp, color = MuyeonColors.textSub,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (p.isDemoProfile == true) {
                        Text(
                            "무용연에서 제공하는 프로필 예시예요. 실제 강사가 아니므로 레슨 문의, 견적 요청 및 예약은 이용할 수 없어요.",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textSub,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                                .clip(RoundedCornerShape(12.dp)).background(MuyeonColors.tileIdle).padding(14.dp),
                        )
                    }
                    // 본인 프로필(채용 시점) — 공개범위 안내(iOS recruitStatus)
                    if (recruitMode && p.selfView == true) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 14.dp)
                                .clip(RoundedCornerShape(12.dp)).background(MuyeonColors.tileIdle).padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.AccountCircle, null, tint = MuyeonColors.primary, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                            Text(
                                "내 프로필이에요. 비공개로 설정한 항목은 나에게만 보이고 다른 회원에게는 노출되지 않아요.",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                                lineHeight = 19.sp, color = MuyeonColors.textSub,
                            )
                        }
                    }
                }
                // 내 후기 줄의 [수정]·[삭제] — 부품(ReviewOwnerActions)을 칸에 끼워 넣기만 한다.
                val mineActions: @Composable () -> Unit = {
                    onEditMyReview?.let { edit ->
                        com.muyeon.app.ui.review.ReviewOwnerActions(
                            reviewApi, userId, onEdit = edit,
                            onDeleted = {
                                onDeletedMyReview()
                                scope.launch { reviewApi.list(userId).onSuccess { reviews = it } }
                            },
                        )
                    }
                }
                ProfileBody(p, reviews, isSelf, onOpenReviews, mineActions)   // 레이아웃 분기
                Spacer(Modifier.height(20.dp))
            }

            // 하단 CTA(iOS ctaBar) — 채용 시점은 무료 [채팅하기] 하나,
            //  일반은 기존 채팅방이 있을 때만 [문의하기] + [레슨 문의](이미 받은 견적이 있으면 [받은 견적 보기]).
            if (!hideCta && !isSelf) {
                Row(
                    Modifier.fillMaxWidth()
                        .shadow(8.dp, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f))
                        .background(MuyeonColors.surface)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (p.isDemoProfile == true && !recruitMode) {
                        CtaButton("문의하기", filled = false, enabled = false, modifier = Modifier.weight(1f)) {}
                        CtaButton("레슨 견적 요청하기", filled = true, enabled = false, modifier = Modifier.weight(1f)) {}
                    } else if (recruitMode) {
                        CtaButton(
                            "채팅하기", filled = true, modifier = Modifier.weight(1f)
                                .alpha(if (p.isDemoProfile == true) 0.45f else 1f),
                            enabled = p.isDemoProfile != true, keepFilledWhenDisabled = true,
                        ) { onOpenChat(p.id) }
                    } else {
                        val roomId = p.chatRoomId ?: 0
                        if (roomId > 0) {
                            CtaButton("문의하기", filled = false, modifier = Modifier.weight(1f)) { onOpenChat(roomId) }
                        }
                        val receivedId = (p.myQuoteId ?: 0).takeIf { it > 0 && p.myQuoteHasResponse == true }
                        CtaButton(if (receivedId != null) "받은 견적 보기" else "레슨 문의", filled = true, modifier = Modifier.weight(1f)) {
                            // 이미 이 강사에게 견적요청한 상태 → 받은 견적 확인으로 분기(iOS·웹 TeacherDetail 과 동일 규칙)
                            when {
                                receivedId != null -> onOpenReceivedQuotes(receivedId)
                                ActiveRole.isDancer(ctx) -> dancerGate = true
                                else -> onRequestQuote(quoteCategoryOf(p))
                            }
                        }
                    }
                }
            }
        }

        // 뒤로 / ⋮ — 스크롤과 무관하게 화면 위에 고정(iOS overlay topLeading/topTrailing)
        if (!embedded) {
            CircleIconButton(
                Icons.AutoMirrored.Filled.ArrowBack, "뒤로",
                modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 8.dp),
                onClick = onClose,
            )
        }
        if (!preview) {
            Box(Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 8.dp)) {
                CircleIconButton(Icons.Filled.MoreHoriz, "더보기", iconSize = 18.dp) { menuOpen = true }
                // 본인 프로필이면 관리(수정/삭제), 남의 프로필이면 신고.
                //  자기 자신을 신고하는 메뉴는 의미가 없어 서로 배타로 둔다(iOS 동일).
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (p.selfView == true) {
                        DropdownMenuItem(
                            text = { Text("수정하기", fontFamily = customFontFamily, fontSize = 14.sp) },
                            onClick = {
                                menuOpen = false
                                editLauncher.launch(ResumeActivity.editIntent(ctx, myResumeId, null))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.danger) },
                            onClick = { menuOpen = false; deleteOpen = true },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("신고하기", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.danger) },
                            onClick = { menuOpen = false; reportOpen = true },
                        )
                    }
                }
            }
        }
    }

    // 삭제 확인 — 되돌릴 수 없어 한 번 더 묻는다. 지원 이력이 있으면 서버가 막고 사유를 돌려준다.
    if (deleteOpen) {
        QuoteDialog(
            title = "이 이력서를 삭제할까요?",
            message = "구직 목록에서 사라져요. 되돌릴 수 없어요.",
            confirmText = "삭제",
            onConfirm = {
                deleteOpen = false
                val id = myResumeId
                if (id == null) toast = "삭제할 이력서를 찾지 못했어요."
                else scope.launch {
                    api.remove(id).onSuccess { onClose() }.onFailure { toast = it.message }
                }
            },
            onDismiss = { deleteOpen = false },
        )
    }

    // 신고 — 사유 4개 중 선택(iOS confirmationDialog "신고 사유를 선택해 주세요"). 본문 {targetType:TEACHER, targetId, reason}.
    if (reportOpen) {
        ReportReasonSheet(
            onPick = { reason ->
                reportOpen = false
                scope.launch {
                    api.report(userId, reason)
                        .onSuccess { toast = "신고가 접수됐어요." }
                        .onFailure { toast = it.message ?: "신고 접수에 실패했어요." }
                }
            },
            onDismiss = { reportOpen = false },
        )
    }
    // 무용수 활동유형 → 레슨 문의 차단 안내. 일반 전환에 성공하면 이어서 견적 요청(iOS RoleGate.dancerLessonCustomerGate).
    if (dancerGate) {
        MuyeonDialog(
            title = "회원유형 안내",
            message = "무용수 유형에서는 레슨을 요청하거나 예약할 수 없어요. 일반 유형으로 전환하면 계속할 수 있어요.",
            actions = listOf(
                MuyeonDialogAction("${ActiveRole.label("GENERAL")} 유형으로 전환하고 계속", MuyeonDialogKind.PRIMARY) {
                    dancerGate = false
                    scope.launch {
                        if (ActiveRole.saveOnServer(ctx, "GENERAL")) {
                            ActiveRole.store(ctx, "GENERAL")
                            onRequestQuote(profile?.let(::quoteCategoryOf))
                        }
                    }
                },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL) { dancerGate = false },
            ),
            onDismiss = { dancerGate = false },
        )
    }
    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

/**
 * 히어로 — 풀블리드 사진(360) + 좌하단 "{장르} 강사" 태그. 사진 없으면 회색 240 + 사람 아이콘(iOS hero).
 *  인스타 스타일 — 사진은 자르지 않고 전체(Fit), 남는 여백은 같은 사진을 흐리게 확대해 비슷한 색감으로 채운다.
 *  blur 는 API 31+ 에서만 적용, 그 이하는 어둡게만.
 */
@Composable
private fun ProfileHero(p: PublicProfile) {
    val url = QuoteUi.imageUrl(p.image)
    Box(Modifier.fillMaxWidth().height(if (url != null) 360.dp else 240.dp).background(MuyeonColors.tileIdle)) {
        if (url != null) {
            Box(Modifier.matchParentSize().background(Color.Black).clipToBounds()) {
                AsyncImage(
                    url, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().scale(1.2f).blur(24.dp),
                    placeholder = com.muyeon.app.ui.components.imageLoadingPainter(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.10f)))
                AsyncImage(url, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(), placeholder = com.muyeon.app.ui.components.imageLoadingPainter())
            }
        } else {
            Icon(
                Icons.Filled.Person, null, tint = MuyeonColors.hint,
                modifier = Modifier.align(Alignment.Center).size(64.dp),
            )
        }
        p.genres?.firstOrNull()?.let { genre ->
            Text(
                "$genre 강사",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                lineHeight = 14.sp, color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** 지역·전문분야 회색 칩 + 평점(탭 → 전체 후기) — iOS chipRow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(p: PublicProfile, onOpenReviews: () -> Unit) {
    val region = p.activeRegion?.takeIf { it.isNotEmpty() }
    val fs = (p.fields ?: p.genres)?.takeIf { it.isNotEmpty() }
    val cnt = p.ratingCount ?: 0
    if (region == null && fs == null && cnt <= 0) {
        Spacer(Modifier.height(8.dp))
        return
    }
    // ★ 2026-11-07 한 줄(Row)이라 지역이 길면 뒤 칩(전문 분야 '기초반, 취미반')이 잘려 보였다 → 넘치면 다음 줄로(FlowRow, 전문 분야 섹션과 같은 부품).
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        region?.let { GrayChip(it) }
        fs?.let { GrayChip(it.take(2).joinToString(", ") { f -> ResumeOptions.fieldLabel(f) }) }
        if (cnt > 0) {
            Row(
                Modifier.clickable(onClick = onOpenReviews),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, null, tint = MuyeonColors.primary, modifier = Modifier.size(12.dp))
                Text(
                    String.format(Locale.KOREA, "%.1f", p.ratingAvg ?: 0.0),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "(후기 $cnt)",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    lineHeight = 15.sp, color = MuyeonColors.textSub,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonColors.hint,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun GrayChip(text: String) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp,
        color = MuyeonColors.textSub, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MuyeonColors.tileIdle)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * 본문 — 레이아웃 분기(웹 TeacherDetail 4e39a91 와 같은 규칙).
 *  · 멤버십 + 이미지형(detailType=IMAGE) + 상세컷 있음 → 상세컷 전체폭 세로 스택(멤버십 혜택)
 *  · 그 외 모든 강사 → 꾸민 레이아웃(ProfileSections — 레슨 가능 시간 점 표·영상·후기)
 *  ★ 2026-11-04 멤버십 없는 강사에게 보이던 기본 폼(라벨 + 읽기전용 박스)은 없앴다.
 */
@Composable
private fun ProfileBody(
    p: PublicProfile, reviews: ReviewList?, isSelf: Boolean, onOpenReviews: () -> Unit,
    mineActions: @Composable () -> Unit = {},
) {
    val detailImages = p.detailImages.orEmpty().filter { it.isNotBlank() }
    if (p.profileMembershipActive == true && p.detailType == "IMAGE" && detailImages.isNotEmpty()) {
        Column(Modifier.fillMaxWidth()) {
            ContactSection(p)
            Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                detailImages.forEach { url ->
                    AsyncImage(
                        QuoteUi.imageUrl(url), null, contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = com.muyeon.app.ui.components.imageLoadingPainter(),
                    )
                }
            }
            VideoSection(p)
            ReviewsSection(reviews, onOpenReviews, mineActions)
        }
    } else {
        ProfileSections(p, reviews, isSelf, onOpenReviews, mineActions)
    }
}

/** 꾸민 레이아웃 섹션 묶음 — iOS PublicProfileView.sections(p) 와 같은 순서·스타일. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSections(
    p: PublicProfile, reviews: ReviewList?, isSelf: Boolean, onOpenReviews: () -> Unit,
    mineActions: @Composable () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        p.intro?.replace("\r", "")?.takeIf { it.isNotEmpty() }?.let {
            ProfileSection("소개글") {
                Text(it, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp, color = MuyeonColors.body)
            }
        }
        // ★ 2026-11-04 기본 정보 — 웹 TeacherDetail 꾸민 레이아웃과 같은 줄·순서(빈 값 줄은 뺀다).
        //  활동 지역·연락처는 이 표로 옮겼다(예전엔 따로 섹션·맨 위 연락처).
        BasicInfoSection(p)
        (p.fields ?: p.genres)?.takeIf { it.isNotEmpty() }?.let { fs ->
            ProfileSection("전문 분야") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fs.forEach { f ->
                        Text(
                            ResumeOptions.fieldLabel(f),
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.primary, maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(MuyeonColors.primary.copy(alpha = 0.10f))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
        if (!p.availableDays.isNullOrEmpty() || !p.availableTimeSlots.isNullOrEmpty()) {
            ProfileSection("레슨 가능 시간") { LessonTimeGrid(p) }
        }
        p.careers?.takeIf { it.isNotEmpty() }?.let { cs ->
            ProfileSection("경력 (요약)") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    verified(p, "CAREER")?.let { SectionVerifiedLine(it) }
                    CareerSummary(cs, p.career)
                }
            }
        }
        p.educations?.takeIf { it.isNotEmpty() }?.let { edus ->
            ProfileSection("학력") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    verified(p, "EDUCATION")?.let { SectionVerifiedLine(it) }
                    edus.forEach { e ->
                        val text = listOf(e.school, e.period).filter { it.isNotEmpty() }.joinToString(" (") +
                            (if (e.period.isEmpty()) "" else ")")
                        BodyText(text)
                    }
                }
            }
        }
        // 경력 · 자격 — 웹과 같이 무용단 경력·자격증 줄(확인 기관 자격 로고는 맨 위).
        val certLogos = verified(p, "CERT")
        if (!p.companyCareer.isNullOrBlank() || !p.certificates.isNullOrBlank() || certLogos != null) {
            ProfileSection("경력 · 자격") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    certLogos?.let { SectionVerifiedLine(it) }
                    InfoRows(listOf("무용단 경력" to p.companyCareer, "자격증" to p.certificates))
                }
            }
        }
        p.performances?.takeIf { it.isNotEmpty() }?.let { perfs ->
            ProfileSection("공연 이력") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    perfs.forEach { pf ->
                        val line = listOf("${pf.year} ${pf.title}", pf.role).filter { it.isNotBlank() }.joinToString(" · ")
                        BodyText(line.trim())
                    }
                }
            }
        }
        p.awards?.takeIf { it.isNotEmpty() }?.let { ProfileSection("수상 이력") { LineList(it) } }
        p.images?.takeIf { it.isNotEmpty() }?.let { imgs ->
            ProfileSection("포트폴리오") { PortfolioGrid(imgs, allowSave = isSelf) }
        }
        VideoSection(p)
        ReviewsSection(reviews, onOpenReviews, mineActions)
    }
}

/** 기본 정보 — 활동 지역 · 희망 근무 지역 · 경력(구간 · 연차) · 희망 급여 · 연락처(공개일 때, 누르면 전화) · 지도 가능 분야. */
@Composable
private fun BasicInfoSection(p: PublicProfile) {
    val ctx = LocalContext.current
    val phone = p.contactPhone?.trim()?.takeIf { p.contactVisible == true && it.isNotEmpty() }
    val rows = listOf(
        "활동 지역" to p.activeRegion,
        "희망 근무 지역" to p.desiredRegion,
        "경력" to listOf(ResumeOptions.careerLabel(p.career).takeIf { !p.career.isNullOrBlank() }, p.careerYears)
            .filter { !it.isNullOrBlank() }.joinToString(" · "),
        "희망 급여" to p.desiredSalary?.let { ResumeOptions.salaryLabel(it) },
    )
    val fields = p.fields.orEmpty().joinToString(", ") { ResumeOptions.fieldLabel(it) }
    if (rows.all { it.second.isNullOrBlank() } && phone == null && fields.isBlank()) return
    ProfileSection("기본 정보") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoRows(rows)
            phone?.let { ph ->
                InfoRow("연락처") {
                    Text(
                        "$ph · 전화하기 ›",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp,
                        color = MuyeonColors.primary,
                        modifier = Modifier.clickable {
                            val digits = ph.filter { it.isDigit() }
                            if (digits.isNotEmpty()) runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))) }
                        },
                    )
                }
            }
            InfoRows(listOf("지도 가능 분야" to fields))
        }
    }
}

/** 강사 대표 장르 → 견적 종류 id(ballet·korean …). 모르는 장르면 null(종류 고르기 화면). */
private fun quoteCategoryOf(p: PublicProfile): String? =
    p.genres?.firstNotNullOfOrNull { com.muyeon.domain.model.teacher.LessonGenres.QUOTE_CATEGORY[it] }

/** 라벨 · 값 줄들 — 값이 빈 줄은 그리지 않는다(웹 ProfileRow). */
@Composable
private fun InfoRows(rows: List<Pair<String, String?>>) {
    rows.forEach { (label, value) -> value?.takeIf { it.isNotBlank() }?.let { v -> InfoRow(label) { BodyText(v) } } }
}

/** 한 줄 — 왼쪽 회색 라벨(고정폭 104, 간격 12 — 웹 .dancer-profile-row·iOS 와 같음) + 오른쪽 값. */
@Composable
private fun InfoRow(label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp,
            color = MuyeonColors.textSub, modifier = Modifier.width(104.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) { value() }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
}

/**
 * 여러 줄(줄당 1건) 텍스트 — 자격증·수상(iOS lineList).
 *  ★ 2026-11-08 줄맞춤: 회원이 공백 여러 개로 칸을 맞춰 적은 글(예: "제8회          “     2010.4.25금상")은
 *   글자 폭이 다른 글꼴에서 어긋난다(웹도 공백을 줄여 같은 문제). 줄마다 [이름 | 날짜·결과] 두 칸으로 나눠
 *   오른쪽 칸을 세로로 맞춘다 — 나누는 기준: 공백 2칸 이상, 없으면 'YYYY.M.D' 날짜가 시작하는 곳. 못 나누면 한 칸 그대로.
 */
@Composable
private fun LineList(text: String) {
    val lines = text.replace("\r", "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val cells = splitAligned(line)
            if (cells == null) BodyText(line)
            else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { BodyText(cells.first) }
                Spacer(Modifier.width(12.dp))
                Text(cells.second, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.body, maxLines = 1, softWrap = false)
            }
        }
    }
}

private val MULTI_SPACE = Regex("\\s{2,}")
private val DATE_AT = Regex("(?<!\\d)(19|20)\\d{2}\\.\\s?\\d{1,2}\\.\\s?\\d{1,2}\\.?")

/** 한 줄 → (이름, 날짜·결과). 나눌 곳이 없으면 null. 가운데 칸(“ 같은 반복 표시)은 이름 쪽에 붙인다. */
private fun splitAligned(line: String): Pair<String, String>? {
    val parts = line.split(MULTI_SPACE).filter { it.isNotBlank() }
    if (parts.size >= 2) return parts.dropLast(1).joinToString(" ") to parts.last()
    val m = DATE_AT.find(line) ?: return null
    if (m.range.first == 0) return null
    return line.substring(0, m.range.first).trim() to line.substring(m.range.first).trim()
}

/** 경력 요약 — 최근 2건 + "외 N건" · "경력 X"(iOS careerSummary). */
@Composable
private fun CareerSummary(cs: List<CareerItem>, bucket: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cs.take(2).forEach { c ->
            val head = listOf(c.academy, c.position).filter { it.isNotEmpty() }.joinToString(" ")
            val line = listOf(head, if (c.period.isEmpty()) "" else "(${c.period})").filter { it.isNotEmpty() }.joinToString(" ")
            BodyText(line)
        }
        val more = (cs.size - 2).takeIf { it > 0 }
        val label = bucket?.takeIf { it.isNotEmpty() }?.let { ResumeOptions.careerLabel(it) }
        if (more != null || label != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                more?.let { SubText("외 ${it}건") }
                label?.let { SubText("경력 $it") }
            }
        }
    }
}

@Composable
private fun SubText(text: String) {
    Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub)
}

/** 포트폴리오 — 4열 정사각 그리드 + 마지막 셀 "+N", 탭 → 풀스크린 뷰어(본인만 저장 허용). iOS portfolioGrid. */
@Composable
private fun PortfolioGrid(imgs: List<String>, allowSave: Boolean) {
    val ctx = LocalContext.current
    val visible = imgs.take(4)
    val more = imgs.size - visible.size
    val urls = imgs.mapNotNull { QuoteUi.imageUrl(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        visible.forEachIndexed { idx, path ->
            Box(
                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                    .background(MuyeonColors.tileIdle)
                    .clickable { ImageViewerActivity.start(ctx, JSONArray(urls).toString(), idx, allowSave) },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(QuoteUi.imageUrl(path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), placeholder = com.muyeon.app.ui.components.imageLoadingPainter())
                if (idx == visible.size - 1 && more > 0) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                    Text(
                        "+$more",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = Color.White,
                    )
                }
            }
        }
        repeat(4 - visible.size) { Spacer(Modifier.weight(1f)) }
    }
}

/** 연락처 — 서버가 열람 권한을 판단해 내려준 경우만(꾸민·이미지 레이아웃 공통). */
@Composable
private fun ContactSection(p: PublicProfile) {
    val ctx = LocalContext.current
    // 연락처 — 서버가 열람 권한을 판단해 contactVisible/contactPhone 을 내려준 경우만.
    //  ★ 화면에서 권한을 판정하지 않는다. 번호가 없으면 서버가 허용하지 않은 것이다.
    //  iOS PublicProfileView.contactSection 과 같은 조건/같은 위치(최상단).
    val contactPhone = p.contactPhone?.trim()
    if (p.contactVisible == true && !contactPhone.isNullOrEmpty()) {
        ProfileSection("연락처") {
            Row(
                Modifier.fillMaxWidth().clickable {
                    val digits = contactPhone.filter { it.isDigit() }
                    if (digits.isNotEmpty()) {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")),
                            )
                        }
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    contactPhone,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.Phone, contentDescription = null,
                    tint = MuyeonColors.primary, modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "전화하기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.primary,
                )
            }
        }
    }
}

/**
 * ★ 2026-10-31 영상 포트폴리오 — 카드의 '영상 보유'를 보고 들어와도 상세에 영상이 없었다(웹·iOS 와 같은 자리).
 *  http(s) 링크만 연다(유튜브 앱·브라우저).
 */
@Composable
private fun VideoSection(p: PublicProfile) {
    val ctx = LocalContext.current
    val url = p.videoUrl?.trim()?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) } ?: return
    ProfileSection("영상") {
        Row(
            Modifier.fillMaxWidth().clickable {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "영상 포트폴리오",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.Slideshow, null, tint = MuyeonColors.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "영상 열기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
            )
        }
    }
}

/** 레슨 후기 요약 — 태그 배지(상위 4) + 최근 3개(별 5개·작성자·내용) + 전체보기. iOS reviewsSection. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewsSection(reviews: ReviewList?, onOpenReviews: () -> Unit, mineActions: @Composable () -> Unit = {}) {
    reviews?.items?.takeIf { it.isNotEmpty() }?.let { items ->
        ProfileSection("레슨 후기 ${reviews.count ?: items.size}개") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val top = reviews.tagCounts.entries.sortedByDescending { it.value }.take(4)
                if (top.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        top.forEach { (tag, n) ->
                            Text(
                                "${ReviewOptions.tagLabel(tag)} $n",
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                lineHeight = 14.sp, color = MuyeonColors.primary, maxLines = 1,
                                modifier = Modifier.clip(RoundedCornerShape(50))
                                    .background(MuyeonColors.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.take(3).forEach { r ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { i ->
                                    Icon(
                                        if (i < (r.rating ?: 0)) Icons.Filled.Star else Icons.Outlined.StarBorder, null,
                                        tint = MuyeonColors.primary, modifier = Modifier.size(12.dp),
                                    )
                                }
                                Text(
                                    r.reviewerName ?: "회원",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                                    lineHeight = 14.sp, color = MuyeonColors.hint,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                                if (r.mine == true) {
                                    Spacer(Modifier.weight(1f))
                                    mineActions()
                                }
                            }
                            r.content?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    it, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                    lineHeight = 20.sp, color = MuyeonColors.body, maxLines = 3,
                                )
                            }
                        }
                    }
                }
                if (items.size > 3) {
                    Text(
                        "후기 전체 보기",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clickable(onClick = onOpenReviews),
                    )
                }
            }
        }
    }
}

/**
 * 레슨 가능 시간 — 월~일 × 오전/오후/저녁 도트 그리드(iOS lessonTimeGrid 와 같은 규칙).
 *  행(시간대) 켜짐 = 그 행 버킷 중 하나라도 선택 || 시간대 미선택,
 *  점 켜짐 = 행 켜짐 && (요일 미선택 || 그 요일 선택), 둘 다 비어 있으면 전부 회색.
 *  켜짐 = 주황(primary), 꺼짐 = 회색(border EAEAEA), 8dp 원.
 */
@Composable
private fun LessonTimeGrid(p: PublicProfile) {
    val days = p.availableDays.orEmpty().toSet()
    val slots = p.availableTimeSlots.orEmpty().toSet()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(34.dp))
            ResumeOptions.weekDays.forEach { d ->
                Text(
                    d,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ResumeOptions.gridRows.forEach { (label, keys) ->
            val rowOn = keys.any { it in slots } || slots.isEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(34.dp),
                )
                ResumeOptions.weekDays.forEach { d ->
                    val on = rowOn && (days.isEmpty() || d in days) && !(days.isEmpty() && slots.isEmpty())
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) MuyeonColors.primary else MuyeonColors.border))
                    }
                }
            }
        }
    }
}

/** 섹션 — 위 구분선(F7F7F7, 위아래 14) + 제목(Bold 15) + 내용, 간격 10(iOS section). */
@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        HorizontalDivider(Modifier.padding(vertical = 14.dp), thickness = Dp.Hairline, color = MuyeonColors.tileIdle)
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun BodySection(title: String, text: String) {
    ProfileSection(title) { BodyText(text) }
}

/** 히어로 위 원형 버튼 — 검정 35% 원 + 흰 아이콘(iOS backButton/moreMenu). */
@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** 하단 CTA 버튼 — 채움: 주황 바탕 흰 글자 / 테두리: EAEAEA 선 + 검정 글자 / 비활성: 회색(iOS ctaBar). */
@Composable
private fun CtaButton(
    text: String,
    filled: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    keepFilledWhenDisabled: Boolean = false,
    onClick: () -> Unit,
) {
    val active = enabled || keepFilledWhenDisabled
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp,
        color = when {
            filled -> Color.White
            active -> MuyeonColors.textHead
            else -> MuyeonColors.hint
        },
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .then(
                if (filled) Modifier.background(if (active) MuyeonColors.primary else MuyeonColors.hint)
                else Modifier.border(1.dp, MuyeonColors.border, shape)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
    )
}

/** 신고 사유 선택 시트 — iOS 와 같은 사유·순서·서버 코드. */
private val REPORT_REASONS = listOf(
    "FAKE_RESUME" to "허위 이력서/프로필",
    "INAPPROPRIATE" to "부적절한 내용",
    "SPAM" to "스팸/광고",
    "ETC" to "기타",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportReasonSheet(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "신고 사유를 선택해 주세요",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )
            HorizontalDivider(color = MuyeonColors.border)
            REPORT_REASONS.forEach { (code, label) ->
                Text(
                    label,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(code) }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                )
            }
            Text(
                "취소",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textSub,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 13.dp),
            )
        }
    }
}

/** 섹션의 확인 기관 항목(없으면 null — 섹션은 지금과 같다). */
private fun verified(p: PublicProfile, section: String): List<VerifiedLogo>? = p.badges?.logos.orEmpty().forSection(section)
