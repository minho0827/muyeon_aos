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
        QuoteNavBar(title = d?.template?.title ?: "설문지", onBack = onClose)

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
                    levels.sortedBy { it.levelOrder }.forEach { g ->
                        Text(
                            g.level,
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.primary,
                        )
                        g.items.sortedBy { it.sortOrder }.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                item.imageUrl?.let {
                                    AsyncImage(
                                        QuoteUi.imageUrl(it), null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                                    )
                                }
                                Text(
                                    item.text,
                                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                                    color = MuyeonColors.body,
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

        fun start(context: Context, dispatchId: Int, canRespond: Boolean) {
            val i = Intent(context, SurveyActivity::class.java)
                .putExtra(EXTRA_DISPATCH, dispatchId)
                .putExtra(EXTRA_CAN_RESPOND, canRespond)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dispatchId = intent.getIntExtra(EXTRA_DISPATCH, 0)
        val canRespond = intent.getBooleanExtra(EXTRA_CAN_RESPOND, true)

        setContent {
            val nav = rememberNavController()
            val api = remember { SurveyApi(TokenManager.getAccessToken(this)) }
            NavHost(nav, startDestination = "respond") {
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
