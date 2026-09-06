package com.muyeon.app.ui.survey

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * 설문 응답/열람 — iOS `Survey/SurveyResponseView.swift` 이식.
 *  문항 타입 3종(SINGLE/MULTI/TEXT) + 기타 입력 + 레벨별 항목.
 *  이미 응답한 설문은 읽기 전용으로 열린다(canRespond=false).
 */
@Composable
fun SurveyResponseScreen(
    api: SurveyApi,
    dispatchId: Int,
    canRespond: Boolean,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    var detail by remember { mutableStateOf<SurveyDispatchDetail?>(null) }
    var answers by remember { mutableStateOf<Map<String, SurveyAnswer>>(emptyMap()) }
    var note by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(dispatchId) {
        api.getDispatch(dispatchId).onSuccess { d ->
            detail = d
            // 기존 응답이 있으면 프리필(수정 회차 = revision 증가)
            d.response?.let { r -> answers = r.answers; note = r.note.orEmpty() }
        }
        loading = false
    }

    val d = detail
    val readOnly = !canRespond

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        val pdfCtx = LocalContext.current
        QuoteNavBar(
            title = d?.template?.title ?: "설문지", onBack = onClose,
            trailing = {
                // 강사 열람 모드에서만 — 응답을 PDF 로 내보내 저장·인쇄·전달(iOS 와 동일).
                if (!canRespond && d != null) {
                    Box(
                        Modifier.size(44.dp).clickable {
                            SurveyPdf.make(
                                pdfCtx, d.template.title, d.template.description,
                                surveyPdfInfo(d), surveyPdfItems(d, answers),
                            )?.let { SurveyPdf.share(pdfCtx, it) }
                                ?: run { toast = "PDF 를 만들지 못했어요." }
                        },
                        Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.IosShare, "PDF 로 내보내기",
                            tint = MuyeonColors.primary, modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
        )

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }
        if (d == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text("설문을 불러오지 못했어요.", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // 헤더 — 회원명 + 연결된 레슨
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                d.template.description?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textSub)
                }
                val meta = listOfNotNull(
                    d.recipientName,
                    d.lesson?.startAt?.let { QuoteUi.relativeTime(it) },
                    d.lesson?.place,
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.secondary)
                }
                d.response?.revision?.takeIf { it > 1 }?.let {
                    Text(
                        "${it}회차 수정본",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        lineHeight = 15.sp, color = MuyeonColors.primary,
                    )
                }
            }

            d.questions.sortedBy { it.sortOrder }.forEach { q ->
                QuestionBlock(
                    q = q,
                    answer = answers[q.id.toString()] ?: SurveyAnswer(),
                    readOnly = readOnly,
                ) { updated -> answers = answers + (q.id.toString() to updated) }
            }

            // 레벨별 항목(있는 장르만)
            d.levels?.takeIf { it.isNotEmpty() }?.let { levels ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "가능한 동작",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textHead,
                    )
                    // 응답이 고른 단계·항목(iOS 예약 키 _level/_items). 열람 시 강조해서 보여준다.
                    val answeredLevel = answers["_level"]?.text
                    val answeredItems = answers["_items"]?.optionIds.orEmpty()
                    levels.sortedBy { it.levelOrder }
                        // 응답한 단계가 있으면 그 단계만 — 전 단계를 나열하면 뭘 골랐는지 안 보인다.
                        .filter { answeredLevel == null || it.level == answeredLevel }
                        .forEach { g ->
                            Text(
                                g.level,
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.primary,
                            )
                            g.items.sortedBy { it.sortOrder }.forEach { item ->
                                val on = answeredItems.contains(item.id)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item.imageUrl?.let {
                                        AsyncImage(
                                            QuoteUi.imageUrl(it), null, contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                                        )
                                    }
                                    if (on) {
                                        Icon(
                                            Icons.Filled.Check, null, tint = MuyeonColors.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Text(
                                        item.text,
                                        fontFamily = customFontFamily,
                                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.sp, lineHeight = 18.sp,
                                        color = if (on) MuyeonColors.textHead else MuyeonColors.body,
                                    )
                                }
                            }
                        }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "추가로 남길 말",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead,
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it }, enabled = !readOnly,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!readOnly) {
            // 필수 문항 미응답이면 비활성
            val missing = d.questions.any { q ->
                q.required && (answers[q.id.toString()]?.let { it.optionIds.isEmpty() && it.text.isNullOrBlank() } ?: true)
            }
            Text(
                if (saving) "제출 중…" else if (missing) "필수 문항을 완료해주세요" else "제출하기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                    .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (!missing && !saving) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = !missing && !saving) {
                        saving = true
                        scope.launch {
                            api.respond(dispatchId, answers, note.trim().ifEmpty { null })
                                .onSuccess { onDone() }.onFailure { toast = it.message }
                            saving = false
                        }
                    }
                    .padding(vertical = 16.dp),
            )
        }
    }

    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

@Composable
private fun QuestionBlock(
    q: SurveyQuestion,
    answer: SurveyAnswer,
    readOnly: Boolean,
    onChange: (SurveyAnswer) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                q.title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            if (q.required) Text("*", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MuyeonColors.primary)
        }
        q.description?.takeIf { it.isNotEmpty() }?.let {
            Text(it, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub)
        }

        when (q.type) {
            "TEXT" -> OutlinedTextField(
                value = answer.text.orEmpty(),
                onValueChange = { onChange(answer.copy(text = it)) },
                enabled = !readOnly,
                placeholder = { q.placeholder?.let { Text(it, fontFamily = customFontFamily, fontSize = 14.sp) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            )
            else -> {
                val multi = q.type == "MULTI"
                q.options.forEach { opt ->
                    val on = answer.optionIds.contains(opt.id)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (on) MuyeonColors.primary else MuyeonColors.border, RoundedCornerShape(10.dp))
                            .clickable(enabled = !readOnly) {
                                val next = when {
                                    multi && on -> answer.optionIds - opt.id
                                    multi -> answer.optionIds + opt.id
                                    on -> emptyList()
                                    else -> listOf(opt.id)   // 단일선택은 교체
                                }
                                onChange(answer.copy(optionIds = next))
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        opt.imageUrl?.let {
                            AsyncImage(
                                QuoteUi.imageUrl(it), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                            )
                        }
                        Text(
                            opt.text,
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                            color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                        if (on) {
                            Box(
                                Modifier.size(20.dp).clip(CircleShape).background(MuyeonColors.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                if (q.allowEtc) {
                    OutlinedTextField(
                        value = answer.etc.orEmpty(),
                        onValueChange = { onChange(answer.copy(etc = it)) },
                        enabled = !readOnly, singleLine = true,
                        placeholder = { Text("기타 (직접 입력)", fontFamily = customFontFamily, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** 설문 화면 진입점 — 채팅 SURVEY_CARD 탭 등에서 사용. */
class SurveyActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_DISPATCH = "dispatchId"
        private const val EXTRA_CAN_RESPOND = "canRespond"
        private const val EXTRA_RECIPIENT = "recipientId"

        fun start(context: Context, dispatchId: Int, canRespond: Boolean) {
            val i = Intent(context, SurveyActivity::class.java)
                .putExtra(EXTRA_DISPATCH, dispatchId)
                .putExtra(EXTRA_CAN_RESPOND, canRespond)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }

        /** 회원의 '레슨 전 설문 프로필'(응답 목록) — 강사가 레슨 상세에서 연다. */
        fun startProfile(context: Context, recipientId: Int) {
            val i = Intent(context, SurveyActivity::class.java).putExtra(EXTRA_RECIPIENT, recipientId)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dispatchId = intent.getIntExtra(EXTRA_DISPATCH, 0)
        val canRespond = intent.getBooleanExtra(EXTRA_CAN_RESPOND, true)
        val recipientId = intent.getIntExtra(EXTRA_RECIPIENT, 0)

        setContent {
            val nav = rememberNavController()
            val api = remember { SurveyApi(TokenManager.getAccessToken(this)) }
            NavHost(nav, startDestination = if (recipientId > 0) "profile" else "respond") {
                composable("profile") {
                    SurveyProfileScreen(
                        api, recipientId,
                        onClose = { finish() },
                        // 프로필에서 연 응답은 언제나 읽기 전용(강사 열람).
                        onOpen = { id -> nav.navigate("respond/$id") },
                    )
                }
                composable("respond/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
                    SurveyResponseScreen(
                        api, id, canRespond = false,
                        onClose = { nav.popBackStack() }, onDone = { nav.popBackStack() },
                    )
                }
                composable("respond") {
                    SurveyResponseScreen(
                        api, dispatchId, canRespond,
                        onClose = { finish() }, onDone = { finish() },
                    )
                }
            }
        }
    }
}

/**
 * 제목 아래 정보 라인 — 회원 / 수업 일시 / 장소 / 응답 일시. iOS exportPDF() 의 info.
 */
private fun surveyPdfInfo(d: SurveyDispatchDetail): List<String> = buildList {
    d.recipientName?.takeIf { it.isNotEmpty() }?.let { add("회원: $it") }
    d.lesson?.startAt?.let { add("수업 일시: ${QuoteUi.relativeTime(it)}") }
    d.lesson?.place?.takeIf { it.isNotEmpty() }?.let { add("장소: $it") }
    d.response?.submittedAt?.let { add("응답 일시: ${QuoteUi.relativeTime(it)}") }
}

/**
 * 문항+응답을 PDF 줄로 — iOS exportPDF() 의 items.
 *  미응답은 "응답 없음", 선택형은 콤마 결합에 기타를 덧붙인다.
 */
private fun surveyPdfItems(
    d: SurveyDispatchDetail,
    answers: Map<String, SurveyAnswer>,
): List<SurveyPdfItem> = buildList {
    d.questions.sortedBy { it.sortOrder }.forEach { q ->
        val a = answers[q.id.toString()]
        val text = if (q.type == "TEXT") {
            a?.text?.trim().orEmpty()
        } else {
            val picked = q.options.filter { a?.optionIds?.contains(it.id) == true }.map { it.text }
            val etc = a?.etc?.takeIf { it.isNotEmpty() }?.let { "기타: $it" }
            (picked + listOfNotNull(etc)).joinToString(", ")
        }
        add(SurveyPdfItem(q.title, text.ifEmpty { "응답 없음" }))
    }
    // 실력 단계 — 응답은 예약 키 _level(단계명) / _items(고른 항목 id)에 담긴다(iOS 와 같은 규약).
    answers["_level"]?.text?.takeIf { it.isNotEmpty() }?.let { lvl ->
        val picked = answers["_items"]?.optionIds.orEmpty()
        val names = d.levels?.firstOrNull { it.level == lvl }
            ?.items?.filter { picked.contains(it.id) }?.map { it.text }.orEmpty()
        add(SurveyPdfItem("실력 단계: $lvl", names.joinToString(", ").ifEmpty { "선택 항목 없음" }))
    }
}

/**
 * 레슨 전 설문 프로필 — iOS `SurveyProfileView.swift` 1:1.
 *  강사가 회원에게 보낸 설문 중 **응답된 것들**의 목록. 탭하면 읽기 전용 상세.
 *
 * ⚠️ 종전 AOS 는 이 화면이 없어 `SurveyApi.responses()` 를 부르는 곳이 하나도 없었다.
 *   강사는 회원이 낸 설문을 채팅방 카드로 하나씩 거슬러 올라가야만 볼 수 있었다.
 */
@Composable
fun SurveyProfileScreen(
    api: SurveyApi,
    recipientId: Int,
    onClose: () -> Unit,
    onOpen: (Int) -> Unit,
) {
    var items by remember { mutableStateOf<List<SurveyDispatchDetail>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(recipientId) {
        // 실패해도 기존 목록은 유지 — iOS 와 동일(빈 화면으로 되돌아가지 않는다).
        api.responses(recipientId).onSuccess { items = it }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(title = "레슨 전 설문 프로필", onBack = onClose)

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    "아직 응답한 설문이 없어요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            }
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items.forEach { it_ -> SurveyProfileRow(it_) { onOpen(it_.dispatch.id) } }
            }
        }
    }
}

@Composable
private fun SurveyProfileRow(d: SurveyDispatchDetail, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MuyeonColors.surface)
            .clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(SURVEY_DONE.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, tint = SURVEY_DONE, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            d.template.genre?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    lineHeight = 14.sp, color = MuyeonColors.primary,
                )
            }
            Text(
                d.template.title,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "응답 완료",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    lineHeight = 14.sp, color = SURVEY_DONE,
                )
                Text(
                    "· ${d.response?.answers?.size ?: 0}개 문항",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                    color = MuyeonColors.textSub,
                )
            }
            // 언제 응답 / 몇 번째 수정 — 제목이 같아도 구분되게(iOS 와 같은 이유).
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                d.response?.submittedAt?.let {
                    Text(
                        "응답 ${QuoteUi.relativeTime(it)}",
                        fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                        color = MuyeonColors.textSub,
                    )
                }
                d.response?.revision?.takeIf { it > 1 }?.let { rev ->
                    val when_ = d.response.updatedAt?.let { " · ${QuoteUi.relativeTime(it)}" }.orEmpty()
                    Text(
                        "· ${rev}번째 수정$when_",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                        lineHeight = 14.sp, color = MuyeonColors.orange,
                    )
                }
            }
        }
        Text("›", fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron)
    }
}

/** 응답 완료 초록 — iOS Color.green. */
private val SURVEY_DONE = Color(0xFF34C759)
