package com.muyeon.app.ui.review.quick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.ui.common.setEdgeContent
import com.muyeon.app.ui.review.ReviewApi
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-14 빠른 후기 화면(당근 거래 후기 흐름, iOS QuickReviewFlow 1:1).
 *  단계마다 따로 화면(NavHost 경로)이다: rating(얼굴+사유) → comment(글·사진) → done(완료). 한 화면 if/else 로 바꿔 그리지 않는다.
 *  상태는 QuickReviewCoordinator 하나. 끝나면 REVIEWS·CHAT_ROOM 결과를 돌려 채팅방 상단 바가 다시 읽는다.
 */
class QuickReviewActivity : com.muyeon.app.result.ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.REVIEWS, ResultKeys.CHAT_ROOM)

    companion object {
        private const val EXTRA_TEACHER_ID = "teacherId"
        private const val EXTRA_TEACHER_NAME = "teacherName"
        private const val EXTRA_LESSON_LINE = "lessonLine"

        fun intent(context: Context, teacherId: Int, teacherName: String, lessonLine: String): Intent =
            Intent(context, QuickReviewActivity::class.java)
                .putExtra(EXTRA_TEACHER_ID, teacherId)
                .putExtra(EXTRA_TEACHER_NAME, teacherName)
                .putExtra(EXTRA_LESSON_LINE, lessonLine)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val teacherId = intent.getIntExtra(EXTRA_TEACHER_ID, 0)
        if (teacherId <= 0) { finish(); return }
        val teacherName = intent.getStringExtra(EXTRA_TEACHER_NAME)?.ifBlank { null } ?: "강사"
        val lessonLine = intent.getStringExtra(EXTRA_LESSON_LINE)?.ifBlank { null } ?: "레슨"

        setEdgeContent {
            val nav = rememberNavController()
            com.muyeon.app.analytics.TrackNavigation("QuickReview", nav)
            val api = remember { ReviewApi(TokenManager.getAccessToken(this)) }
            val c = remember { QuickReviewCoordinator(teacherId, teacherName, lessonLine, api) }
            val scope = rememberCoroutineScope()
            NavHost(
                nav, startDestination = "rating",
                popEnterTransition = com.muyeon.app.ui.common.nav.MuyeonNav.popEnter,
                popExitTransition = com.muyeon.app.ui.common.nav.MuyeonNav.popExit,
            ) {
                composable("rating") {
                    ReviewRatingStep(c, onClose = { finish() }, onNext = { nav.navigate("comment") })
                }
                composable("comment") {
                    ReviewCommentStep(
                        c, api,
                        onBack = { nav.popBackStack() },
                        onSubmit = {
                            scope.launch {
                                if (c.submit()) {
                                    addResultKeys(ResultKeys.REVIEWS)
                                    nav.navigate("done") { popUpTo("rating") { inclusive = true } }
                                }
                            }
                        },
                        // 자세히 쓰기 — 기존 긴 후기 폼(별점 3축·레슨 형태·목적…)
                        onDetail = {
                            com.muyeon.app.ui.resume.ResumeActivity.startReviewWrite(this@QuickReviewActivity, teacherId, teacherName, null)
                            finish()
                        },
                    )
                }
                composable("done") { ReviewDoneStep(c, onDone = { finish() }) }
            }
        }
    }
}
