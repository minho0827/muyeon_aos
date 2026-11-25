package com.muyeon.app.ui.castingstage.screens

import com.muyeon.app.util.runCatchingCancellable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.launch
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.components.*

/** 캐스팅 공고 상세(iOS StageCastingDetailView). */

@Composable
fun StageCastingDetailScreen(
    api: CastingStageApi, id: Int, onBack: () -> Unit, onOpenUrl: (String) -> Unit, onWriteResume: () -> Unit,
    /** 작성자 [수정] → 등록 흐름 수정 모드(CastingEditFlow). */
    onEdit: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var c by remember { mutableStateOf<CastingDetail?>(null) }
    var failed by remember { mutableStateOf<String?>(null) }
    var liked by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    val resumes = remember { mutableStateListOf<StageResume>() }
    var resumeId by remember { mutableStateOf<Int?>(null) }
    var showApply by remember { mutableStateOf(false) }
    var showNoResume by remember { mutableStateOf(false) }
    // ★ 2026-11-16 캐스팅 지원은 필요 유형표(CASTING_APPLY = 무용수) + 공통 관문. "강사면 무용수로 전환" 특별 규칙은 없앴다.
    val gate = com.muyeon.app.ui.common.rememberTypeGate()
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id, com.muyeon.app.auth.accountEpoch()) {
        liked = false
        runCatchingCancellable { api.detail(id) }.onSuccess { c = it }.onFailure { failed = it.message ?: "공고를 찾을 수 없어요." }
        applied = api.applied(id)
        liked = id in api.scrappedIds()
    }

    // 작성자 동작 — 내 공고 관리와 같은 마감·삭제(확인창·서버 문구 그대로). 마감 뒤엔 다시 읽고, 삭제 뒤엔 닫는다.
    val myId = remember { com.muyeon.app.ui.common.NativeHttp(ctx).myUserId() }
    val postingApi = remember { com.muyeon.app.ui.jobposting.JobPostingApi(api.token) }
    val ownerActions = com.muyeon.app.ui.jobposting.rememberPostingActions(postingApi) { _, change ->
        com.muyeon.app.navigation.AppResults.publish(setOf(com.muyeon.app.result.ResultKeys.JOB_POSTINGS))
        if (change == com.muyeon.app.ui.jobposting.PostingChange.DELETED) {
            android.widget.Toast.makeText(ctx, com.muyeon.app.R.string.owner_posting_deleted, android.widget.Toast.LENGTH_SHORT).show()
            onBack()
        } else scope.launch { runCatchingCancellable { api.detail(id) }.onSuccess { c = it } }
    }

    suspend fun startApply() {
        applying = true
        val list = api.dancerResumes()
        applying = false
        resumes.clear(); resumes.addAll(list)
        if (list.isEmpty()) { showNoResume = true; return }
        resumeId = (list.firstOrNull { it.isDefault } ?: list.first()).id
        showApply = true
    }

    val detail = c
    if (detail == null) {
        Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
            Box(Modifier.size(44.dp).clickable(onClick = onBack), Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead) }
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                if (failed != null) Text(failed!!, style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub) else CircularProgressIndicator(color = Brand)
            }
        }
        return
    }
    val dl = StageDate.deadline(detail.d("deadline"))
    // ★ 2026-11-25 마감일 없음('-'·빈 값)은 마감이 아니다 — 날짜로 읽히는데 지난 경우만 마감.
    val closed = detail.status == "CLOSED" || (StageDate.short(detail.d("deadline")) != null && dl == null)
    val sections = buildList {
        detail.d("summary")?.let { add(StageDetailSection("summary", "공고 요약") { StageExpandableText(it) }) }
        val cond = listOf(
            "모집 역할" to detail.role, "장르" to detail.genre, "인원" to detail.headcount.takeIf { it > 0 }?.let { "${it}명" },
            "성별" to detail.gender, "나이" to detail.d("age"), "키" to detail.height, "출연료" to detail.d("fee"),
            "영상" to (if (detail.videoRequired) "영상 제출 필수" else null), "수상 경력" to (if (detail.awardPreferred) "수상 경력 우대" else null),
        ).mapNotNull { (k, v) -> v?.let { k to it } }
        if (cond.isNotEmpty()) add(StageDetailSection("cond", "모집 조건") { StageInfoRows(cond) })
        val how = listOf("마감" to (StageDate.short(detail.d("deadline")) ?: "마감일 없음"), "지원 방법" to detail.d("applyMethod"), "문의" to detail.d("contact"))
            .mapNotNull { (k, v) -> v?.let { k to it } }
        if (how.isNotEmpty()) add(StageDetailSection("how", "지원 방법") { StageInfoRows(how) })
        if (detail.isExternal) add(StageDetailSection("source", "정보 출처") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StageInfoRows(listOf("출처" to (detail.d("sourceName") ?: "웹"), "수집일" to (StageDate.short(detail.d("collectedAt")) ?: "-")))
                Text("무용연이 웹에서 모은 공고예요. 조건과 마감일은 바뀔 수 있으니 지원 전에 원문을 꼭 확인해 주세요.", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
            }
        })
    }
    StageDetailScaffold(
        title = detail.title,
        shareText = "${detail.title}\n${BuildConfig.API_BASE_URL}/casting/${detail.id}",
        liked = liked,
        onLike = {
            // ★ 11-17 비회원 — 하트가 칠해졌다가 조용히 되돌아갔다. 로그인으로 보내고 이 공고로 돌아오게.
            if (myId == null) { com.muyeon.app.ui.auth.LoginActivity.start(ctx, "/casting/$id"); return@StageDetailScaffold }
            val next = !liked
            liked = next
            scope.launch { if (!api.setScrap(id, next)) liked = !next }
        },
        onBack = onBack,
        sections = sections,
        header = {
            Column {
                StageHeroGallery(detail.photos, Icons.Filled.Star, Color(0xFFFFA262), Color(0xFFFFF3EA), placeholder = com.muyeon.app.R.drawable.stage_noimage_casting)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (closed) stageChipText("마감", MuyeonColors.textSub, Ink100)()
                        else dl?.let { stageChipText(it, Blue, Color(0xFFFFF3EA))() }
                        detail.genre?.let { stageChipText(it, MuyeonColors.textHead, Ink100)() }
                        if (detail.isExternal) stageChipText("웹 공고", MuyeonColors.textSub, Ink100)()
                    }
                    Text(detail.title, style = Bold, fontSize = 24.sp, color = MuyeonColors.textHead)
                    detail.team?.let { Text(it, style = Regular, fontSize = 16.sp, color = MuyeonColors.textSub) }
                }
                val rows = listOf("일정" to detail.schedule, "장소" to (detail.place ?: detail.region), "마감" to (StageDate.short(detail.d("deadline")) ?: "마감일 없음"))
                    .mapNotNull { (k, v) -> v?.let { k to it } }
                if (rows.isNotEmpty()) Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) { StageInfoRows(rows) }
            }
        },
        bottom = {
            // 하단 — 작성자/지원자 둘 중 하나를 고르기만 한다.
            val ref = com.muyeon.app.ui.jobposting.PostingRef("CASTING", id)
            if (myId != null && detail.authorId == myId) StageCastingOwnerBar(
                open = !closed, busy = ownerActions.busy,
                onDelete = { ownerActions.requestDelete(ref) }, onClose = { ownerActions.requestClose(ref) },
                onEdit = onEdit,
            )
            else StageCastingApplyButton(detail, closed, applied, applying, loggedIn = myId != null, onOpenUrl,
                onApply = { gate.require(com.muyeon.domain.model.member.TypeFeature.CASTING_APPLY) { scope.launch { startApply() } } })
        },
    )

    ownerActions.Dialogs()
    com.muyeon.app.ui.common.TypeGateHost(gate)
    if (showNoResume) AlertDialog(
        onDismissRequest = { showNoResume = false },
        title = { Text("저장된 이력서가 없어요. 작성할까요?", style = Bold) },
        confirmButton = { TextButton({ showNoResume = false; onWriteResume() }) { Text("이력서 작성하기", color = Brand) } },
        dismissButton = { TextButton({ showNoResume = false }) { Text("취소", color = MuyeonColors.textSub) } },
    )
    if (showApply) AlertDialog(
        onDismissRequest = { if (!applying) showApply = false },
        title = { Text("지원할까요?", style = Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("선택한 이력서로 이 공고에 지원해요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub)
                resumes.forEach { r ->
                    val on = r.id == resumeId
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, if (on) Brand else Ink300, RoundedCornerShape(12.dp))
                            .clickable { resumeId = r.id }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(if (on) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, null, tint = if (on) Brand else MuyeonColors.textSub)
                        Text((r.title?.takeIf { it.isNotBlank() } ?: "이력서") + if (r.isDefault) " (기본)" else "", style = Medium, fontSize = 15.sp, color = MuyeonColors.textHead)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !applying, onClick = {
                scope.launch {
                    applying = true
                    runCatchingCancellable { api.apply(id, resumeId) }
                        .onSuccess { applied = true; toast = "지원이 접수됐어요." }
                        .onFailure { toast = it.message ?: "지원에 실패했어요. 잠시 후 다시 시도해 주세요." }
                    applying = false
                    showApply = false
                }
            }) { Text(if (applying) "지원하는 중…" else "지원하기", color = Brand) }
        },
        dismissButton = { TextButton({ showApply = false }) { Text("취소", color = MuyeonColors.textSub) } },
    )
    toast?.let { msg ->
        AlertDialog(onDismissRequest = { toast = null }, text = { Text(msg, style = Regular) },
            confirmButton = { TextButton({ toast = null }) { Text("확인", color = Brand) } })
    }
}

/** 지원자 하단 버튼 — 마감 / 원문 공고(웹 수집) / 지원함 / 이력서로 지원(로그인 회원, 유형은 관문이 본다) / 비회원 안내. */
@Composable
private fun StageCastingApplyButton(
    detail: CastingDetail, closed: Boolean, applied: Boolean, applying: Boolean, loggedIn: Boolean,
    onOpenUrl: (String) -> Unit, onApply: () -> Unit,
) {
    when {
        closed -> StageBottomButton("마감된 공고예요", enabled = false) {}
        detail.isExternal -> {
            val url = detail.d("applyUrl") ?: detail.d("sourceUrl")
            StageBottomButton(if (detail.d("applyUrl") != null) "원문에서 지원하기" else "원문 공고 보러 가기", enabled = url != null) { url?.let(onOpenUrl) }
        }
        applied -> StageBottomButton("지원한 공고예요", enabled = false) {}
        loggedIn -> StageBottomButton("이력서로 지원하기", busy = applying) { onApply() }
        // ★ 11-17 비회원 — 회색 '무용수 회원만…' 대신 누를 수 있게. onApply 의 유형 관문(TypeGate)이 비회원을 로그인으로 보낸다.
        else -> StageBottomButton("로그인하고 지원하기") { onApply() }
    }
}

// MARK: 소식 상세
