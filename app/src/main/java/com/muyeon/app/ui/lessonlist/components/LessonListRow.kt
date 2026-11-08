package com.muyeon.app.ui.lessonlist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.domain.model.home.HomeLesson

/** 레슨 목록 '목록 보기' 한 줄 — 웹 today-menu-list.basic(썸네일 · 제목 · 가격·장소 · 찜). */
@Composable
fun LessonListRow(lesson: HomeLesson, liked: Boolean, onClick: () -> Unit, onToggleLike: () -> Unit, modifier: Modifier = Modifier) {
    val price = when (val p = lesson.price) {
        null -> stringResource(R.string.home_price_ask)
        0 -> stringResource(R.string.home_price_free)
        else -> stringResource(R.string.home_price_won, HomeLabels.won(p))
    }
    ListRow(
        title = lesson.title,
        onClick = onClick,
        modifier = modifier,
        sub = listOfNotNull(price, lesson.place).joinToString(" · "),
        thumbUrl = lesson.image,
        thumbFallbackRes = R.drawable.img_noimage,
        trailing = { HeartButton(liked, onToggleLike) },
    )
}
