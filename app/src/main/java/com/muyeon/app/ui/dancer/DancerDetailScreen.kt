package com.muyeon.app.ui.dancer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.components.ActionSheet
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.ImageCarousel
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ReadArea
import com.muyeon.app.ui.components.ReadField
import com.muyeon.app.ui.components.ReadSectionTitle
import com.muyeon.app.ui.components.ReportSheet
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.dancer.components.LessonTimeGrid
import com.muyeon.app.ui.resume.RecruitChatLimitDialog
import com.muyeon.app.ui.resume.RecruitChatLimitException
import com.muyeon.app.ui.resume.ResumeOptions
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * 무용수 상세 — 웹 /dancers/:id. 사진 · 배지·이름·한 줄 소개·지역 · 찜 / 소개글 · 기본 정보 · 레슨 가능 시간 · 경력 · 레슨 후기(전체 보기).
 *   아래 버튼: 공연팀 [캐스팅 제안 보내기], 학원·강사 [채팅하기]. 맨 아래 허위 프로필 신고 · 차단.
 */
@Composable
fun DancerDetailScreen(
    viewModel: DancerDetailViewModel,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenImage: (List<String>, Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    onReviews: () -> Unit,
    onOpenChat: (Long, String) -> Unit,
    onMembership: () -> Unit,
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var limit by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is DancerEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                is DancerEvent.MessageRes -> Toast.makeText(context, e.res, Toast.LENGTH_SHORT).show()
                DancerEvent.LoginRequired -> onLogin()
                is DancerEvent.OpenChat -> onOpenChat(e.roomId, e.title)
                DancerEvent.ChatLimit -> limit = true
                DancerEvent.Blocked -> { Toast.makeText(context, R.string.dancer_blocked, Toast.LENGTH_SHORT).show(); onBack() }
                else -> Unit
            }
        }
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.dancer_detail_title), primary = false, onBack = onBack)
        val d = s.profile
        when {
            s.isLoading -> Unit
            d == null -> EmptyView(s.errorMessage ?: stringResource(R.string.dancer_not_found),
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ImageCarousel(d.images, onOpen = { onOpenImage(d.images, it) }, fallbackRes = R.drawable.img_noimage_teacher)
                    Column(Modifier.fillMaxWidth().padding(Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
                                d.major?.let { LevelBadge(it, CategoryColors.Level.High) }
                                d.gender?.let { LevelBadge(it, CategoryColors.Level.Middle) }
                                if (d.hasVideo) LevelBadge(stringResource(R.string.dancer_video), CategoryColors.Level.Row)
                                if (d.awards != null) LevelBadge(stringResource(R.string.dancer_award), CategoryColors.Level.Row)
                            }
                            HeartButton(s.liked, viewModel::toggleLike, size = Dimensions.heartCircle)
                        }
                        Text(d.name.ifBlank { stringResource(R.string.dancer_default_name) }, style = AppTypography.Title20, color = AppColors.TextHead)
                        d.oneLiner?.let { Text(it, style = AppTypography.Body14, color = AppColors.TextSub) }
                        d.region?.let { Text(it, style = AppTypography.Sub13, color = AppColors.TextHint) }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL)) {
                        if (d.intro != null) {
                            ReadSectionTitle(stringResource(R.string.dancer_sec_intro))
                            ReadArea(d.intro)
                        }
                        ReadSectionTitle(stringResource(R.string.dancer_sec_basic))
                        ReadField(stringResource(R.string.dancer_birth), d.birth)
                        ReadField(stringResource(R.string.dancer_major), d.major)
                        ReadField(stringResource(R.string.dancer_body), listOfNotNull(d.gender, d.height, d.weight).joinToString(" · ").ifBlank { null })
                        ReadField(stringResource(R.string.dancer_region), d.region)
                        ReadArea(d.education, label = stringResource(R.string.dancer_education))
                        if (d.availableDays.isNotEmpty() || d.availableTimeSlots.isNotEmpty()) {
                            ReadSectionTitle(stringResource(R.string.dancer_sec_lesson_time))
                            LessonTimeGrid(d.availableDays, d.availableTimeSlots)
                        }
                        val career = d.career?.let(ResumeOptions::careerLabel)?.takeIf { it.isNotBlank() }
                        if (listOf(career, d.companyCareer, d.awards, d.performances, d.videoUrl).any { it != null }) {
                            ReadSectionTitle(stringResource(R.string.dancer_sec_career))
                            ReadField(stringResource(R.string.dancer_career), career)
                            ReadArea(d.companyCareer, label = stringResource(R.string.dancer_company_career))
                            ReadArea(d.awards, label = stringResource(R.string.dancer_awards))
                            ReadArea(d.performances, label = stringResource(R.string.dancer_performances))
                            d.videoUrl?.let { url -> ReadField(stringResource(R.string.dancer_video_portfolio), url, linkText = stringResource(R.string.dancer_video_open), onLink = { onOpenUrl(url) }) }
                        }
                        ReadSectionTitle(stringResource(R.string.dancer_sec_reviews))
                        Text(stringResource(R.string.dancer_reviews_all), style = AppTypography.Body14SemiBold, color = AppColors.Primary,
                            modifier = Modifier.clickable(onClick = onReviews).padding(vertical = Dimensions.spacingXS))
                        Row(Modifier.fillMaxWidth().padding(vertical = Dimensions.spacingXL), horizontalArrangement = Arrangement.Center) {
                            Text(stringResource(R.string.dancer_report), style = AppTypography.Sub13, color = AppColors.TextSub,
                                modifier = Modifier.clickable { viewModel.showReport(true) }.padding(Dimensions.spacingS))
                            if (!d.self) Text(stringResource(R.string.dancer_block), style = AppTypography.Sub13, color = AppColors.TextSub,
                                modifier = Modifier.clickable { viewModel.askBlock(true) }.padding(Dimensions.spacingS))
                        }
                    }
                }
                MuyeonBottomButton(
                    stringResource(if (s.isTeam) R.string.dancer_offer else R.string.dancer_chat), "dancer.cta", loading = s.busy,
                ) { if (s.isTeam) viewModel.startOffer() else viewModel.askChat(true) }
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
    if (limit) RecruitChatLimitDialog(RecruitChatLimitException(null, null, null, null), onUpgrade = { limit = false; onMembership() }, onDismiss = { limit = false })
}
