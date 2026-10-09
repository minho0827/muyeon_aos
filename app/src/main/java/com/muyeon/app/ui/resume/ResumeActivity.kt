package com.muyeon.app.ui.resume

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.result.OnResultKeys
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.ReturnResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.quote.QuoteWizardActivity
import com.muyeon.app.ui.review.ReviewApi
import com.muyeon.app.ui.review.ReviewEditSheet
import com.muyeon.app.ui.review.ReviewListScreen
import com.muyeon.app.ui.review.ReviewWriteScreen
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute
import com.muyeon.app.webview.WebCallbacks

/**
 * 이력서/공개프로필/리뷰 컨테이너 — 웹 브릿지 진입점.
 *  iOS `WebViewModel+ResumeScreens.swift` / `+ReviewWrite.swift` 의 present* 대응.
 *
 *  경로: list ↔ edit / visibility ↔ preview(공개프로필) / profile ↔ reviews ↔ write / applicant
 *
 *  결과: RESUME·PROFILE·REVIEWS(이력서·공개범위·후기 어느 것이든 바뀌었을 수 있다).
 */
class ResumeActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.RESUME, ResultKeys.PROFILE, ResultKeys.REVIEWS)

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
        private const val EXTRA_RATING = "rating"
        private const val EXTRA_CONTENT = "content"
        private const val EXTRA_SEEK_PROFILE = "seekProfile"

        /** 이력서 목록(mode=teacher|dancer). */
        fun startList(context: Context, mode: String?) =
            context.go(intent(context, "list").putExtra(EXTRA_MODE, mode ?: ""))

        /** 이력서 편집 인텐트 — 네이티브 부모(공개 프로필 등)가 결과 런처로 띄울 때. */
        fun editIntent(context: Context, resumeId: Int?, mode: String?, seekProfile: Boolean = false): Intent =
            intent(context, "edit")
                .putExtra(EXTRA_RESUME_ID, resumeId ?: 0)
                .putExtra(EXTRA_MODE, mode ?: "")
                .putExtra(EXTRA_SEEK_PROFILE, seekProfile)

        /** 이력서 편집(resumeId 없으면 신규). seekProfile=구직 프로필 등록 모드. */
        fun startEdit(context: Context, resumeId: Int?, mode: String?, seekProfile: Boolean = false) =
            context.go(editIntent(context, resumeId, mode, seekProfile))

        fun startVisibility(context: Context, mode: String?) =
            context.go(intent(context, "visibility").putExtra(EXTRA_MODE, mode ?: ""))

        fun profileIntent(context: Context, userId: Int, src: String? = null): Intent =
            intent(context, "profile").putExtra(EXTRA_USER_ID, userId).putExtra(EXTRA_SRC, src ?: "")

        fun startProfile(context: Context, userId: Int, src: String? = null) =
            context.go(profileIntent(context, userId, src))

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
        /**
         * 리뷰 수정 시트. 저장 API 는 웹이 __onReviewEdited 로 수행하므로 teacherId 가 필요 없다
         * (웹 openReviewEdit payload 에도 rating/content 만 온다).
         */
        fun startReviewEdit(context: Context, rating: Int, content: String) =
            context.go(
                intent(context, "reviewEdit")
                    .putExtra(EXTRA_RATING, rating)
                    .putExtra(EXTRA_CONTENT, content),
            )

        fun startApplicant(context: Context, postingId: Int, applicationId: Int, kind: String?) =
            context.go(
                intent(context, "applicant")
                    .putExtra(EXTRA_JOB_ID, postingId)
                    .putExtra(EXTRA_APPLICATION_ID, applicationId)
                    .putExtra(EXTRA_KIND, kind ?: "JOB"),
            )

        private fun intent(context: Context, route: String) =
            Intent(context, ResumeActivity::class.java).putExtra(EXTRA_ROUTE, route)

        private fun Context.go(i: Intent) = launchScreen(i)
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
        val editRating = intent.getIntExtra(EXTRA_RATING, 0)
        val editContent = intent.getStringExtra(EXTRA_CONTENT).orEmpty()

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
                composable("edit") { e ->
                    // 공개 범위에서 돌아오면(RESUME) 폼은 그대로 두고 설정 값만 다시 읽는다.
                    var settingsReload by rememberSaveable { mutableIntStateOf(0) }
                    e.OnResultKeys { keys -> if (ResultKeys.RESUME in keys) settingsReload++ }
                    ResumeEditScreen(
                        api = api, resumeId = resumeIdExtra.takeIf { it > 0 }, mode = mode,
                        onClose = { back() }, onSaved = { back() },
                        isSeekProfile = seekProfileExtra,
                        onVisibility = { nav.navigate("visibility") },
                        settingsReload = settingsReload,
                    )
                }
                composable("edit/{id}") { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull()?.takeIf { it > 0 }
                    ResumeEditScreen(api = api, resumeId = id, mode = mode, onClose = { back() }, onSaved = { back() })
                }
                composable("seek/{id}") { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull()?.takeIf { it > 0 }
                    var settingsReload by rememberSaveable { mutableIntStateOf(0) }
                    e.OnResultKeys { keys -> if (ResultKeys.RESUME in keys) settingsReload++ }
                    ResumeEditScreen(
                        api = api, resumeId = id, mode = mode,
                        onClose = { back() }, onSaved = { back() },
                        isSeekProfile = true,
                        onVisibility = { nav.navigate("visibility") },
                        settingsReload = settingsReload,
                    )
                }
                composable("visibility") { e ->
                    nav.ReturnResultKeys(e, ResultKeys.RESUME)
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
                        // 채팅방에서 연 프로필은 열람 전용 — 하단 CTA 숨김(iOS src:"chat", hideCta:true)
                        hideCta = src == "chat",
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
                        onClose = { back() },
                        // 웹 강사 리뷰 목록 재조회(iOS presentReviewWrite onSaved 와 동일) — 결과 키 REVIEWS.
                        onDone = { addResultKeys(ResultKeys.REVIEWS); back() },
                    )
                }
                composable("reviewEdit") {
                    // 리뷰 수정은 네이티브가 UI 만 담당하고 저장은 웹이 __onReviewEdited 로 수행한다.
                    ReviewEditSheet(
                        initialRating = editRating, initialContent = editContent,
                        onSave = { r, c ->
                            WebCallbacks.reviewEdited(this@ResumeActivity, r, c)
                            finish()
                        },
                        onClose = { back() },
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
