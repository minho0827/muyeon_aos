package com.muyeon.app.ui.space.wizard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.jobposting.DetailImages
import com.muyeon.app.ui.jobposting.SectionHead
import com.muyeon.app.ui.onboarding.BusinessNumberTransformation
import com.muyeon.app.ui.space.SpaceApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-18 공간 사업자 확인 단계 상태 — 등록 위저드(호스트 동의 다음)와 내 공간 [확인하기] 화면이 같이 쓴다.
 *  화면을 오가도 입력이 남게 화면 쪽에서 remember 한다.
 */
class SpaceBusinessState {
    var loading by mutableStateOf(true)
    var academy by mutableStateOf(SpaceHostAcademy(false, null, null))
    /** true = 첫 질문(학원 인증 회원만), false = 번호 입력. */
    var asking by mutableStateOf(false)
    var number by mutableStateOf("")
    var startDate by mutableStateOf("")
    var docs by mutableStateOf<List<String>>(emptyList())
    var uploading by mutableStateOf(false)
    var checking by mutableStateOf(false)
    var check by mutableStateOf<SpaceBizCheck?>(null)
    var checkFailed by mutableStateOf(false)

    val canSubmit: Boolean get() = SpaceBusinessRules.canSubmitOwn(number, startDate, check, checking, uploading)

    fun ownInput(): SpaceBusinessInput = SpaceBusinessInput.own(number, startDate, docs)

    /** 뒤로 — 번호 입력에서 첫 질문으로(학원 인증 회원). 처리했으면 true. */
    fun back(): Boolean {
        if (!loading && !asking && academy.verified) { asking = true; return true }
        return false
    }

    suspend fun load(api: SpaceApi) {
        if (!loading) return
        // 못 읽으면 학원 질문 없이 번호 입력(서버가 등록 때 다시 판정한다).
        academy = api.hostBusiness().getOrDefault(SpaceHostAcademy(false, null, null))
        asking = academy.verified
        loading = false
    }
}

/**
 * 사업자 확인 단계 본문 + 아래 버튼. ColumnScope 안에서 남은 높이를 다 쓴다.
 *  첫 질문: [네, 이 학원이에요] → ACADEMY 로 바로 onSubmit · [아니요, 다른 공간이에요] → 번호 입력.
 *  번호 입력: 10자리(개업일자 8자리면 함께)에서 450ms 뒤 자동 확인 → 확인되면 [submitLabel].
 */
@Composable
fun ColumnScope.SpaceStepBusiness(api: SpaceApi, bs: SpaceBusinessState, submitLabel: String, busy: Boolean, onSubmit: (SpaceBusinessInput) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { bs.load(api) }

    // 자동 확인(원장 인증 화면과 같은 방식) — 입력이 바뀌면 지난 결과를 지우고 450ms 뒤 다시 확인.
    LaunchedEffect(bs.number, bs.startDate, bs.asking, bs.loading) {
        bs.check = null; bs.checkFailed = false; bs.checking = false
        if (bs.loading || bs.asking) return@LaunchedEffect
        val req = SpaceBusinessRules.checkRequest(bs.number, bs.startDate) ?: return@LaunchedEffect
        delay(450)
        bs.checking = true
        try {
            api.checkBusiness(req.first, req.second).onSuccess { bs.check = it }.onFailure { e ->
                if (e is CancellationException) throw e
                bs.checkFailed = true
            }
        } finally { bs.checking = false }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = SpaceBusinessRules.MAX_DOCS)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        bs.uploading = true
        scope.launch {
            var list = bs.docs
            uris.take((SpaceBusinessRules.MAX_DOCS - list.size).coerceAtLeast(0)).forEach { u ->
                UploadImagePreparer.prepare(ctx, u)?.let { bytes -> api.uploadImage(bytes).onSuccess { list = list + it } }
            }
            bs.docs = list
            bs.uploading = false
        }
    }

    if (bs.loading) { WizardLoading(); return }

    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (bs.asking) {
            SectionHead(SpaceBusinessRules.question(bs.academy.name), "학원 인증 때 확인한 사업자 정보로 바로 등록돼요.")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StageBottomButton(SpaceBusinessRules.YES_ACADEMY, busy = busy) { onSubmit(SpaceBusinessInput.ACADEMY) }
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
                        .clickable(enabled = !busy) { bs.asking = false },
                    Alignment.Center,
                ) { Text(SpaceBusinessRules.NO_OTHER, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MuyeonColors.textHead) }
            }
            return@Column
        }

        SectionHead(SpaceBusinessRules.TITLE_OWN, "사업자등록증에 적힌 번호를 넣으면 자동으로 확인해요.")
        FormField("사업자등록번호", required = true) {
            OutlinedTextField(
                value = bs.number,
                onValueChange = { bs.number = SpaceBusinessRules.digits(it, 10) },
                visualTransformation = BusinessNumberTransformation, singleLine = true,
                placeholder = { Text("예: 123-45-67890", fontFamily = customFontFamily, color = MuyeonColors.hint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            val msg = SpaceBusinessRules.checkMessage(bs.check, bs.checkFailed)
            when {
                bs.checking -> Text("사업자등록번호 확인 중이에요", fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 17.sp, color = MuyeonColors.textSub)
                msg != null -> Text(
                    msg.first, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 17.sp,
                    color = if (msg.second) MuyeonColors.danger else MuyeonColors.primary,
                )
            }
        }
        FormField("개업일자", hint = "(선택)") {
            OutlinedTextField(
                value = bs.startDate,
                onValueChange = { bs.startDate = SpaceBusinessRules.digits(it, 8) },
                singleLine = true,
                placeholder = { Text("예: 20200115 (사업자등록증의 개업연월일)", fontFamily = customFontFamily, color = MuyeonColors.hint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(SpaceBusinessRules.HELP, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub)
        }
        DetailImages(
            images = bs.docs, uploading = bs.uploading,
            onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemove = { i -> bs.docs = bs.docs.filterIndexed { idx, _ -> idx != i } },
            title = "사업자등록증 사진 (선택)", hint = "최대 ${SpaceBusinessRules.MAX_DOCS}장까지 올릴 수 있어요.", max = SpaceBusinessRules.MAX_DOCS,
        )
    }
    if (!bs.asking) {
        Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            StageBottomButton(submitLabel, enabled = bs.canSubmit, busy = busy) { onSubmit(bs.ownInput()) }
        }
    }
}

