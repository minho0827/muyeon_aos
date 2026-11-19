package com.muyeon.app.ui.dancer

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.components.ActionSheet
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.ReportSheet
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.resume.BodyText
import com.muyeon.app.ui.resume.CircleIconButton
import com.muyeon.app.ui.resume.CredentialBadgeRow
import com.muyeon.app.ui.resume.CredentialBadges
import com.muyeon.app.ui.resume.CtaButton
import com.muyeon.app.ui.resume.GrayChip
import com.muyeon.app.ui.resume.InfoRows
import com.muyeon.app.ui.resume.LineList
import com.muyeon.app.ui.resume.PortfolioGrid
import com.muyeon.app.ui.resume.ProfileCtaBar
import com.muyeon.app.ui.resume.ProfileFavoriteButton
import com.muyeon.app.ui.resume.ProfileHeroImage
import com.muyeon.app.ui.resume.ProfileLessonTimeGrid
import com.muyeon.app.ui.resume.ProfileSection
import com.muyeon.app.ui.resume.ProfileVideoSection
import com.muyeon.app.ui.resume.RecruitChatLimitDialog
import com.muyeon.app.ui.resume.RecruitChatLimitException
import com.muyeon.app.ui.resume.ResumeOptions
import com.muyeon.app.ui.resume.VerifiedLogoRow
import com.muyeon.domain.model.dancer.DancerProfile
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 무용수 상세 — 웹 /dancers/:id. ★ 2026-11-15 강사 상세(PublicProfileScreen)와 같은 모양으로 바꿨다(부품: ui/resume/ProfileDetailParts.kt).
 *   히어로 사진(뒤로·더보기 동그라미 버튼) · 이름·찜 · 확인 배지 · 지역/장르/성별 칩 · 한 줄 소개
 *   / 소개글 · 기본 정보 · 레슨 가능 시간 · 경력 · 학력 · 경력·자격 · 공연 이력 · 수상 이력 · 포트폴리오 · 영상 · 레슨 후기.
 *   아래 버튼: 공연팀 [캐스팅 제안 보내기], 학원·강사 [채팅하기]. 신고·차단은 오른쪽 위 더보기(로그인한 회원만).
 *   ★ 2026-11-16 레슨 받는 무용수(서버 lessonProvider)면 그 밖 회원·비회원에게 강사 상세와 같은 레슨 버튼([DancerLessonCtaBar]).
 *   소개만 받은 응답(limited — 일반회원·무용수·비회원)이면 키·몸무게 등이 없어 그 줄이 빠지고, 아래 버튼도 그리지 않는다.
 */
@Composable
fun DancerDetailScreen(
    viewModel: DancerDetailViewModel,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenImage: (List<String>, Int) -> Unit,
    /** 영상 링크는 강사 상세와 같은 부품(ProfileVideoSection)이 직접 연다 — 부르는 쪽 호환으로 남겨 둔다. */
    @Suppress("UNUSED_PARAMETER") onOpenUrl: (String) -> Unit,
    onReviews: () -> Unit,
    onOpenChat: (Long, String) -> Unit,
    onMembership: () -> Unit,
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var limit by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    // ★ 2026-11-16 유형이 바뀌면(MY 두 번 누르기 등) 버튼·공개 범위를 다시 맞춘다 — 한 번만 읽던 잔상 제거.
    val activeType = com.muyeon.app.ui.common.rememberActiveType()
    LaunchedEffect(activeType) { viewModel.onTypeChanged(activeType) }
    val gate = com.muyeon.app.ui.common.rememberTypeGate()
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is DancerEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                is DancerEvent.MessageRes -> Toast.makeText(context, e.res, Toast.LENGTH_SHORT).show()
                DancerEvent.LoginRequired -> onLogin()
                is DancerEvent.OpenChat -> onOpenChat(e.roomId, e.title)
                DancerEvent.ChatLimit -> limit = true
                DancerEvent.Blocked -> { Toast.makeText(context, R.string.dancer_blocked, Toast.LENGTH_SHORT).show(); onBack() }
                DancerEvent.Unscrapped -> { Toast.makeText(context, R.string.common_scrap_removed, Toast.LENGTH_SHORT).show(); onBack() }
                else -> Unit
            }
        }
    }
    // 위 바(MainTopBar)가 없어졌다 — 흰 바탕에 맞게 상태바 아이콘을 어둡게(MainTopBar primary=false 와 같다).
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            (view.context as? android.app.Activity)?.window?.let { w ->
                androidx.core.view.WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = true
            }
            onDispose { }
        }
    }
    val d = s.profile
    Box(Modifier.fillMaxSize().background(MuyeonColors.surface).statusBarsPadding().navigationBarsPadding()) {
        when {
            s.isLoading -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
            // ★ 2026-11-19 탈퇴한 무용수 — 다시 시도 대신 찜 빼기
            d == null && s.withdrawn -> Column(Modifier.fillMaxSize().padding(top = 52.dp)) {
                EmptyView(stringResource(R.string.member_withdrawn_title), content = stringResource(R.string.member_withdrawn_desc),
                    buttonText = stringResource(R.string.member_withdrawn_unscrap), onButton = viewModel::unscrapWithdrawn)
            }
            d == null -> Column(Modifier.fillMaxSize().padding(top = 52.dp)) {
                EmptyView(s.errorMessage ?: stringResource(R.string.dancer_not_found),
                    buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            }
            else -> Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ProfileHeroImage(
                        d.images.firstOrNull(),
                        stringResource(R.string.dancer_title, d.major ?: stringResource(R.string.dancer_major_default)),
                        onClick = { onOpenImage(d.images, 0) },
                    )
                    DancerHeader(d, liked = s.liked, showLike = !s.isSelf, onLike = viewModel::toggleLike)
                    DancerSections(d, isSelf = s.isSelf, onReviews = onReviews)
                    Spacer(Modifier.height(20.dp))
                }
                // 하단 CTA — 공연팀·학원·강사가 전체 프로필을 볼 때만(소개만·내 프로필이면 없음).
                val lessonCta = d.lessonCta(s.isSelf)
                if (!d.limited && s.canContact && !s.isSelf) {
                    ProfileCtaBar {
                        CtaButton(
                            stringResource(if (s.isTeam) R.string.dancer_offer else R.string.dancer_chat),
                            filled = true, enabled = !s.busy, keepFilledWhenDisabled = true, modifier = Modifier.weight(1f),
                        ) {
                            // 필요 유형표 + 공통 관문 — 버튼은 지금 유형 기준으로 그려졌으므로 보통 바로 지난다.
                            if (s.isTeam) gate.require(com.muyeon.domain.model.member.TypeFeature.CASTING_OFFER) { viewModel.startOffer() }
                            else gate.require(com.muyeon.domain.model.member.TypeFeature.RECRUIT_CHAT) { viewModel.askChat(true) }
                        }
                    }
                } else if (lessonCta != null) {
                    // ★ 2026-11-16 레슨 받는 무용수 — 강사 상세와 같은 [문의하기]·[레슨 문의]/[받은 견적 보기](소개만 보는 회원·비회원도).
                    DancerLessonCtaBar(
                        lessonCta,
                        onChat = { rid -> onOpenChat(rid, d.name) },
                        onReceived = { qid -> com.muyeon.app.ui.quote.LessonInquiry.openReceived(context, qid.toInt()) },
                        // ★ 2026-11-16 지금 유형과 상관없이 문의한다(고객 쪽 유형 관문 제거).
                        onRequest = {
                            scope.launch { com.muyeon.app.ui.quote.LessonInquiry.start(context, d.id.toInt(), com.muyeon.app.ui.quote.LessonInquiry.categoryOf(d.genres)) }
                        },
                    )
                }
            }
        }

        // 뒤로 / ⋯ — 스크롤과 무관하게 화면 위에 고정(강사 상세와 같다).
        CircleIconButton(
            Icons.AutoMirrored.Filled.ArrowBack, "뒤로",
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 8.dp),
            onClick = onBack,
        )
        if (d != null && s.loggedIn && !s.isSelf) {
            Box(Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 8.dp)) {
                CircleIconButton(Icons.Filled.MoreHoriz, "더보기", iconSize = 18.dp) { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dancer_report), fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.danger) },
                        onClick = { menuOpen = false; viewModel.showReport(true) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dancer_block), fontFamily = customFontFamily, fontSize = 14.sp) },
                        onClick = { menuOpen = false; viewModel.askBlock(true) },
                    )
                }
            }
        }
    }
    if (s.pickCall) ActionSheet(
        title = stringResource(R.string.dancer_pick_call),
        actions = s.castingCalls.map { c ->
            SheetAction(listOfNotNull(c.title, c.genre, c.region).joinToString(" · "), primary = false, onClick = { viewModel.sendOffer(c.id) })
        } + SheetAction(stringResource(R.string.dancer_offer_without_call), primary = true, onClick = { viewModel.sendOffer(null) }),
        onDismiss = viewModel::closePick,
    )
    if (s.report) ReportSheet(busy = s.busy, onSubmit = viewModel::sendReport, onDismiss = { viewModel.showReport(false) })
    CustomDialog(
        title = stringResource(R.string.dancer_chat_q), content = stringResource(R.string.dancer_chat_body),
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.dancer_chat_start),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askChat(false) }, onLeftButtonClick = { viewModel.askChat(false) },
        onRightButtonClick = viewModel::startChat, showPopup = s.confirmChat,
    )
    CustomDialog(
        title = stringResource(R.string.dancer_block_q, s.profile?.name.orEmpty()), content = stringResource(R.string.dancer_block_body),
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.dancer_block),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askBlock(false) }, onLeftButtonClick = { viewModel.askBlock(false) },
        onRightButtonClick = viewModel::blockUser, showPopup = s.confirmBlock,
    )
    s.doneMessage?.let { m ->
        CustomDialog(
            title = stringResource(m), content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
            buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = viewModel::dismissDone, onRightButtonClick = viewModel::dismissDone, showPopup = true,
        )
    }
    com.muyeon.app.ui.common.TypeGateHost(gate)
    if (limit) RecruitChatLimitDialog(RecruitChatLimitException(null, null, null, null), onUpgrade = { limit = false; onMembership() }, onDismiss = { limit = false })
}

/** 이름·찜 · 확인 배지 · 칩(지역·장르·성별) · 확인 기관 로고 · 한 줄 소개 — 강사 상세 본문 머리와 같은 크기·간격. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DancerHeader(d: DancerProfile, liked: Boolean, showLike: Boolean, onLike: () -> Unit) {
    val badges = remember(d.badgesJson) { d.badgesJson?.let { runCatching { CredentialBadges.from(JSONObject(it)) }.getOrNull() } }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                d.name.ifBlank { stringResource(R.string.dancer_default_name) },
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                lineHeight = 26.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            // ★ 2026-11-16 이름(weight 1f, fill=false)과 빈칸(weight 1f)이 남는 폭을 반씩 나눠 하트가 가운데쯤 떴다 — 이름이 다 가져가게.
            if (showLike) ProfileFavoriteButton(liked, onLike)
        }
        CredentialBadgeRow(badges)
        val genreChip = d.genres.takeIf { it.isNotEmpty() }?.take(2)?.joinToString(", ") ?: d.major
        val chips = listOfNotNull(d.region, genreChip, d.gender)
        if (chips.isEmpty()) {
            Spacer(Modifier.height(8.dp))
        } else {
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) { chips.forEach { GrayChip(it) } }
        }
        badges?.logos?.takeIf { it.isNotEmpty() }?.let { VerifiedLogoRow(it, Modifier.padding(top = 10.dp)) }
        d.oneLiner?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 20.sp, color = MuyeonColors.textSub,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** 본문 섹션 — 강사 상세 ProfileSections 와 같은 섹션 틀·글자. 값이 없는 줄·섹션은 그리지 않는다(소개만 응답이면 저절로 빠진다). */
@Composable
private fun DancerSections(d: DancerProfile, isSelf: Boolean, onReviews: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        d.intro?.replace("\r", "")?.takeIf { it.isNotEmpty() }?.let {
            ProfileSection(stringResource(R.string.dancer_sec_intro)) {
                Text(it, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp, color = MuyeonColors.body)
            }
        }
        val basic = listOf(
            stringResource(R.string.dancer_major) to (d.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: d.major),
            stringResource(R.string.dancer_birth) to d.birth,
            stringResource(R.string.dancer_body) to listOfNotNull(d.gender, d.height, d.weight).joinToString(" · ").ifBlank { null },
            stringResource(R.string.dancer_region) to d.region,
            stringResource(R.string.dancer_career) to d.career?.let(ResumeOptions::careerLabel)?.takeIf { it.isNotBlank() },
        )
        if (basic.any { !it.second.isNullOrBlank() }) {
            ProfileSection(stringResource(R.string.dancer_sec_basic)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { InfoRows(basic) }
            }
        }
        if (d.availableDays.isNotEmpty() || d.availableTimeSlots.isNotEmpty()) {
            ProfileSection(stringResource(R.string.dancer_sec_lesson_time)) {
                ProfileLessonTimeGrid(d.availableDays.map { it.take(1) }, d.availableTimeSlots)
            }
        }
        d.careers?.let { ProfileSection(stringResource(R.string.dancer_sec_career)) { Lines(it) } }
        d.education?.let { ProfileSection(stringResource(R.string.dancer_education)) { Lines(it) } }
        d.companyCareer?.let { cc ->
            ProfileSection("경력 · 자격") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { InfoRows(listOf("무용단 경력" to cc)) }
            }
        }
        d.performances?.let { ProfileSection("공연 이력") { Lines(it) } }
        d.awards?.let { ProfileSection("수상 이력") { LineList(it) } }
        d.images.drop(1).takeIf { it.isNotEmpty() }?.let { imgs ->
            ProfileSection("포트폴리오") { PortfolioGrid(imgs, allowSave = isSelf) }
        }
        ProfileVideoSection(d.videoUrl)
        // ★ 2026-11-16 강사 상세와 같은 후기 칸 — 후기가 있을 때만 '레슨 후기 N개'(예전엔 0개여도 '후기 전체 보기' 줄을 늘 그렸다).
        val context = LocalContext.current
        var reviews by remember(d.id) { mutableStateOf<com.muyeon.app.ui.review.ReviewList?>(null) }
        LaunchedEffect(d.id, com.muyeon.app.auth.accountEpoch()) {
            com.muyeon.app.ui.review.ReviewApi(com.muyeon.app.utils.TokenManager.getAccessToken(context))
                .list(d.id.toInt()).onSuccess { reviews = it }
        }
        com.muyeon.app.ui.resume.ReviewsSection(reviews, onReviews)
    }
}

/** 줄바꿈으로 이어 붙인 글 → 한 줄씩(강사 상세 학력·공연 이력과 같은 글자, 간격 6). */
@Composable
private fun Lines(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.replace("\r", "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }.forEach { BodyText(it) }
    }
}

/** ★ 2026-11-16 레슨 버튼 재료 — 무용수 레슨 추가 기능(DancerLessonProviderAddon). */
data class DancerLessonCta(
    /** 이미 있는 채팅방 — 있으면 [문의하기]. */
    val chatRoomId: Long?,
    /** 내가 보낸 요청에 답이 왔다 — [받은 견적 보기]. */
    val receivedQuoteId: Long?,
)

/** 서버가 레슨 받는 무용수라고 할 때만(내 프로필 아님). 강사 상세(PublicProfileScreen) CTA 와 같은 규칙. */
fun DancerProfile.lessonCta(isSelf: Boolean): DancerLessonCta? =
    if (!lessonProvider || isSelf) null
    else DancerLessonCta(chatRoomId?.takeIf { it > 0 }, myQuote?.takeIf { it.hasResponse && it.quoteId > 0 }?.quoteId)

/** 레슨 버튼 줄 — [문의하기](방이 있을 때) + [레슨 문의] 또는 [받은 견적 보기]. */
@Composable
fun DancerLessonCtaBar(cta: DancerLessonCta, onChat: (Long) -> Unit, onReceived: (Long) -> Unit, onRequest: () -> Unit) {
    ProfileCtaBar {
        cta.chatRoomId?.let { rid ->
            CtaButton(stringResource(R.string.dancer_lesson_chat), filled = false, modifier = Modifier.weight(1f)) { onChat(rid) }
        }
        val received = cta.receivedQuoteId
        CtaButton(
            stringResource(if (received != null) R.string.dancer_lesson_received else R.string.dancer_lesson_inquiry),
            filled = true, modifier = Modifier.weight(1f),
        ) { if (received != null) onReceived(received) else onRequest() }
    }
}
