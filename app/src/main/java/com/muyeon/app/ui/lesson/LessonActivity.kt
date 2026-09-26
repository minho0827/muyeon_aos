package com.muyeon.app.ui.lesson

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute
import com.muyeon.app.webview.WebCallbacks

/**
 * 레슨 컨테이너 — 웹 브릿지 진입점.
 *  iOS `WebViewModel+Hub`/`+Present` 의 presentLessonCalendar / presentLessonCreate /
 *  presentLessonSlotManage / presentLessonManage / presentLessonBooking 대응.
 */
class LessonActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_ID = "id"
        private const val EXTRA_RESERVATION_ID = "reservationId"

        fun startCalendar(context: Context) = context.go(intent(context, "calendar"))
        fun startManage(context: Context) = context.go(intent(context, "manage"))
        fun startCreate(context: Context) = context.go(intent(context, "create"))
        fun startEdit(context: Context, lessonId: Int) =
            context.go(intent(context, "edit").putExtra(EXTRA_ID, lessonId))
        fun startSlots(context: Context, productId: Int?) =
            context.go(intent(context, "slots").putExtra(EXTRA_ID, productId ?: 0))
        fun startBooking(context: Context, productId: Int) =
            context.go(intent(context, "booking").putExtra(EXTRA_ID, productId))
        fun startDetail(context: Context, lessonId: Int) =
            context.go(intent(context, "detail").putExtra(EXTRA_ID, lessonId))

        /** 레슨 설정(노출·성별·견적 수신 조건) — 웹 /lessonSettings 의 네이티브 이식. */
        fun startSettings(context: Context) = context.go(intent(context, "settings"))

        /** 예약 상세(웹 예약내역 항목 탭) — iOS presentLessonReservationDetail 대응. */
        fun startReservationDetail(context: Context, reservationId: Int) =
            context.go(intent(context, "reservation").putExtra(EXTRA_RESERVATION_ID, reservationId))

        private fun intent(context: Context, route: String) =
            Intent(context, LessonActivity::class.java).putExtra(EXTRA_ROUTE, route)

        private fun Context.go(i: Intent) {
            if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
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
            //  목록을 다시 읽게 한다(iOS presentLessonCreate.onCreated 와 동일). 네이티브 '레슨 관리'
            //  안에서 연 경우는 그 목록으로 되돌아간다(웹 목록도 대기열 콜백으로 맞춘다).
            fun lessonSaved() {
                WebCallbacks.lessonsChanged(this@LessonActivity)
                if (route == "create" || route == "edit") openWebAndFinish("/myLessons") else back()
            }

            NavHost(nav, startDestination = route) {
                composable("calendar") {
                    // 배지 상태(신규 예약·조율 중 확인)는 기기 저장 — iOS UserDefaults 대응.
                    val calPrefs = remember {
                        getSharedPreferences("muyeon.calendar", Context.MODE_PRIVATE)
                    }
                    val state = viewModel { LessonCalendarState(lessonApi, calendarApi, calPrefs) }
                    LessonCalendarScreen(
                        state = state,
                        onClose = { finish() },
                        onOpenLesson = { lid -> nav.navigate("detail/$lid") },
                        onManageCalendars = { nav.navigate("calendars") },
                        onOpenChat = { rid -> ChatActivity.startRoom(this@LessonActivity, rid) },
                    )
                }
                composable("calendars") {
                    // 내 캘린더(관리) — 생성/편집/삭제 후 캘린더 화면이 재로딩되도록 back 으로 복귀.
                    CalendarManageScreen(api = calendarApi, onClose = { back() })
                }
                composable("manage") {
                    LessonManageScreen(
                        api = productApi,
                        onClose = { finish() },
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
                        onDone = {
                            WebCallbacks.reservationsChanged(this@LessonActivity)
                            openWebAndFinish("/myReservations")
                        },
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
                            WebCallbacks.reservationsChanged(this@LessonActivity, rid)
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
                        onChanged = { rid -> WebCallbacks.reservationsChanged(this@LessonActivity, rid) },
                        onCanceled = { rid ->
                            // 취소 완료 → 웹 예약내역에 즉시 반영(재조회 없이 콜백).
                            WebCallbacks.lessonReservationCanceled(this@LessonActivity, rid)
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
