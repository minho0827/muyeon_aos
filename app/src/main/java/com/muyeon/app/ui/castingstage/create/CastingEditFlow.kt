package com.muyeon.app.ui.castingstage.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.muyeon.app.ui.castingstage.data.CastingStageApi
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.util.runCatchingCancellable
import org.json.JSONObject
import java.time.LocalDate

/**
 * 공연 공고 수정 — 등록 흐름(CastingCreateScreen)을 수정 모드로 다시 쓴다. 웹 /casting/:id/edit 대신.
 *  GET /casting/:id 로 네 단계를 모두 채우고, 저장은 PATCH /casting/:id(바꾼 칸만).
 *  서버 수정이 받지 않는 칸(리허설·키·전공·출연료 구간·리허설비·교통비·제출물)은 보이되 잠근다 — [CastingEditRules].
 */
@Composable
fun CastingEditFlow(
    api: CastingStageApi,
    castingId: Int,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    var original by remember { mutableStateOf<CastingCreateForm?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(castingId) {
        runCatchingCancellable { CastingEditRules.formFrom(api.castingJson(castingId)) }
            .onSuccess { original = it }
            .onFailure { error = it.message?.takeIf { m -> m.isNotBlank() } ?: "공고를 불러오지 못했어요." }
    }
    val o = original
    if (o != null) {
        CastingCreateScreen(
            api = api, onClose = onClose, onCreated = { onSaved() },
            onOpenMembership = {}, edit = CastingEdit(castingId, o),
        )
    } else {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary) }
        }
    }
    error?.let { msg ->
        MuyeonDialog(
            title = "알림", message = msg,
            actions = listOf(MuyeonDialogAction("확인") { onClose() }),
            onDismiss = { error = null; onClose() },
        )
    }
}

/** 수정 모드 값 — 고칠 공고 id + 불러온 원본(바뀐 칸 비교용). */
data class CastingEdit(val id: Int, val original: CastingCreateForm)

/**
 * 수정 규칙 — 서버 casting.service update 와 같다.
 *  받는 칸: title(2자 이상)·genre·gender·region·place·schedule·role·headcount·imageUrl·deadline(YYYY-MM-DD)·fee·age·applyMethod·contact·summary.
 */
object CastingEditRules {
    private val ISO = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    /** GET /casting/:id → 등록 폼 값. */
    fun formFrom(c: JSONObject): CastingCreateForm {
        val d = c.optJSONObject("details") ?: JSONObject()
        fun JSONObject.s(k: String): String = if (isNull(k)) "" else optString(k).takeIf { it != "null" }.orEmpty().trim()
        fun date(v: String): LocalDate? = v.takeIf { ISO.matches(it) }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val rehearsal = d.s("rehearsal").split("~").map { it.trim() }
        val deadline = d.s("deadline")
        val feeRange = d.s("feeRange").ifEmpty { null }
        // 서버는 부가설명이 없으면 구간 이름을 fee 에 넣어 둔다 — 그 경우 부가설명 칸은 비운다.
        val fee = d.s("fee").takeIf { it != CastingCreateOptions.feeLabel(feeRange) }.orEmpty()
        val req = d.optJSONObject("requirements")
        return CastingCreateForm(
            title = c.s("title"),
            genre = c.s("genre").ifEmpty { null },
            place = c.s("place"),
            region = c.s("region"),
            regionCode = c.s("regionCode").ifEmpty { null },
            imageUrl = c.s("imageUrl").ifEmpty { null },
            schedule = date(c.s("schedule")),
            rehearsalStart = rehearsal.getOrNull(0)?.let(::date),
            rehearsalEnd = rehearsal.getOrNull(1)?.let(::date),
            deadline = date(deadline),
            deadlineUndecided = deadline == "-",
            roles = c.s("role").split(",").map { it.trim() }.filter { it.isNotEmpty() },
            headcount = if (c.has("headcount") && !c.isNull("headcount")) c.optInt("headcount").toString() else "",
            gender = c.s("gender").ifEmpty { null },
            age = d.s("age"),
            height = c.s("height"),
            major = d.s("major").ifEmpty { null },
            feeRange = feeRange,
            fee = fee,
            rehearsalFee = d.optBoolean("rehearsalFee"),
            transportFee = d.optBoolean("transportFee"),
            videoRequired = req?.optBoolean("video") ?: c.optBoolean("videoRequired"),
            photoRequired = req?.optBoolean("photo") ?: false,
            awardRequired = req?.optBoolean("award") ?: c.optBoolean("awardPreferred"),
            careerRequired = req?.optBoolean("career") ?: false,
            introRequired = req?.optBoolean("intro") ?: false,
            desc = d.s("summary").ifEmpty { d.s("description") },
            applyMethod = d.s("applyMethod"),
            contact = d.s("contact"),
        )
    }

    /** 서버 PATCH 가 받는 칸만 값으로. 비교·전송 둘 다 이 표를 쓴다. */
    private fun patchable(f: CastingCreateForm): Map<String, Any> = mapOf(
        "title" to f.trimmedTitle,
        "genre" to f.genre.orEmpty(),
        "gender" to f.gender.orEmpty(),
        "region" to f.region.trim(),
        "place" to f.place.trim(),
        "schedule" to (f.schedule?.toString() ?: ""),
        "role" to f.roles.joinToString(", "),
        "headcount" to (f.headcount.toIntOrNull() ?: 0).coerceIn(0, 500),
        "imageUrl" to f.imageUrl.orEmpty(),
        // 미정·빈칸은 ""(서버는 YYYY-MM-DD 아니면 거절).
        "deadline" to (f.deadline?.takeIf { !f.deadlineUndecided }?.toString() ?: ""),
        "fee" to f.fee.trim(),
        "age" to f.age.trim(),
        "applyMethod" to f.applyMethod.trim(),
        "contact" to f.contact.trim(),
        "summary" to f.desc.trim(),
    )

    /** 바뀐 칸만 담은 PATCH 본문(웹 CastingEdit 과 같다). 바뀐 게 없으면 빈 객체. */
    fun patchBody(original: CastingCreateForm, edited: CastingCreateForm): JSONObject {
        val before = patchable(original)
        return JSONObject().apply { patchable(edited).forEach { (k, v) -> if (before[k] != v) put(k, v) } }
    }

    /** 앱에서 먼저 막는 규칙(서버와 같은 문구). 통과하면 null. */
    fun validate(edited: CastingCreateForm): String? = when {
        edited.trimmedTitle.length < 2 -> "공고 제목을 2자 이상 적어 주세요."
        else -> null
    }
}
