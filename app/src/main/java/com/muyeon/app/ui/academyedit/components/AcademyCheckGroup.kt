package com.muyeon.app.ui.academyedit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormField

/**
 * 여러 개 고르는 칸 — 웹 CheckGroup(라벨 '(복수 선택)' + 안내 + 체크 목록). 앱 등록 화면 공용 칩(FormChipWrap)으로 그린다.
 *   [options] = (보낼 값, 보이는 이름) · 순서는 웹 목록 그대로.
 */
@Composable
fun AcademyCheckGroup(
    label: String,
    options: List<Pair<String, String>>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    hint: String? = null,
) {
    FormField(label, hint = stringResource(R.string.academy_edit_multi)) {
        if (hint != null) AcademyFieldGuide(hint)
        FormChipWrap(options, isOn = { it in selected }, onTap = onToggle)
    }
}
