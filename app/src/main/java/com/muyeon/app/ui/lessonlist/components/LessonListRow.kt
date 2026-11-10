package com.muyeon.app.ui.lessonlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.lesson.LessonFormat
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.home.HomeLesson

/**
 * 레슨 목록 '목록 보기' 한 줄 — 웹 today-menu-list.basic(썸네일 · 제목 · 가격·장소 · 찜).
 *   ★ 11-10 제목 위 배지 [유형(주황 high)] [난이도(노랑 middle)] [진행 방식…(올리브 row)] — iOS 와 같다(사용자 결정).
 *   값 없는 건 빼고, 하나도 없으면 배지 줄을 그리지 않는다. 넘치면 다음 줄(최대 2줄). 홈 레슨 탭 · 전체보기 공용.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonListRow(lesson: HomeLesson, liked: Boolean, onClick: () -> Unit, onToggleLike: () -> Unit, modifier: Modifier = Modifier) {
    val price = when (val p = lesson.price) {
        null -> stringResource(R.string.home_price_ask)
        0 -> stringResource(R.string.home_price_free)
        else -> stringResource(R.string.home_price_won, HomeLabels.won(p))
    }
    // 유형 — 상세 '레슨 정보'와 같은 문구(정규 / 원데이 / 체험 클래스).
    val kind = when (lesson.lessonKind) {
        "REGULAR" -> stringResource(R.string.lesson_detail_kind_regular)
        "ONEDAY" -> stringResource(R.string.lesson_detail_kind_oneday)
        "EXPERIENCE" -> stringResource(R.string.lesson_detail_kind_experience)
        else -> null
    }
    // 난이도는 iOS 와 같은 매핑(코드 → 한글, 한글·모르는 값은 그대로) · 진행 방식은 상세와 같은 라벨(개인 레슨 · 그룹 레슨 · 학원 · 온라인), 최대 3개.
    val badges = listOfNotNull(
        kind?.let { it to CategoryColors.Level.High },
        lesson.level?.let { levelLabel(it) to CategoryColors.Level.Middle },
    ) + lesson.formats.orEmpty().distinct().take(MAX_FORMATS).map { LessonFormat.label(it) to CategoryColors.Level.Row }
    ListRow(
        title = lesson.title,
        onClick = onClick,
        modifier = modifier,
        sub = listOfNotNull(price, lesson.place).joinToString(" · "),
        thumbUrl = lesson.image,
        thumbFallbackRes = R.drawable.img_noimage,
        trailing = { HeartButton(liked, onToggleLike) },
        tags = if (badges.isEmpty()) null else ({
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
                maxLines = 2,
            ) { badges.forEach { (text, level) -> LevelBadge(text, level) } }
        }),
    )
}

/** 난이도 — iOS 와 같다: beginner 초급 · intermediate 중급 · advanced 고급, 그 밖(한글 등)은 원문. */
private fun levelLabel(v: String): String = when (v.trim().lowercase()) {
    "beginner" -> "초급"
    "intermediate" -> "중급"
    "advanced" -> "고급"
    else -> v
}

private const val MAX_FORMATS = 3
