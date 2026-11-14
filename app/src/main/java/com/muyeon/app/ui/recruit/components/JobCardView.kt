package com.muyeon.app.ui.recruit.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.CardTags
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.components.SlotText
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.recruit.RecruitLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.recruit.JobCard

/**
 * 구인 카드 — 웹 JobBoard hire 카드: 사진(그리드 79:64 · 목록 2:1) · 왼쪽 위 '신규'(3일) · 왼쪽 아래 마감(D-n · 3일 이하 빨강) ·
 *   오른쪽 위 찜 · 제목 · 📍지역 · ⏰요일/시간 · ₩급여 · 태그 3(지원함/열람함 · 장르 · 직무 · 급여 · 분야).
 */
@Composable
fun JobCardView(card: JobCard, grid: Boolean, status: String?, liked: Boolean, onClick: () -> Unit, onLike: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().clip(shape).border(1.dp, AppColors.Divider, shape).clickable(onClick = onClick)) {
        Box {
            val frame = Modifier.fillMaxWidth().aspectRatio(if (grid) 79f / 64f else 2f)
            // ★ 2026-11-14 사진 없는 공고 — 장르별 '○○ 강사 구해요!' 그림. 사진을 못 불러와도 같은 그림(깨진 칸을 보이지 않는다).
            val fallback = RecruitLabels.jobThumb(card.genre)
            if (card.image.isNullOrBlank()) {
                Image(painterResource(fallback), null, frame, contentScale = ContentScale.Crop)
            } else {
                RemoteImage(card.image, frame, fallbackRes = fallback, thumbnail = true)
            }
            if (HomeLabels.isRecent(card.createdAt)) Badge(stringResource(R.string.home_new), NEW_BG, Modifier.align(Alignment.TopStart).padding(10.dp))
            RecruitLabels.deadline(card.deadline)?.let { d ->
                Badge(if (d.days > 0) stringResource(d.res, d.days) else stringResource(d.res), if (d.urgent) URGENT_BG else DIM_BG,
                    Modifier.align(Alignment.BottomStart).padding(10.dp))
            }
            HeartButton(liked, onLike, size = 20.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).shadow(4.dp, CircleShape).background(AppColors.OverlayWhite, CircleShape))
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // 제목 2줄 · 지역 · 요일/시간 · 급여 — 값이 없어도 자리를 남긴다(카드 높이 맞춤).
            SlotText(card.title, AppTypography.CardTitle16, AppColors.TextHead, lines = 2)
            Line(card.region?.let { "📍 $it" })
            Line(listOfNotNull(card.days, card.time).takeIf { it.isNotEmpty() }?.let { "⏰ " + it.joinToString(" · ") })
            Line(card.pay?.let { "₩ $it" })
            val salary = HomeLabels.salary(card.salary)?.let { stringResource(it) }
            val fields = card.fields.map { f -> LessonLabels.field(f)?.let { stringResource(it) } ?: f }
            CardTags(listOfNotNull(status, card.genre, card.job, salary) + fields, Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Line(text: String?) = SlotText(text, AppTypography.Sub13, AppColors.TextSub)

@Composable
private fun Badge(text: String, bg: Color, modifier: Modifier) =
    Text(text, style = AppTypography.Caption12, color = Color.White, modifier = modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp))

// 웹 JobBoard NewBadge · DeadlineBadge 색.
private val NEW_BG = Color(0xE6F58232)
private val URGENT_BG = Color(0xEBE2362D)
private val DIM_BG = Color.Black.copy(alpha = 0.55f)
