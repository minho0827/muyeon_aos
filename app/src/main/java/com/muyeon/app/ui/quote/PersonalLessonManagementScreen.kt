package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.lesson.LessonManageScreen
import com.muyeon.app.ui.lesson.LessonProductApi

/**
 * 「개인레슨 관리」 — 강사·학원 원장이 개인레슨 업무를 한 화면에서 처리한다.
 *  iOS `PersonalLessonManagementView.swift` 이식.
 *
 * 전에는 카드 그리드 허브(QuoteDashboardScreen)에서 기능을 골라 다시 들어가야 했다.
 *  카드 화면은 아무 일도 하지 않으면서 한 번 더 누르게만 만든다. 게다가 같은 대화가
 *  '받은 제안'과 '보낸 제안' 두 카드로 쪼개져 있었다. 중간 화면을 없애고 세 탭으로 묶는다.
 *    수강생 상담 — 수강생 문의 + 내가 보낸 제안을 한 목록에서
 *    수강생 찾기 — 레슨을 원하는 수강생의 공개 요청에서 바로 제안
 *    레슨 소개  — 내가 등록한 레슨에서 바로 수정·개설
 *
 * ⚠️ iOS 와 달리 좌우 스와이프로 탭을 넘기지 않는다(HorizontalPager 미사용).
 *   '수강생 찾기' 안의 장르·지역 칩이 가로 스크롤인데(지역은 개수가 가변이라 한 줄 고정 불가),
 *   가로로 페이징되는 컨테이너 안에 가로 스크롤 면을 두면 같은 축의 제스처가 겹친다.
 *   iOS 에서 실제로 그 문제가 나서 칩을 한 줄로 줄여 해결했지만, 여기선 줄일 수가 없다.
 */
enum class LessonStudioTab(val title: String, val guide: String) {
    CONSULT(
        "수강생 상담",
        "수강생 문의와 내가 보낸 레슨 제안을 한곳에서 확인하고, 대화·일정 조율까지 진행할 수 있어요.",
    ),
    FIND(
        "수강생 찾기",
        "개인레슨을 원하는 수강생의 공개 요청을 검색하고, 조건에 맞는 제안이 가능해요.",
    ),
    LESSON("레슨 소개", "내가 제공하는 개인레슨을 등록하고 수정할 수 있어요."),
}

@Composable
fun PersonalLessonManagementScreen(
    quoteApi: QuoteApi,
    productApi: LessonProductApi,
    roleLabel: String,
    onClose: () -> Unit,
    onOpenSent: (SentQuoteItem) -> Unit,
    onAutoReply: () -> Unit,
    onCreateLesson: () -> Unit,
    onEditLesson: (Int) -> Unit,
    onSlots: (Int) -> Unit,
    onGoGenreSettings: () -> Unit,
    onGoLessonSettings: (() -> Unit)? = null,
) {
    var tab by remember { mutableStateOf(LessonStudioTab.CONSULT) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(
            title = if (roleLabel.isEmpty()) "개인레슨 관리" else "개인레슨 관리 ($roleLabel)",
            onBack = onClose,
            trailing = {
                // 자동응답은 상담 탭에서만 — 다른 탭에서는 할 일이 아니다.
                if (tab == LessonStudioTab.CONSULT) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline, contentDescription = "자동응답 설정",
                        tint = MuyeonColors.textHead,
                        modifier = Modifier.padding(end = 12.dp).size(22.dp)
                            .clickable(onClick = onAutoReply),
                    )
                }
            },
        )
        SubTabRow(tab) { tab = it }
        HorizontalDivider(color = MuyeonColors.border)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                LessonStudioTab.CONSULT -> StudentConsultScreen(api = quoteApi, onOpenSent = onOpenSent)
                LessonStudioTab.FIND -> QuoteBrowseScreen(
                    api = quoteApi,
                    onClose = onClose,
                    onGoGenreSettings = onGoGenreSettings,
                    onGoLessonSettings = onGoLessonSettings,
                    embedded = true,
                )
                LessonStudioTab.LESSON -> LessonManageScreen(
                    api = productApi,
                    onClose = onClose,
                    onCreate = onCreateLesson,
                    onEdit = onEditLesson,
                    onSlots = onSlots,
                    embedded = true,
                )
            }
        }
    }
}

/**
 * 화면 안의 2차 탭 줄 — pacera SubTabBar 규격(좌측정렬 · 48dp · 글자 폭만큼의 2dp 밑줄).
 *  색과 굵기만으로 상태를 표시하면 색을 구분하기 어려운 사람에게는 굵기 하나만 남는다.
 *  밑줄은 형태 신호라 색과 무관하게 읽힌다.
 */
@Composable
private fun SubTabRow(selected: LessonStudioTab, onSelect: (LessonStudioTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MuyeonColors.surface)
            .horizontalScroll(rememberScrollState()).height(48.dp).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        LessonStudioTab.entries.forEach { entry ->
            val on = selected == entry
            Column(
                Modifier.clickable { onSelect(entry) }.fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    entry.title,
                    fontFamily = customFontFamily,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp, lineHeight = 20.sp,
                    color = if (on) MuyeonColors.textHead else MuyeonColors.secondary,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .background(if (on) MuyeonColors.primary else MuyeonColors.surface),
                )
            }
        }
    }
}
