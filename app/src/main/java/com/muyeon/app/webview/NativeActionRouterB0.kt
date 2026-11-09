package com.muyeon.app.webview

import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.muyeon.app.utils.TokenManager
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.usecase.member.GetRoleManageLaunchUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.java.KoinJavaComponent

/**
 * ★ 2026-11-08 묶음 0 — 경로표(NativeRoutesB0)가 쓰는 앱 안 전용 액션(웹 브리지 계약 아님 — 웹은 보내지 않는다).
 *   [NativeActionRouter.open] 이 맨 먼저 이 표를 본다. iOS Shell/Navigation/NativeActionRouter+B0.swift 와 같은 이름·같은 동작.
 *   이미 있는 앱 화면만 연다(새 화면 없음). 로그인이 필요한 화면은 비회원이면 앱 로그인부터(돌아오면 그 주소로).
 */
object NativeActionRouterB0 {
    /** 웹 IDENTITY_ROLES — 서류 없이 본인 인증만으로 부여되는 유형(인증 화면 대신 유형 관리에서 추가). */
    private val IDENTITY_ROLES = setOf("TEACHER", "DANCER")

    fun open(activity: Activity, action: String, d: JSONObject): Boolean {
        when (action) {
            "openFieldVisibilityLink" -> loginFirst(activity, "/fieldVisibility") {
                com.muyeon.app.ui.resume.ResumeActivity.startVisibility(activity, resumeMode(activity, d.optString("as")))
            }
            // 웹 nativeLink /resumeEdit — id 가 있으면 그 이력서를 읽어 저장된 역할(강사·무용수)로 연다.
            "openResumeEditLink" -> loginFirst(activity, d.optString("redirect").ifEmpty { "/resumeEdit" }) {
                val id = d.optString("id").toIntOrNull()
                val seek = d.optString("seekProfile") == "1"
                val asMode = resumeMode(activity, d.optString("as"))
                if (id == null) com.muyeon.app.ui.resume.ResumeActivity.startEdit(activity, null, asMode, seekProfile = seek)
                else scope(activity).launch {
                    val doc = com.muyeon.app.ui.resume.ResumeApi(TokenManager.getAccessToken(activity)).getOne(id).getOrNull()
                    val mode = when (doc?.data?.roleIntent) { "DANCER" -> "dancer"; "TEACHER" -> "teacher"; else -> asMode }
                    com.muyeon.app.ui.resume.ResumeActivity.startEdit(activity, id, mode, seekProfile = seek)
                }
            }
            "openJobPostingEdit" -> loginFirst(activity, d.optString("redirect")) {
                val jid = d.optString("jobId").toIntOrNull() ?: return true
                com.muyeon.app.ui.jobposting.JobPostingActivity.startForm(activity, jid)
            }
            "openLessonSettings" -> loginFirst(activity, "/lessonSettings") {
                com.muyeon.app.ui.lesson.LessonActivity.startSettings(activity)
            }
            "openStudioMemberDetail" -> loginFirst(activity, d.optString("redirect")) {
                val mid = d.optString("memberId").toIntOrNull() ?: return true
                com.muyeon.app.ui.studio.StudioActivity.startMember(activity, mid)
            }
            // 회원유형 관리 — 보유 유형·배경 이미지를 서버에서 받아 연다(웹 util/openMemberType · 셸 MY '유형 관리'와 같다).
            //  ?verify=ROLE(학원·공연팀 등) 이면 그 유형 인증 화면(웹 goRoleVerification). 강사·무용수는 유형 관리에서 추가.
            "openMemberType" -> loginFirst(activity, d.optString("redirect").ifEmpty { "/my/memberType" }) {
                val verify = d.optString("verify").uppercase()
                if (verify.isNotEmpty() && verify !in IDENTITY_ROLES) {
                    com.muyeon.app.ui.onboarding.OnboardingActivity.startVerification(activity, verify, completeInApp = true)
                } else scope(activity).launch {
                    val uc = KoinJavaComponent.get<GetRoleManageLaunchUseCase>(GetRoleManageLaunchUseCase::class.java)
                    when (val r = uc()) {
                        is ApiResult.Success -> com.muyeon.app.ui.onboarding.OnboardingActivity.startRoleManage(
                            activity, r.data.rolesJson, r.data.heroImage, ActiveRole.current(activity), completeInApp = true,
                        )
                        is ApiResult.Error -> Toast.makeText(activity, r.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "openAccountRecovery" -> com.muyeon.app.ui.auth.LoginActivity.startRecovery(activity, d.optString("type"))
            // 웹 community/:category — 앱 커뮤니티 목록을 그 카테고리 칩으로.
            "openCommunityCategory" -> {
                val prefs = KoinJavaComponent.get<KeyValueStore>(KeyValueStore::class.java)
                com.muyeon.app.ui.community.CommunityListViewModel.preselect(prefs, d.optString("category"))
                com.muyeon.app.ui.main.MainActivity.openPath(activity, "/community")
            }
            else -> return false
        }
        return true
    }

    /** 웹 nativeLink resumeMode — as=dancer|teacher 가 우선, 없으면 지금 유형. */
    private fun resumeMode(activity: Activity, asQuery: String): String = when (asQuery) {
        "dancer" -> "dancer"
        "teacher" -> "teacher"
        else -> if (ActiveRole.isDancer(activity)) "dancer" else "teacher"
    }

    private inline fun loginFirst(activity: Activity, redirect: String, open: () -> Unit) {
        if (TokenManager.getAccessToken(activity).isNullOrEmpty()) {
            com.muyeon.app.ui.auth.LoginActivity.start(activity, redirect.ifEmpty { null })
        } else open()
    }

    private fun scope(activity: Activity): CoroutineScope =
        (activity as? ComponentActivity)?.lifecycleScope ?: MainScope()
}
