package com.muyeon.app.ui.academy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.muyeon.app.utils.TokenManager

/**
 * 학원↔강사 소속 컨테이너 — 웹 `openAcademyTeachers`(학원) / `openAcademyInvites`(강사) 진입점.
 *  iOS `WebViewModel+Academy.swift` 의 presentAcademyTeachers / presentAcademyInvites 대응.
 *
 *  닫을 때 웹에 변경을 알린다(WebCallbacks.academyChanged) — iOS presentAcademyFull 과 동일.
 */
class AcademyMembershipActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"

        /** [학원] 소속 강사 관리. */
        fun startTeachers(context: Context) = context.go(intent(context, "teachers"))

        /** [강사] 학원 소속 신청·관리. */
        fun startInvites(context: Context) = context.go(intent(context, "invites"))

        private fun intent(context: Context, route: String) =
            Intent(context, AcademyMembershipActivity::class.java).putExtra(EXTRA_ROUTE, route)

        private fun Context.go(i: Intent) {
            if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "invites"
        setContent {
            val api = remember { AcademyTeacherApi(TokenManager.getAccessToken(this)) }
            if (route == "teachers") {
                AcademyTeachersScreen(api = api, onClose = { finish() })
            } else {
                AcademyInvitesScreen(api = api, onClose = { finish() })
            }
        }
    }
}
