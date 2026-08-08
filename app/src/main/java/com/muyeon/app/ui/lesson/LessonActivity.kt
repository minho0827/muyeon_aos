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
import com.muyeon.app.webview.NativeWebRoute

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

            NavHost(nav, startDestination = route) {
                composable("calendar") {
                    val state = viewModel { LessonCalendarState(lessonApi, calendarApi) }
                    LessonCalendarScreen(
                        state = state,
                        onClose = { finish() },
                        onOpenLesson = { lid -> nav.navigate("detail/$lid") },
                        // 캘린더 관리 화면은 미이식 — 웹 폴백(스코프 E 이후).
                        onManageCalendars = { openWebAndFinish("/lessonCalendar") },
                    )
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
                    LessonWizardScreen(wizardApi, null, onClose = { back() }, onCreated = { back() })
                }
                composable("edit") {
                    LessonWizardScreen(wizardApi, id.takeIf { it > 0 }, onClose = { back() }, onCreated = { back() })
                }
                composable("edit/{id}") { e ->
                    val lid = e.arguments?.getString("id")?.toIntOrNull()
                    LessonWizardScreen(wizardApi, lid, onClose = { back() }, onCreated = { back() })
                }
                composable("slots") {
                    LessonSlotManageScreen(slotApi, id.takeIf { it > 0 }, onClose = { back() })
                }
                composable("slots/{id}") { e ->
                    LessonSlotManageScreen(slotApi, e.arguments?.getString("id")?.toIntOrNull(), onClose = { back() })
                }
                composable("booking") {
                    LessonBookingScreen(bookingApi, id, onClose = { back() }, onDone = { back() })
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
            }
        }
    }

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
