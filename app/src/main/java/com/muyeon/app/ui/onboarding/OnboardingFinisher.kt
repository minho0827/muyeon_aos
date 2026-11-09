package com.muyeon.app.ui.onboarding

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute
import com.muyeon.app.webview.WebCallbackQueue
import com.muyeon.domain.model.auth.RoleSubmit
import com.muyeon.domain.usecase.auth.CompleteRoleOnboardingUseCase
import kotlinx.coroutines.launch
import org.json.JSONArray

/** 인증 화면이 넘긴 서류 한 묶음. */
data class VerifyDocs(
    val role: String,
    val urls: List<String>,
    val academyName: String?,
    val businessNumber: String? = null,
    val startDate: String? = null,
) {
    /** ★ 2026-10-22 학원은 사진 없이 사업자등록번호만으로도 제출된다(국세청 진위확인 자동 승인). */
    val submitted: Boolean get() = urls.isNotEmpty() || businessNumber != null
}

/**
 * ★ 2026-11-09 회원유형·인증을 마친 뒤 할 일 — 연 곳마다 하나씩.
 *   웹 창이 열었으면 [WebOnboardingFinisher](웹 콜백), 앱 화면이 열었으면 [InAppOnboardingFinisher](앱이 직접 API).
 *   OnboardingActivity 는 시작할 때 하나만 고르고, 화면 코드는 이것만 부른다.
 */
interface OnboardingFinisher {
    /** 인증 화면 직행(openRoleVerification)에서 제출. */
    fun verified(d: VerifyDocs)
    /** 유형 관리 → 인증에서 제출(서류가 있을 때만 부른다). 끝나면 관리 화면까지 닫는다. */
    fun manageVerified(d: VerifyDocs)
    /** 가입 직후 유형 선택(+서류) 완료. */
    fun roleChosen(d: VerifyDocs)

    companion object {
        fun of(activity: ComponentActivity, inApp: Boolean, completeRole: CompleteRoleOnboardingUseCase): OnboardingFinisher =
            if (inApp) InAppOnboardingFinisher(activity, completeRole) else WebOnboardingFinisher(activity)
    }
}

/** 웹 창이 연 경우 — 웹 콜백(__onRoleVerify 등)이 제출·이동을 한다(iOS 계약). */
class WebOnboardingFinisher(private val activity: ComponentActivity) : OnboardingFinisher {
    override fun verified(d: VerifyDocs) = NativeWebRoute.notifyWebAndFinish(activity, OnboardingJs.verify("__onRoleVerify", d))

    // 큐에 쌓인 콜백은 웹뷰가 앞으로 나올 때 실행된다.
    override fun manageVerified(d: VerifyDocs) {
        WebCallbackQueue.enqueue(activity, OnboardingJs.verify("__onRoleManageVerify", d))
        activity.finish()
    }

    override fun roleChosen(d: VerifyDocs) = NativeWebRoute.notifyWebAndFinish(activity, OnboardingJs.verify("__onRoleComplete", d))
}

/** 앱 화면이 연 경우 — 같은 API 를 앱이 부르고, 닫으면 연 앱 화면으로 돌아간다. 웹 창은 띄우지 않는다. */
class InAppOnboardingFinisher(
    private val activity: ComponentActivity,
    private val completeRole: CompleteRoleOnboardingUseCase,
) : OnboardingFinisher {
    private var busy = false

    override fun verified(d: VerifyDocs) = submitAndClose(d)
    override fun manageVerified(d: VerifyDocs) = submitAndClose(d)

    /** 역할 추가 → 서류 제출 → 유형별 화면(ROLE_LANDING · 심사 안내). */
    override fun roleChosen(d: VerifyDocs) = once {
        val to = completeRole(d.role, d.urls, d.academyName, d.businessNumber, d.startDate?.filter(Char::isDigit))
        ActiveRole.syncFromServer(activity)
        com.muyeon.app.ui.main.MainActivity.openPath(activity, to)
        activity.finish()
    }

    /** 서류 제출 뒤 보유 유형·차단 상태를 다시 묻고 닫는다. 실패하면 화면에 남아 다시 누를 수 있다. */
    private fun submitAndClose(d: VerifyDocs) = once {
        val r = completeRole.submit(d.role, d.urls, d.academyName, d.businessNumber, d.startDate?.filter(Char::isDigit))
        if (r == RoleSubmit.NONE && d.submitted) {
            Toast.makeText(activity, "제출하지 못했어요. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            busy = false
            return@once
        }
        ActiveRole.syncFromServer(activity)
        com.muyeon.app.webview.VerifyGate.refresh(activity)
        if (r != RoleSubmit.NONE) {
            val msg = if (r == RoleSubmit.APPROVED) "인증이 완료됐어요." else "서류를 제출했어요. 심사 후 알려드릴게요."
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
        activity.finish()
    }

    private fun once(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        activity.lifecycleScope.launch { block() }
    }
}

/** 웹 콜백 JS 한 줄. */
object OnboardingJs {
    fun esc(s: String) = s.replace("\\", "\\\\").replace("'", "\\'")

    /**
     * (역할, 서류 URL **JSON 문자열**, 학원명, 사업자번호, 개업일자 YYYYMMDD)
     *  ★ 웹은 2번째 인자를 `JSON.parse` 한다 — 배열 리터럴을 넘기면 서류가 한 장도 제출되지 않는다(iOS 와 같은 문자열 계약).
     */
    fun verify(fn: String, d: VerifyDocs): String {
        val json = JSONArray(d.urls).toString()
        return "if(window.$fn){ window.$fn('${esc(d.role)}', '${esc(json)}', '${esc(d.academyName.orEmpty())}', '${esc(d.businessNumber.orEmpty())}', '${esc(d.startDate.orEmpty().filter(Char::isDigit))}'); }"
    }
}
