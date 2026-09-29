package com.muyeon.app.ui.academy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.utils.TokenManager

/**
 * 학원↔강사 소속 컨테이너 — 웹 `openAcademyTeachers`(학원) / `openAcademyInvites`(강사) 진입점.
 *  iOS `WebViewModel+Academy.swift` 의 presentAcademyTeachers / presentAcademyInvites 대응.
 *
 *  닫을 때 웹에 변경을 알린다(결과 키 ACADEMY) — iOS presentAcademyFull 과 동일.
 */
class AcademyMembershipActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.ACADEMY)

    companion object {
        private const val EXTRA_ROUTE = "route"

        fun teachersIntent(context: Context) = intent(context, "teachers")
        fun invitesIntent(context: Context) = intent(context, "invites")

        /** [학원] 소속 강사 관리. */
        fun startTeachers(context: Context) = context.launchScreen(teachersIntent(context))

        /** [강사] 학원 소속 신청·관리. */
        fun startInvites(context: Context) = context.launchScreen(invitesIntent(context))

        private fun intent(context: Context, route: String) =
            Intent(context, AcademyMembershipActivity::class.java).putExtra(EXTRA_ROUTE, route)
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
