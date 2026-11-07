package com.muyeon.app.ui.home.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.home.HomeActions
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.home.HomeUiState
import com.muyeon.app.ui.home.HomeViewModel
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.home.HomeRecruit
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.domain.model.teacher.TeacherCard

/**
 * 스와이프 홈 섹션 순서 — 웹 HomeSwipe.
 *   일반·비회원: 좋아할 강사 · (비회원)강사 채용정보 · 추천 레슨 · 체험 · 원데이 · 공간 대관 · 양도양수 · 장르별 강사 구인.
 *   학원: "📍 우리 학원 맞춤" · 추천 강사(또는 조건 안내) · 운영 중인 클래스 · 강사 모집 공고 · 레슨을 찾는 회원 · 공간 · 양도양수.
 */
@Composable
fun HomeSwipeContent(state: HomeUiState, vm: HomeViewModel, actions: HomeActions, modifier: Modifier = Modifier) {
    val teacherW = slideWidth(0.4f, 200.dp)
    val lessonW = slideWidth(0.82f)
    // DM(레슨 문의) — 비회원·일반 회원(웹 홈 dmTeacher). 그 밖 유형은 ⋮ 추천 안 받기.
    val showDm = !state.isLoggedIn || state.activeType == ActiveType.GENERAL
    val teacherCard: @Composable (TeacherCard, Modifier) -> Unit = { t, m ->
        HomeTeacherSlide(
            t, showDm,
            onClick = { actions.native("openPublicProfile", mapOf("userId" to t.id.toString())) },
            onRequest = { actions.native("openQuoteWizard", mapOf("category" to LessonGenres.QUOTE_CATEGORY[t.genres.firstOrNull()].orEmpty(), "targetTeacherId" to t.id.toString())) },
            onBlockMenu = { vm.openBlock(t) },
            modifier = m,
        )
    }
    val lessonCard: @Composable (com.muyeon.domain.model.home.HomeLesson, String?, Modifier) -> Unit = { l, tag, m ->
        HomeLessonSlide(l, tag, l.id in state.likedLessons, onClick = { actions.openPath("/lessons/${l.id}") }, onToggleLike = { vm.toggleLessonLike(l.id) }, modifier = m)
    }
    val lessonList: (String, String) -> String = { title, type -> "/lessonList?title=${Uri.encode(title)}&type=$type" }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimensions.sectionGap)) {
        if (state.isAcademy) {
            val a = state.academy
            val name = a.profile?.academyName ?: stringResource(R.string.home_our_academy)
            Text(stringResource(R.string.home_academy_heading), style = AppTypography.Title20, color = AppColors.TextHead, modifier = Modifier.padding(start = 20.dp))
            if (a.recTeachers.isNotEmpty()) {
                HomeCarouselSection(stringResource(R.string.home_academy_rec, name + eulReul(name)), a.recTeachers, { it.id }, teacherW, onMore = { actions.openPath("/teachers?recommend=1") }, card = teacherCard)
            } else if (a.recLoaded) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_academy_rec, name + eulReul(name)), style = AppTypography.Section16, color = AppColors.TextHead)
                    val has = a.profile?.hasCriteria != false
                    Text(stringResource(if (has) R.string.home_academy_rec_empty else R.string.home_academy_rec_setup), style = AppTypography.Body14, color = AppColors.TextBody)
                    SheetButton(SheetAction(stringResource(if (has) R.string.home_academy_widen else R.string.home_academy_setup), primary = !has) { actions.openPath("/academyProfile") }, Modifier.fillMaxWidth())
                }
            }
            HomeCarouselSection(
                stringResource(R.string.home_academy_lessons, name), a.myLessons, { it.id }, lessonW, onMore = { actions.openPath("/myLessons") },
                empty = if (a.myLessonsLoaded) ({ HomeEmptyCard(stringResource(R.string.home_academy_lessons_empty_t), stringResource(R.string.home_academy_lessons_empty_d), Modifier.fillMaxWidth(0.82f)) }) else null,
            ) { l, m -> lessonCard(l, null, m) }
            HomeCarouselSection(
                stringResource(R.string.home_academy_jobs, name), a.myJobs, { it.id }, slideWidth(0.7f), onMore = { actions.openPath("/myPostings") },
                empty = if (a.myJobsLoaded) ({ HomeEmptyCard(stringResource(R.string.home_academy_jobs_empty_t), stringResource(R.string.home_academy_jobs_empty_d), Modifier.fillMaxWidth(0.7f)) }) else null,
            ) { j, m -> HomeMyJobSlide(j, { actions.openPath("/jobs/${j.id}") }, m) }
            HomeQuoteRequests(
                state.quotes, state.likedQuotes,
                onOpen = { q -> actions.openPath(if (q.isMine) "/myQuotes/${q.id}" else "/availableQuotes/${q.id}" + if (!q.canRespond) "?sent=1" else "") },
                onToggleLike = vm::toggleQuoteLike, onMore = { actions.openPath("/availableQuotes") }, showTitleIcon = false,
            )
        } else {
            HomeCarouselSection(stringResource(R.string.home_sec_teachers), state.teachers, { it.id }, teacherW, onMore = { actions.openTab(MainTab.LESSONS) }, card = teacherCard)
            if (!state.isLoggedIn) {
                HomeNoticeList(
                    stringResource(R.string.home_sec_job_notice), R.drawable.ic_web_speaker, AppColors.Primary,
                    state.jobNotice.map { r -> HomeNoticeRow(r.id, r.title ?: stringResource(R.string.home_recruit_default), KstDates.dot(r.createdAt), HomeLabels.isRecent(r.createdAt)) { actions.openPath("/jobs/${r.id}") } },
                    onMore = { actions.openPath("/jobs?tab=hire") },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            // 더보기 목록 제목은 이모지 없이(웹 lessonList title).
            val listPopular = stringResource(R.string.home_list_popular)
            val listExp = stringResource(R.string.home_list_experience)
            val listOne = stringResource(R.string.home_list_oneday)
            val tPopular = stringResource(R.string.home_sec_popular)
            HomeCarouselSection(tPopular, state.preview.popular, { it.id }, lessonW, onMore = { actions.openPath(lessonList(listPopular, "popular")) }) { l, m -> lessonCard(l, null, m) }
            val tExp = stringResource(R.string.home_sec_experience)
            val exp = stringResource(R.string.home_tag_experience)
            HomeCarouselSection(tExp, state.preview.experience, { it.id }, lessonW, onMore = { actions.openPath(lessonList(listExp, "experience")) }) { l, m -> lessonCard(l, exp, m) }
            val tOne = stringResource(R.string.home_sec_oneday)
            val one = stringResource(R.string.home_tag_oneday)
            HomeCarouselSection(tOne, state.preview.oneday, { it.id }, lessonW, onMore = { actions.openPath(lessonList(listOne, "oneday")) }) { l, m -> lessonCard(l, one, m) }
        }
        HomeCarouselSection(stringResource(R.string.home_sec_spaces), state.spaces, { it.id }, teacherW, fit = true, onMore = { actions.native("openSpaces", emptyMap()) }) { s, m ->
            HomeSpaceCard(s, { actions.native("openSpaceDetail", mapOf("spaceId" to s.id.toString())) }, m)
        }
        HomeCarouselSection(stringResource(R.string.home_sec_transfers), state.transfers, { it.id }, teacherW, fit = true, onMore = { actions.native("openStudioTransfers", emptyMap()) }) { t, m ->
            HomeTransferCard(t, { actions.native("openStudioTransferDetail", mapOf("id" to t.id.toString())) }, m)
        }
        if (!state.isAcademy) {
            HomeCarouselSection(stringResource(R.string.home_sec_recruits), state.recruits, { "${it.kind}-${it.id}" }, teacherW, onMore = { actions.openPath("/recruit/all") }) { r, m ->
                HomeRecruitSlide(r, { actions.openPath(if (r.kind == HomeRecruit.Kind.JOB) "/jobs/${r.id}" else "/subs/${r.id}") }, m)
            }
        }
    }
}

/** 을/를(웹 withEulReul — 받침 있거나 한글이 아니면 '을'). */
internal fun eulReul(name: String): String {
    val c = name.lastOrNull()?.code?.minus(0xAC00) ?: return "를"
    return if (c < 0 || c > 11171 || c % 28 != 0) "을" else "를"
}
