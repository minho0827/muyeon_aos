package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.home.HomeActions
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.home.TeacherHomeUiState
import com.muyeon.app.ui.home.TeacherHomeViewModel
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.app.ui.theme.PillColors
import com.muyeon.domain.model.home.TeacherHomePosting

/** 강사 홈 본문 — 웹 TeacherHome 순서. */
@Composable
fun TeacherHomeContent(state: TeacherHomeUiState, vm: TeacherHomeViewModel, actions: HomeActions, modifier: Modifier = Modifier) {
    val name = state.name ?: stringResource(R.string.pending_role_member)
    val w = slideWidth(0.7f)
    val p = state.preview
    val card: @Composable (TeacherHomePosting, Boolean, Modifier) -> Unit = { x, match, m ->
        TeacherHomePostingSlide(x, match, x.id in state.liked[x.kind].orEmpty(), { actions.openPath("/${pathOf(x.kind)}/${x.id}") }, { vm.toggleLike(x.kind, x.id) }, m)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimensions.sectionGap)) {
        Text(stringResource(R.string.home_teacher_heading), style = AppTypography.Title20, color = AppColors.TextHead, modifier = Modifier.padding(start = 20.dp))
        when {
            state.loading -> Text(stringResource(R.string.home_teacher_loading), style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.padding(horizontal = 20.dp))
            state.error != null -> Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.error, style = AppTypography.Body14, color = AppColors.TextBody)
                SheetButton(SheetAction(stringResource(R.string.common_retry), false) { vm.load() })
            }
        }
        if (p != null) {
            TeacherSection(stringResource(R.string.home_teacher_academies, name), p.academies, w, { actions.openPath("/jobs") }, stringResource(R.string.home_teacher_academies_empty)) { x, m -> card(x, true, m) }
            TeacherSection(stringResource(R.string.home_teacher_postings, name), p.postings, w, null, stringResource(R.string.home_teacher_postings_empty)) { x, m -> card(x, false, m) }
            TeacherSection(stringResource(R.string.home_teacher_urgent), p.urgent, w, { actions.openPath("/subs") }, stringResource(R.string.home_teacher_urgent_empty)) { x, m -> card(x, false, m) }
        }
        HomeQuoteRequests(
            state.quotes, state.likedQuotes,
            onOpen = { q -> actions.openPath(if (q.isMine) "/myQuotes/${q.id}" else "/availableQuotes/${q.id}" + if (!q.canRespond) "?sent=1" else "") },
            onToggleLike = vm::toggleQuoteLike, onMore = { actions.openPath("/availableQuotes") },
        )
    }
}

@Composable
private fun TeacherSection(title: String, items: List<TeacherHomePosting>, w: androidx.compose.ui.unit.Dp, onMore: (() -> Unit)?, empty: String, card: @Composable (TeacherHomePosting, Modifier) -> Unit) {
    HomeCarouselSection(title, items, { "${it.kind}-${it.id}" }, w, onMore = onMore,
        empty = { Text(empty, style = AppTypography.Body14, color = AppColors.TextSub) }, card = card)
}

private fun pathOf(kind: String) = when (kind) { "SUB" -> "subs"; "CASTING" -> "casting"; else -> "jobs" }

private fun colorsOf(kind: String): PillColors = when (kind) { "SUB" -> CategoryColors.FeedSub; "CASTING" -> CategoryColors.FeedCasting; else -> CategoryColors.FeedJob }

/** 강사 홈 카드 — 웹 postingSlide: 사진 3:2 · 하트 · [종류](+[장르]) · 제목 · 보조 두 줄 · 3일 안이면 "🕒 신규". */
@Composable
fun TeacherHomePostingSlide(x: TeacherHomePosting, showMatch: Boolean, liked: Boolean, onClick: () -> Unit, onToggleLike: () -> Unit, modifier: Modifier = Modifier) {
    val c = colorsOf(x.kind)
    HomeCard(
        imageUrl = x.image, fallbackRes = R.drawable.img_noimage, onClick = onClick, modifier = modifier, image = HomeImageSize.Ratio(3f / 2f),
        overlay = {
            HeartButton(liked, onToggleLike, size = 20.dp, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).shadow(4.dp, CircleShape).background(AppColors.OverlayWhite, CircleShape))
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(stringResource(when (x.kind) { "SUB" -> R.string.home_kind_sub; "CASTING" -> R.string.home_kind_casting; else -> R.string.home_kind_teacher_job }), c)
            if (showMatch) x.genre?.let { Pill(it, c) }
        }
        HomeCardTitle(x.title)
        x.sub?.let { HomeCardSub(it) }
        x.info?.let { HomeCardSub(it) }
        if (HomeLabels.isRecent(x.createdAt)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_web_clock), null, tint = CategoryColors.NewLabel, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.home_new), style = AppTypography.Caption12, color = CategoryColors.NewLabel)
            }
        }
    }
}
