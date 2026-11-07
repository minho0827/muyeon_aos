package com.muyeon.app.ui.lessons.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.CardTags
import com.muyeon.app.ui.components.DmButton
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.components.TutorialAnchors
import com.muyeon.app.ui.components.tutorialAnchor
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.teacher.TeacherCard

/**
 * 강사 카드 — 웹 LessonTab today-menu-list .menu-card. 사진(그리드 79:64 · 목록 2:1, 얼굴 쪽 위 15%) + 오른쪽 위 하트(흰 원) ·
 *   이름 / "{장르} 레슨" · 오른쪽 DM(일반 회원) · 📍 지역 · 복숭아 태그(예시 프로필 · 분야 · 경력, 최대 3).
 *   튜토리얼 앵커: card · heart · quote(웹 data-tut).
 */
@Composable
fun LessonTeacherCard(
    teacher: TeacherCard,
    grid: Boolean,
    liked: Boolean,
    showDm: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
    anchors: TutorialAnchors? = null,
) {
    val t = teacher
    val fieldNames = t.fields.mapNotNull { f -> LessonLabels.field(f)?.let { stringResource(it) } ?: f.takeIf { it.isNotBlank() } }
    val fields = fieldNames.ifEmpty { t.genres }
    val tags = listOfNotNull(
        if (t.isDemoProfile) stringResource(R.string.lesson_demo_profile) else null,
    ) + fields + listOfNotNull(LessonLabels.career(t.career)?.let { stringResource(it) })
    val primary = t.genres.firstOrNull() ?: fields.firstOrNull()
    val shape = RoundedCornerShape(Dimensions.cardRadius)

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AppColors.Divider, shape)
            .clickable(onClick = onClick)
            .tutorialAnchor(anchors, "card")
            .testTag("teacher_card_${t.id}"),
    ) {
        Box {
            RemoteImage(
                url = t.image,
                fallbackRes = R.drawable.img_noimage_teacher,
                thumbnail = true,
                alignment = BiasAlignment(0f, -0.7f),
                modifier = Modifier.fillMaxWidth().aspectRatio(if (grid) 79f / 64f else 2f),
            )
            HeartButton(
                liked = liked, onToggle = onToggleLike, size = 20.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .tutorialAnchor(anchors, "heart")
                    .shadow(4.dp, CircleShape)
                    .background(AppColors.OverlayWhite, CircleShape),
            )
        }
        Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                Column(Modifier.weight(1f)) {
                    Text(t.name.ifBlank { stringResource(R.string.lesson_teacher_default) }, style = AppTypography.CardTitle16, color = AppColors.TextHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        primary?.let { stringResource(R.string.lesson_genre_lesson, it) } ?: stringResource(R.string.lesson_teacher_generic),
                        style = AppTypography.Sub13, color = AppColors.TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showDm) DmButton(onClick = onRequest, enabled = !t.isDemoProfile, modifier = Modifier.tutorialAnchor(anchors, "quote"))
            }
            // 지역 줄은 값이 없어도 자리를 남긴다(카드 높이 맞춤).
            com.muyeon.app.ui.components.SlotText(t.region?.let { "📍 $it" }, AppTypography.Sub13, AppColors.TextSub)
            CardTags(tags)
        }
    }
}
