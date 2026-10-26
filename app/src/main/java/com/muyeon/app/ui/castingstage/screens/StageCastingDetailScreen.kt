package com.muyeon.app.ui.castingstage.screens

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
import com.muyeon.app.webview.ActiveRole
import kotlinx.coroutines.launch
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.components.*

/** 캐스팅 공고 상세(iOS StageCastingDetailView). */

@Composable
fun StageCastingDetailScreen(api: CastingStageApi, id: Int, onBack: () -> Unit, onOpenUrl: (String) -> Unit, onWriteResume: () -> Unit) {
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
    var showSwitch by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        runCatching { api.detail(id) }.onSuccess { c = it }.onFailure { failed = it.message ?: "공고를 찾을 수 없어요." }
        applied = api.applied(id)
        liked = id in api.scrappedIds()
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
    val closed = detail.status == "CLOSED" || (detail.d("deadline") != null && dl == null)
    val sections = buildList {
        detail.d("summary")?.let { add(StageDetailSection("summary", "공고 요약") { StageExpandableText(it) }) }
        val cond = listOf(
            "모집 역할" to detail.role, "장르" to detail.genre, "인원" to detail.headcount.takeIf { it > 0 }?.let { "${it}명" },
            "성별" to detail.gender, "나이" to detail.d("age"), "키" to detail.height, "출연료" to detail.d("fee"),
            "영상" to (if (detail.videoRequired) "영상 제출 필수" else null), "수상 경력" to (if (detail.awardPreferred) "수상 경력 우대" else null),
        ).mapNotNull { (k, v) -> v?.let { k to it } }
        if (cond.isNotEmpty()) add(StageDetailSection("cond", "모집 조건") { StageInfoRows(cond) })
        val how = listOf("마감" to StageDate.short(detail.d("deadline")), "지원 방법" to detail.d("applyMethod"), "문의" to detail.d("contact"))
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
                val rows = listOf("일정" to detail.schedule, "장소" to (detail.place ?: detail.region), "마감" to StageDate.short(detail.d("deadline")))
                    .mapNotNull { (k, v) -> v?.let { k to it } }
                if (rows.isNotEmpty()) Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) { StageInfoRows(rows) }
            }
        },
        bottom = {
            val type = ActiveRole.current(ctx)
            when {
                closed -> StageBottomButton("마감된 공고예요", enabled = false) {}
                detail.isExternal -> {
                    val url = detail.d("applyUrl") ?: detail.d("sourceUrl")
                    StageBottomButton(if (detail.d("applyUrl") != null) "원문에서 지원하기" else "원문 공고 보러 가기", enabled = url != null) { url?.let(onOpenUrl) }
                }
                applied -> StageBottomButton("지원한 공고예요", enabled = false) {}
                type == "DANCER" || type == "TEACHER" -> StageBottomButton("이력서로 지원하기", busy = applying) {
                    if (type == "TEACHER") showSwitch = true else scope.launch { startApply() }
                }
                else -> StageBottomButton("무용수 회원만 지원할 수 있어요", enabled = false) {}
            }
        },
    )

    if (showSwitch) AlertDialog(
        onDismissRequest = { showSwitch = false },
        title = { Text("무용수 유형으로 전환해 주세요", style = Bold) },
        text = { Text("캐스팅 공고 지원은 무용수 유형에서 이용할 수 있어요. 무용수 유형으로 전환할까요?", style = Regular) },
        confirmButton = {
            TextButton({
                showSwitch = false
                scope.launch {
                    if (ActiveRole.saveOnServer(ctx, "DANCER")) { ActiveRole.store(ctx, "DANCER"); startApply() }
                    else toast = "유형을 바꾸지 못했어요. 잠시 후 다시 시도해 주세요."
                }
            }) { Text("전환하기", color = Brand) }
        },
        dismissButton = { TextButton({ showSwitch = false }) { Text("취소", color = MuyeonColors.textSub) } },
    )
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
                    runCatching { api.apply(id, resumeId) }
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

// MARK: 소식 상세
