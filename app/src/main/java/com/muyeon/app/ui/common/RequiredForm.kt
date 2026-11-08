package com.muyeon.app.ui.common

import android.widget.Toast
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-08 등록 화면 공통 필수값 확인(사용자 요청 — 모든 등록 화면이 같이 쓴다).
 *   필수 칸마다 [requiredField] 로 이름·채워졌는지를 알려 두고, 등록(다음) 버튼에서 [validate] 를 부른다.
 *   빠진 칸이 있으면: ① 그 칸으로 스크롤 ② 칸 이름을 빨갛게 + '필수 항목이에요'([RequiredHint]) ③ "○○을(를) 입력해 주세요." 토스트.
 *   칸을 채우면 강조는 저절로 사라진다. 순서는 화면에 그려진 순서(위 → 아래)다.
 */
@Stable
class RequiredForm internal constructor(private val scope: CoroutineScope) {
    private class Entry(var label: String, var filled: Boolean, var group: String?, val requester: BringIntoViewRequester)
    private val entries = LinkedHashMap<String, Entry>()
    /** 지금 강조 중인 칸. */
    var errorKey by mutableStateOf<String?>(null)
        private set

    internal fun update(key: String, label: String, filled: Boolean, group: String?, requester: BringIntoViewRequester) {
        val e = entries.getOrPut(key) { Entry(label, filled, group, requester) }
        e.label = label; e.filled = filled; e.group = group
        if (filled && errorKey == key) errorKey = null
    }

    internal fun remove(key: String) { entries.remove(key) }

    fun isError(key: String) = errorKey == key

    /** 빠진 첫 칸의 이름(없으면 null). [group] 을 주면 그 묶음(위저드 단계)만 본다. */
    fun firstMissingLabel(group: String? = null): String? = firstMissing(group)?.value?.label

    private fun firstMissing(group: String?) = entries.entries.firstOrNull { (_, e) -> !e.filled && (group == null || e.group == group) }

    /** 다 채웠으면 true. 아니면 첫 빈 칸으로 스크롤·강조·토스트 후 false. */
    fun validate(context: android.content.Context, group: String? = null): Boolean {
        val miss = firstMissing(group) ?: run { errorKey = null; return true }
        errorKey = miss.key
        scope.launch { runCatching { miss.value.requester.bringIntoView() } }
        Toast.makeText(context, context.getString(R.string.required_missing, miss.value.label), Toast.LENGTH_SHORT).show()
        return false
    }
}

@Composable
fun rememberRequiredForm(): RequiredForm {
    val scope = rememberCoroutineScope()
    return remember { RequiredForm(scope) }
}

/** 필수 칸 표시 — 이 칸으로 스크롤할 수 있게 하고, 이름·채워졌는지를 알린다. [group] = 위저드 단계 등. */
@Composable
fun Modifier.requiredField(form: RequiredForm, key: String, label: String, filled: Boolean, group: String? = null): Modifier {
    val requester = remember(key) { BringIntoViewRequester() }
    SideEffect { form.update(key, label, filled, group, requester) }
    androidx.compose.runtime.DisposableEffect(key) { onDispose { form.remove(key) } }
    return this.bringIntoViewRequester(requester)
}

/** 빠진 칸 아래 빨간 안내 — 강조 중일 때만 보인다. */
@Composable
fun RequiredHint(form: RequiredForm, key: String) {
    if (form.isError(key)) Text(stringResource(R.string.required_hint), style = AppTypography.Caption12, color = AppColors.Danger)
}

/** 칸 이름 색 — 강조 중이면 빨강. */
fun RequiredForm.labelColor(key: String) = if (isError(key)) AppColors.Danger else AppColors.TextHead

/** 화면이 내려 주는 필수값 확인기 — 공용 FormField 가 읽는다(없으면 확인하지 않는다). */
val LocalRequiredForm = androidx.compose.runtime.staticCompositionLocalOf<RequiredForm?> { null }
