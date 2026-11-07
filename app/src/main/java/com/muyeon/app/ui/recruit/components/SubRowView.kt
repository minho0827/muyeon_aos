package com.muyeon.app.ui.recruit.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.recruit.RecruitLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.recruit.SubRow

/**
 * 대타 줄 — 웹 SubList 행(사진·찜 없음 · 오른쪽 >): 칩[금액 올림/내림 · 장르 · "하루 · 2타임" · 대상] · 제목(없으면 학원명) ·
 *   "학원 · 지역 · 날짜 시간 · 급여".
 */
@Composable
fun SubRowView(s: SubRow, onClick: () -> Unit) {
    val salary = HomeLabels.salary(s.salary)?.let { stringResource(it) } ?: s.pay ?: stringResource(R.string.recruit_pay_negotiable)
    val date = listOfNotNull(RecruitLabels.tbd(s.classDate) ?: stringResource(R.string.recruit_tbd), s.classTime).joinToString(" ")
    val sub = (listOfNotNull(s.academy, s.region) + date + salary).joinToString(" · ")
    ListRow(
        title = s.title ?: s.academy.orEmpty(),
        onClick = onClick,
        sub = sub,
        tags = {
            if (s.payChange != 0) Pill(stringResource(if (s.payChange > 0) R.string.recruit_pay_up else R.string.recruit_pay_down), CategoryColors.Boosted)
            s.genre?.let { Pill(it, CategoryColors.genre(it)) }
            val sched = listOfNotNull(RecruitLabels.schedule(s.scheduleType)?.let { stringResource(it) },
                s.classCount?.takeIf { it > 0 }?.let { stringResource(R.string.recruit_class_count, it) }).joinToString(" · ")
            if (sched.isNotEmpty()) Pill(sched, CategoryColors.FeedSub)
            RecruitLabels.target(s.target)?.let { Pill(stringResource(it), CategoryColors.Default) }
        },
        trailing = { Icon(painterResource(R.drawable.ic_web_arrow_right_bold), null, tint = AppColors.TextHint, modifier = Modifier.size(18.dp)) },
    )
}
