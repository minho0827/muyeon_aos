package com.muyeon.app.ui.resume

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.quote.QuoteWizardActivity
import com.muyeon.app.ui.review.ReviewApi
import com.muyeon.app.ui.review.ReviewListScreen
import com.muyeon.app.ui.review.ReviewWriteScreen
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute

/**
 * 이력서/공개프로필/리뷰 컨테이너 — 웹 브릿지 진입점.
 *  iOS `WebViewModel+ResumeScreens.swift` / `+ReviewWrite.swift` 의 present* 대응.
 *
 *  경로: list ↔ edit / visibility ↔ preview(공개프로필) / profile ↔ reviews ↔ write / applicant
 */
class ResumeActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_RESUME_ID = "resumeId"
        private const val EXTRA_USER_ID = "userId"
        private const val EXTRA_SRC = "src"
        private const val EXTRA_JOB_ID = "postingId"
        private const val EXTRA_KIND = "applicationKind"
        private const val EXTRA_APPLICATION_ID = "applicationId"
        private const val EXTRA_TEACHER_NAME = "teacherName"
        private const val EXTRA_LESSON_TYPE = "lessonType"
        private const val EXTRA_SEEK_PROFILE = "seekProfile"

        /** 이력서 목록(mode=teacher|dancer). */
        fun startList(context: Context, mode: String?) =
            context.go(intent(context, "list").putExtra(EXTRA_MODE, mode ?: ""))

        /** 이력서 편집(resumeId 없으면 신규). seekProfile=구직 프로필 등록 모드. */
        fun startEdit(context: Context, resumeId: Int?, mode: String?, seekProfile: Boolean = false) =
            context.go(
                intent(context, "edit")
                    .putExtra(EXTRA_RESUME_ID, resumeId ?: 0)
                    .putExtra(EXTRA_MODE, mode ?: "")
                    .putExtra(EXTRA_SEEK_PROFILE, seekProfile),
            )

        fun startVisibility(context: Context, mode: String?) =
            context.go(intent(context, "visibility").putExtra(EXTRA_MODE, mode ?: ""))

        fun startProfile(context: Context, userId: Int, src: String? = null) =
            context.go(intent(context, "profile").putExtra(EXTRA_USER_ID, userId).putExtra(EXTRA_SRC, src ?: ""))

        fun startReviewList(context: Context, teacherId: Int) =
            context.go(intent(context, "reviews").putExtra(EXTRA_USER_ID, teacherId))

        fun startReviewWrite(context: Context, teacherId: Int, teacherName: String?, lessonType: String?) =
            context.go(
                intent(context, "write")
                    .putExtra(EXTRA_USER_ID, teacherId)
                    .putExtra(EXTRA_TEACHER_NAME, teacherName ?: "")
                    .putExtra(EXTRA_LESSON_TYPE, lessonType ?: ""),
            )

        /** 지원자 이력서(원장). kind=JOB(구인) | SUB(대타) — postingId 가 각각 jobId/subId. */
        fun startApplicant(context: Context, postingId: Int, applicationId: Int, kind: String?) =
            context.go(
                intent(context, "applicant")
                    .putExtra(EXTRA_JOB_ID, postingId)
                    .putExtra(EXTRA_APPLICATION_ID, applicationId)
                    .putExtra(EXTRA_KIND, kind ?: "JOB"),
            )

        private fun intent(context: Context, route: String) =
            Intent(context, ResumeActivity::class.java).putExtra(EXTRA_ROUTE, route)

        private fun Context.go(i: Intent) {
            if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "list"
        val mode = ResumeMode.from(intent.getStringExtra(EXTRA_MODE)?.ifEmpty { null })
        val resumeIdExtra = intent.getIntExtra(EXTRA_RESUME_ID, 0)
        val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
        val src = intent.getStringExtra(EXTRA_SRC)?.ifEmpty { null }
        val postingId = intent.getIntExtra(EXTRA_JOB_ID, 0)
        val applicationKind = ApplicantPostingKind.from(intent.getStringExtra(EXTRA_KIND))
        val applicationId = intent.getIntExtra(EXTRA_APPLICATION_ID, 0)
        val teacherName = intent.getStringExtra(EXTRA_TEACHER_NAME).orEmpty()
        val lessonType = intent.getStringExtra(EXTRA_LESSON_TYPE)?.ifEmpty { null }
        val seekProfileExtra = intent.getBooleanExtra(EXTRA_SEEK_PROFILE, false)

        setContent {
            val nav = rememberNavController()
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { ResumeApi(token) }
            val reviewApi = remember { ReviewApi(token) }
            val prefs = remember { getSharedPreferences("muyeon.resume", MODE_PRIVATE) }

            fun back() { if (!nav.popBackStack()) finish() }

            NavHost(nav, startDestination = route) {
                composable("list") {
                    ResumeListScreen(
                        api = api, mode = mode, onClose = { finish() },
                        onEdit = { id -> nav.navigate("edit/${id ?: 0}") },
                        onVisibility = { nav.navigate("visibility") },
                        // 기본 이력서(없으면 첫 이력서, 그것도 없으면 신규)를 구직 프로필로 등록
                        onSeekProfile = { id -> nav.navigate("seek/${id ?: 0}") },
                    )
                }
                composable("edit") {
                    ResumeEditScreen(
                        api = api, resumeId = resumeIdExtra.takeIf { it > 0 }, mode = mode,
                        onClose = { back() }, onSaved = { back() },
                        isSeekProfile = seekProfileExtra,
                        onVisibility = { nav.navigate("visibility") },
                    )
                }
                composable("edit/{id}") { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull()?.takeIf { it > 0 }
                    ResumeEditScreen(api = api, resumeId = id, mode = mode, onClose = { back() }, onSaved = { back() })
                }
                composable("seek/{id}") { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull()?.takeIf { it > 0 }
                    ResumeEditScreen(
                        api = api, resumeId = id, mode = mode,
                        onClose = { back() }, onSaved = { back() },
                        isSeekProfile = true,
                        onVisibility = { nav.navigate("visibility") },
                    )
                }
                composable("visibility") {
                    FieldVisibilityScreen(
                        api = api, mode = mode, prefs = prefs,
                        onClose = { back() },
                        // 본인 프로필을 일반회원 시점으로(preview=1)
                        onPreview = { nav.navigate("preview") },
                    )
                }
                composable("preview") {
                    PublicProfileScreen(
                        api = api, reviewApi = reviewApi, userId = 0, preview = true,
                        onClose = { back() }, onOpenChat = {}, onRequestQuote = {}, onOpenReviews = {},
                    )
                }
                composable("profile") {
                    PublicProfileScreen(
                        api = api, reviewApi = reviewApi, userId = userId, src = src,
                        onClose = { back() },
                        onOpenChat = { roomId -> ChatActivity.startRoom(this@ResumeActivity, roomId) },
                        // 지정 견적요청 — 이 강사에게만(iOS presentQuoteWizardDirect targetTeacherId)
                        onRequestQuote = { QuoteWizardActivity.start(this@ResumeActivity, null, userId.toString()) },
                        onOpenReviews = { nav.navigate("reviews") },
                    )
                }
                composable("reviews") {
                    ReviewListScreen(
                        api = reviewApi, teacherId = userId,
                        onClose = { back() }, onWrite = { nav.navigate("write") },
                    )
                }
                composable("write") {
                    ReviewWriteScreen(
                        api = reviewApi, resumeApi = api, teacherId = userId,
                        teacherName = teacherName, teacherImage = null,
                        lessonDateLine = null, prefillLessonType = lessonType,
                        onClose = { back() }, onDone = { back() },
                    )
                }
                composable("applicant") {
                    ApplicantResumeScreen(
                        api = api, reviewApi = reviewApi,
                        postingId = postingId, applicationId = applicationId, kind = applicationKind,
                        onClose = { back() },
                        onOpenChat = { roomId, name -> ChatActivity.startRoom(this@ResumeActivity, roomId, name) },
                    )
                }
            }
        }
    }

    /** 네이티브 미이식 화면으로 나갈 때(멤버십 등) — 웹 경로 폴백. */
    @Suppress("unused")
    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
