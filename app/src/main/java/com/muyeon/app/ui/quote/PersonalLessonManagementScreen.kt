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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

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
 * 탭은 누르는 것 말고 **좌우로 밀어서도** 오간다(HorizontalPager). 탭 줄과 페이저가
 *  같은 상태 하나를 보므로 서로를 되받아 왕복하는 일이 없다.
 *
 * ⚠️ 이 페이저 안에는 **가로 스크롤 면을 두지 않는다.** 같은 축의 제스처가 겹치면
 *   칩 줄에서 밀 때 탭이 넘어가거나 거꾸로 그 영역에서 탭 전환이 먹히지 않는다(iOS 에서 겪었다).
 *   그래서 '수강생 찾기'의 장르·지역 칩도 한 줄 고정 + 필터 시트로 바꿨다(QuoteBrowseScreens).
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
    val tabs = LessonStudioTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val tab = tabs[pagerState.currentPage]

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
        SubTabRow(tab) { target ->
            scope.launch { pagerState.animateScrollToPage(tabs.indexOf(target)) }
        }
        HorizontalDivider(color = MuyeonColors.border)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            // 페이지를 넘겨도 다시 그리지 않게 유지 — 목록 스크롤 위치와 로딩 상태가 살아 있어야
            //  오가는 느낌이 끊기지 않는다(조회도 다시 돌지 않는다).
            beyondViewportPageCount = 1,
        ) { page ->
            when (tabs[page]) {
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
