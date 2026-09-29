package com.muyeon.app.ui.lesson

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.result.OnResultKeys
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.ReturnResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.result.rememberResultLauncher
import com.muyeon.app.result.setResultKeys
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute

/**
 * 레슨 컨테이너 — 웹 브릿지 진입점.
 *  iOS `WebViewModel+Hub`/`+Present` 의 presentLessonCalendar / presentLessonCreate /
 *  presentLessonSlotManage / presentLessonManage / presentLessonBooking 대응.
 *
 *  결과: 닫히면 LESSONS·LESSON_SCHEDULE·RESERVATIONS(+ 레슨 설정이면 QUOTE_PREFS — 견적 수신 조건).
 *   예약 변경·취소는 RESERVATIONS 에 reservationId / canceledId 를 싣는다(웹 즉시 반영용).
 */
class LessonActivity : ResultActivity() {

    override val defaultResultKeys: Set<String>
        get() = buildSet {
            add(ResultKeys.LESSONS); add(ResultKeys.LESSON_SCHEDULE); add(ResultKeys.RESERVATIONS)
            if (intent?.getStringExtra(EXTRA_ROUTE) == "settings") add(ResultKeys.QUOTE_PREFS)
        }

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_ID = "id"
        private const val EXTRA_RESERVATION_ID = "reservationId"

        // ── 인텐트 팩토리 — 네이티브 부모는 rememberResultLauncher 로 띄워 결과를 받는다 ──
        fun calendarIntent(context: Context) = intent(context, "calendar")
        fun manageIntent(context: Context) = intent(context, "manage")
        fun createIntent(context: Context) = intent(context, "create")
        fun editIntent(context: Context, lessonId: Int) = intent(context, "edit").putExtra(EXTRA_ID, lessonId)
        fun slotsIntent(context: Context, productId: Int?) =
            intent(context, "slots").putExtra(EXTRA_ID, productId ?: 0)
        fun bookingIntent(context: Context, productId: Int) =
            intent(context, "booking").putExtra(EXTRA_ID, productId)
        fun detailIntent(context: Context, lessonId: Int) = intent(context, "detail").putExtra(EXTRA_ID, lessonId)

        /** 레슨 설정(노출·성별·견적 수신 조건) — 웹 /lessonSettings 의 네이티브 이식. */
        fun settingsIntent(context: Context) = intent(context, "settings")

        /** 예약 상세(웹 예약내역 항목 탭) — iOS presentLessonReservationDetail 대응. */
        fun reservationDetailIntent(context: Context, reservationId: Int) =
            intent(context, "reservation").putExtra(EXTRA_RESERVATION_ID, reservationId)

        fun startCalendar(context: Context) = context.launchScreen(calendarIntent(context))
        fun startManage(context: Context) = context.launchScreen(manageIntent(context))
        fun startCreate(context: Context) = context.launchScreen(createIntent(context))
        fun startEdit(context: Context, lessonId: Int) = context.launchScreen(editIntent(context, lessonId))
        fun startSlots(context: Context, productId: Int?) = context.launchScreen(slotsIntent(context, productId))
        fun startBooking(context: Context, productId: Int) = context.launchScreen(bookingIntent(context, productId))
        fun startDetail(context: Context, lessonId: Int) = context.launchScreen(detailIntent(context, lessonId))
        fun startSettings(context: Context) = context.launchScreen(settingsIntent(context))
        fun startReservationDetail(context: Context, reservationId: Int) =
            context.launchScreen(reservationDetailIntent(context, reservationId))

        private fun intent(context: Context, route: String) =
            Intent(context, LessonActivity::class.java).putExtra(EXTRA_ROUTE, route)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "calendar"
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val reservationId = intent.getIntExtra(EXTRA_RESERVATION_ID, 0)

        // 무용수 유형은 레슨 활동 전면 차단(2026-09-26 정책) — 모든 진입점을 여기서 한 번에 막는다.
        //  운영(관리·개설·수정·예약시간·레슨 설정)은 강사·학원 전용, 예약은 일반 유형으로 전환 안내.
        //  캘린더·예약 상세·변경은 이미 잡힌 약속 확인이라 열어 둔다.
        val allowed = when (route) {
            "manage", "create", "edit", "slots", "settings" -> ActiveRole.allowLessonProvider(this)
            "booking" -> ActiveRole.allowLessonCustomer(this)
            else -> true
        }
        if (!allowed) { finish(); return }

        setContent {
            val nav = rememberNavController()
            val token = remember { TokenManager.getAccessToken(this) }
            val lessonApi = remember { LessonApi(token) }
            val calendarApi = remember { UserCalendarApi(token) }
            val wizardApi = remember { LessonWizardApi(token) }
            val productApi = remember { LessonProductApi(token) }
            val slotApi = remember { LessonSlotApi(token) }
            val bookingApi = remember { LessonBookingApi(token) }

            fun back() { if (!nav.popBackStack()) finish() }

            // 레슨 개설·수정 완료. 웹에서 바로 연 경우(시작 화면이 create/edit)는 웹 내 레슨으로 돌아가
            //  목록을 다시 읽게 한다(iOS presentLessonCreate.onCreated 와 동일).
            //  ★ 네이티브 부모(개인레슨 관리 등)가 띄웠으면 웹으로 튕기지 않는다 — CLEAR_TOP 이 그 부모를
            //    죽인다. LESSONS 결과만 들고 부모로 돌아가 부모가 다시 읽는다(웹도 버블링으로 듣는다).
            //  네이티브 '레슨 관리' 안(NavHost)에서 연 경우는 그 목록으로 되돌아간다.
            fun lessonSaved() {
                addResultKeys(ResultKeys.LESSONS)
                when {
                    route != "create" && route != "edit" -> {
                        nav.setResultKeys(ResultKeys.LESSONS)
                        back()
                    }
                    launchedFromNative -> finish()
                    else -> openWebAndFinish("/myLessons")
                }
            }

            NavHost(nav, startDestination = route) {
                composable("calendar") { entry ->
                    // 배지 상태(신규 예약·조율 중 확인)는 기기 저장 — iOS UserDefaults 대응.
                    val calPrefs = remember {
                        getSharedPreferences("muyeon.calendar", Context.MODE_PRIVATE)
                    }
                    val state = viewModel { LessonCalendarState(lessonApi, calendarApi, calPrefs) }
                    // 복귀 재조회는 결과 키로만 — 상세·내 캘린더(NavHost)와 채팅(액티비티).
                    entry.OnResultKeys { keys -> if (ResultKeys.LESSON_SCHEDULE in keys) state.load() }
                    // 채팅에서 제안 수락·거절 등으로 일정이 바뀔 수 있다 — 채팅이 돌려준 결과면 다시 읽는다.
                    val chatLauncher = rememberResultLauncher { state.load() }
                    LessonCalendarScreen(
                        state = state,
                        onClose = { finish() },
                        onOpenLesson = { lid -> nav.navigate("detail/$lid") },
                        onManageCalendars = { nav.navigate("calendars") },
                        onOpenChat = { rid -> chatLauncher.launch(ChatActivity.roomIntent(this@LessonActivity, rid)) },
                    )
                }
                composable("calendars") {
                    // 내 캘린더(관리) — 생성/편집/삭제하면 캘린더 화면이 돌아와서 다시 읽도록 결과를 심는다.
                    CalendarManageScreen(
                        api = calendarApi, onClose = { back() },
                        onChanged = { nav.setResultKeys(ResultKeys.LESSON_SCHEDULE) },
                    )
                }
                composable("manage") {
                    LessonManageScreen(
                        api = productApi,
                        onClose = { finish() },
                        // 삭제·복원·노출권 — 닫힐 때 웹 내 레슨도 다시 읽게(기본 키지만 명시해 둔다).
                        onChanged = { addResultKeys(ResultKeys.LESSONS) },
                        onCreate = { nav.navigate("create") },
                        onEdit = { lid -> nav.navigate("edit/$lid") },
                        onSlots = { pid -> nav.navigate("slots/$pid") },
                    )
                }
                composable("create") {
                    LessonWizardScreen(wizardApi, null, onClose = { back() }, onCreated = { lessonSaved() })
                }
                composable("edit") {
                    LessonWizardScreen(wizardApi, id.takeIf { it > 0 }, onClose = { back() }, onCreated = { lessonSaved() })
                }
                composable("edit/{id}") { e ->
                    val lid = e.arguments?.getString("id")?.toIntOrNull()
                    LessonWizardScreen(wizardApi, lid, onClose = { back() }, onCreated = { lessonSaved() })
                }
                composable("slots") {
                    LessonSlotManageScreen(slotApi, id.takeIf { it > 0 }, onClose = { back() })
                }
                composable("slots/{id}") { e ->
                    LessonSlotManageScreen(slotApi, e.arguments?.getString("id")?.toIntOrNull(), onClose = { back() })
                }
                composable("booking") {
                    LessonBookingScreen(
                        bookingApi, id,
                        onClose = { back() },
                        // 0원 레슨 예약 확정 — 웹 예약내역을 다시 읽게 하고 내 예약으로 보낸다(iOS 와 같은 결말).
                        //  (RESERVATIONS 는 기본 결과 키 — 웹이 결과로 다시 읽는다.)
                        onDone = { openWebAndFinish("/myReservations") },
                        // 예약금 결제는 웹 결제창으로 넘긴다(iOS 와 동일 경로).
                        //  /reservations/:id?pay=1 이 토스 결제창을 띄우고, 승인되면 예약이 확정된다.
                        onNeedPayment = { rid -> openWebAndFinish("/reservations/$rid?pay=1") },
                    )
                }
                composable("detail") {
                    LessonDetailScreen(
                        lessonApi, calendarApi, id, onClose = { back() },
                        onOpenChat = { rid -> ChatActivity.startRoom(this@LessonActivity, rid) },
                        onOpenReservation = { openWebAndFinish("/myReservations") },
                    )
                }
                composable("detail/{id}") { e ->
                    // 캘린더에서 연 상세 — 돌아가면 캘린더가 다시 읽는다(취소·변경 반영).
                    nav.ReturnResultKeys(e, ResultKeys.LESSON_SCHEDULE)
                    val lid = e.arguments?.getString("id")?.toIntOrNull() ?: 0
                    LessonDetailScreen(
                        lessonApi, calendarApi, lid, onClose = { back() },
                        onOpenChat = { rid -> ChatActivity.startRoom(this@LessonActivity, rid) },
                        onOpenReservation = { openWebAndFinish("/myReservations") },
                    )
                }
                composable("settings") {
                    LessonProvideSettingsScreen(
                        api = remember { LessonSettingsApi(token) },
                        onClose = { back() },
                        // 강사프로필 = 기본 이력서. 이력서 관리로 보낸다(iOS onManageProfile).
                        onManageProfile = {
                            com.muyeon.app.ui.resume.ResumeActivity.startList(this@LessonActivity, "teacher")
                        },
                    )
                }
                composable("reschedule/{pid}/{rid}") { e ->
                    val pid = e.arguments?.getString("pid")?.toIntOrNull() ?: 0
                    val rid = e.arguments?.getString("rid")?.toIntOrNull()
                    LessonBookingScreen(
                        bookingApi, pid, rescheduleReservationId = rid,
                        onClose = { back() },
                        // 변경 요청 접수 — 강사 승인 전이지만 '변경 요청 중' 표시가 바뀌므로 웹을 다시 읽게 한다.
                        onDone = {
                            rid?.let { reservationChanged(it) }
                            finish()
                        },
                    )
                }
                composable("reservation") {
                    LessonReservationDetailScreen(
                        bookingApi, reservationId,
                        onClose = { back() },
                        // 변경: 같은 레슨상품 예약 화면을 '변경 모드'로 연다(원자적 리스케줄).
                        //  ★ 예전엔 웹 /lessons/{pid}?reschedule={rid} 로 보냈는데 웹은 reschedule 을 읽지 않아,
                        //    거기서 예약하면 기존 예약이 옮겨지지 않고 **새 예약이 하나 더** 생겼다.
                        //    변경 모드는 네이티브 예약 화면에 이미 있으므로 앱 안에서 바로 연다.
                        onChange = { pid, rid -> nav.navigate("reschedule/$pid/$rid") },
                        onChanged = { rid -> reservationChanged(rid) },
                        onCanceled = { rid ->
                            // 취소 완료 → 웹 예약내역에 즉시 취소선(canceledId) + 재조회.
                            putResultPayload(ResultKeys.RESERVATIONS, bundleOf("canceledId" to rid.toString()))
                            finish()
                        },
                    )
                }
            }
        }
    }

    /** 예약 변경(요청) — 웹 예약상세가 그 건을 다시 읽도록 reservationId 를 싣는다. */
    private fun reservationChanged(reservationId: Int) =
        putResultPayload(ResultKeys.RESERVATIONS, bundleOf("reservationId" to reservationId.toString()))

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
