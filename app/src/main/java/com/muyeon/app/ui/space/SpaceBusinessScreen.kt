package com.muyeon.app.ui.space

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.muyeon.app.ui.common.ApiMessageError
import com.muyeon.app.ui.common.market.MarketMessageDialog
import com.muyeon.app.ui.common.market.WizardTopBar
import com.muyeon.app.ui.space.wizard.SpaceBusinessState
import com.muyeon.app.ui.space.wizard.SpaceStepBusiness
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-18 내 공간 [확인하기]·[다시 확인하기] — 등록 위저드의 사업자 확인 단계만 띄운다.
 *  제출 = PATCH /spaces/:id { business } → onDone(bizStatus). 실패하면 서버 문구 그대로(학원 미인증이면 번호 입력으로).
 */
@Composable
fun SpaceBusinessScreen(api: SpaceApi, spaceId: Int, onClose: () -> Unit, onDone: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val bs = remember { SpaceBusinessState() }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler { if (!bs.back()) onClose() }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        WizardTopBar("사업자 확인", onClose)
        SpaceStepBusiness(api, bs, submitLabel = "확인 요청하기", busy = saving) { input ->
            if (saving) return@SpaceStepBusiness
            saving = true
            scope.launch {
                api.updateBusiness(spaceId, input).onSuccess { onDone(it.bizStatus) }.onFailure { e ->
                    if ((e as? ApiMessageError)?.code == "SPACE_BUSINESS_ACADEMY_NOT_VERIFIED") bs.asking = false
                    error = e.message ?: "사업자 정보를 보내지 못했어요."
                }
                saving = false
            }
        }
    }
    error?.let { msg -> MarketMessageDialog(msg) { error = null } }
}
