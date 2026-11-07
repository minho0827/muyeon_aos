package com.muyeon.app.ui.subwizard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.quote.QuoteChatBubble
import com.muyeon.app.ui.quote.QuoteOptionRow
import com.muyeon.app.ui.quote.QuotePrimaryButton
import com.muyeon.app.ui.quote.QuoteProgressBar
import com.muyeon.app.ui.quote.QuoteRegionPicker
import com.muyeon.app.ui.quote.QuoteTypingBubble
import com.muyeon.app.ui.recruit.RecruitLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ★ 2026-11-08 대타공고 설문 등록 — 레슨 견적 위저드와 같은 채팅형(질문 말풍선 → 고르기 → 답 말풍선 · '수정').
 *   마지막에 제목(자동)·학원 이름을 확인하고 등록(POST /subs). 등록되면 [onCreated](목록 새로고침·안내).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubWizardScreen(
    onClose: () -> Unit,
    onCreated: (Long) -> Unit,
    prefillId: String? = null,
    vm: SubWizardViewModel = koinViewModel { org.koin.core.parameter.parametersOf(prefillId) },
) {
    val context = LocalContext.current
    vm.titleTemplate = stringResource(R.string.subwiz_auto_title)
    val targetText = RecruitLabels.TARGETS.associateWith { t -> RecruitLabels.target(t)?.let { stringResource(it) } ?: t }
    vm.targetLabel = { targetText[it] ?: it }
    var typing by remember { mutableStateOf(true) }
    var regionPicker by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    BackHandler { if (!vm.back()) onClose() }
    LaunchedEffect(vm.currentIndex) {
        typing = true; scroll.animateScrollTo(scroll.maxValue); delay(600); typing = false; delay(60); scroll.animateScrollTo(scroll.maxValue)
    }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is SubWizardEvent.Created -> { Toast.makeText(context, R.string.subwiz_done, Toast.LENGTH_SHORT).show(); onCreated(e.id) }
                is SubWizardEvent.Failed -> Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.subwiz_title), style = AppTypography.Title18, color = AppColors.TextHead)
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = AppColors.TextBody, modifier = Modifier.size(18.dp))
                }
            }
            QuoteProgressBar(progress = vm.progress, stepText = "${vm.currentIndex + 1}/${vm.steps.size}")
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (vm.currentIndex == 0) Text(stringResource(R.string.subwiz_intro), style = AppTypography.Body14, color = AppColors.TextSub,
                modifier = Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(12.dp)).padding(16.dp))
            vm.answered.forEach { st ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuoteChatBubble(text = question(st.id), isQuestion = true)
                    QuoteChatBubble(text = display(vm, st).ifBlank { stringResource(R.string.subwiz_skipped) }, isQuestion = false, onEdit = { vm.edit(st.id) })
                }
            }
            val st = vm.current
            AnimatedVisibility(typing, enter = fadeIn(), exit = fadeOut()) { QuoteTypingBubble() }
            AnimatedVisibility(!typing, enter = fadeIn(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    QuoteChatBubble(text = question(st.id), isQuestion = true)
                    when (st.type) {
                        SubStepType.SINGLE, SubStepType.MULTI -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            st.options.forEach { o ->
                                QuoteOptionRow(label = optionLabel(st.id, o), isSelected = o in vm.answer(st.id).values, isMulti = st.type == SubStepType.MULTI) { vm.toggle(o) }
                            }
                        }
                        SubStepType.CHIPS -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            st.options.forEach { o -> Chip(optionLabel(st.id, o), o in vm.answer(st.id).values) { vm.toggle(o) } }
                        }
                        SubStepType.DATE -> DateStep(vm)
                        SubStepType.REGION -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            vm.answer(st.id).values.firstOrNull()?.let { Text(it, style = AppTypography.Section16, color = AppColors.TextHead) }
                            QuotePrimaryButton(
                                text = stringResource(if (vm.answer(st.id).isEmpty) R.string.subwiz_region_pick else R.string.subwiz_region_repick),
                                modifier = Modifier.fillMaxWidth(), filled = false, onClick = { regionPicker = true },
                            )
                        }
                        SubStepType.TEXT -> TextBox(vm.answer(st.id).text.orEmpty(), stringResource(R.string.subwiz_desc_hint), vm::setText)
                        SubStepType.IMAGE -> ImageStep(vm)
                        SubStepType.CONFIRM -> ConfirmStep(vm)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            QuotePrimaryButton(
                text = stringResource(if (vm.isLast) R.string.subwiz_submit else R.string.subwiz_next),
                modifier = Modifier.weight(1f), filled = true, enabled = vm.canProceed, onClick = vm::next,
            )
        }
    }
    if (regionPicker) Box(Modifier.fillMaxSize().background(AppColors.Surface)) {
        QuoteRegionPicker(
            token = TokenManager.getAccessToken(context),
            onSelect = { names, codes ->
                // 여러 곳을 고를 수 있는 피커라 첫 곳만(대타 수업 장소는 한 곳).
                vm.setRegion(names.split(",").first().trim(), codes.split(",").firstOrNull()?.trim()?.ifEmpty { null })
                regionPicker = false
            },
            onClose = { regionPicker = false },
        )
    }
}

@Composable
private fun question(id: String): String = stringResource(
    when (id) {
        SubWizardQuestions.GENRE -> R.string.subwiz_q_genre
        SubWizardQuestions.TARGET -> R.string.subwiz_q_target
        SubWizardQuestions.FIELDS -> R.string.subwiz_q_fields
        SubWizardQuestions.SCHEDULE -> R.string.subwiz_q_schedule
        SubWizardQuestions.DATE -> R.string.subwiz_q_date
        SubWizardQuestions.START -> R.string.subwiz_q_start
        SubWizardQuestions.LENGTH -> R.string.subwiz_q_length
        SubWizardQuestions.COUNT -> R.string.subwiz_q_count
        SubWizardQuestions.REGION -> R.string.subwiz_q_region
        SubWizardQuestions.SALARY -> R.string.subwiz_q_salary
        SubWizardQuestions.CAREER -> R.string.subwiz_q_career
        SubWizardQuestions.DESC -> R.string.subwiz_q_desc
        SubWizardQuestions.IMAGE -> R.string.subwiz_q_image
        else -> R.string.subwiz_q_confirm
    },
)

@Composable
private fun optionLabel(id: String, v: String): String = when (id) {
    SubWizardQuestions.TARGET -> RecruitLabels.target(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.FIELDS -> LessonLabels.field(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.SCHEDULE -> RecruitLabels.schedule(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.LENGTH -> v.toInt().let { m -> if (m % 60 == 0) stringResource(R.string.subwiz_len_h, m / 60) else if (m < 60) stringResource(R.string.subwiz_len_m, m) else stringResource(R.string.subwiz_len_hm, m / 60, m % 60) }
    SubWizardQuestions.COUNT -> stringResource(R.string.recruit_class_count, v.toInt())
    SubWizardQuestions.SALARY -> HomeLabels.salary(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.CAREER -> if (v == "ANY") stringResource(R.string.career_any) else LessonLabels.career(v)?.let { stringResource(it) } ?: v
    else -> v
}

@Composable
private fun display(vm: SubWizardViewModel, st: SubStep): String = when (st.type) {
    SubStepType.DATE -> vm.answer(st.id).values.firstOrNull()?.let { runCatching { LocalDate.parse(it).format(DATE_FMT) }.getOrNull() }.orEmpty()
    SubStepType.REGION -> vm.answer(st.id).values.firstOrNull().orEmpty()
    SubStepType.TEXT -> vm.answer(st.id).text.orEmpty()
    SubStepType.IMAGE -> if (vm.answer(st.id).text.isNullOrBlank()) "" else stringResource(R.string.subwiz_image_added)
    else -> vm.answer(st.id).values.map { optionLabel(st.id, it) }.joinToString(", ")
}

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd(E)", Locale.KOREAN)

/** 날짜 — 오늘부터만 고를 수 있다(지난 날 대타는 없다). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateStep(vm: SubWizardViewModel) {
    val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    val state = rememberDatePickerState(
        initialSelectedDateMillis = vm.answer(SubWizardQuestions.DATE).values.firstOrNull()?.let { runCatching { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull() },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
        },
    )
    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
    }
    DatePicker(state = state, title = null, headline = null, showModeToggle = false)
}

/** 마지막 확인 — 제목(자동, 고칠 수 있음) · 학원/단체 이름 · 수업 시간 요약 · 지난 시각 경고. */
@Composable
private fun ConfirmStep(vm: SubWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.subwiz_f_title), style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
        TextBox(vm.title, stringResource(R.string.subwiz_title_hint)) { vm.title = it }
        Text(stringResource(R.string.subwiz_f_academy), style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
        TextBox(vm.academy, stringResource(R.string.subwiz_academy_hint)) { vm.academy = it }
        vm.classTime()?.let { Text(stringResource(R.string.subwiz_time_summary, it), style = AppTypography.Body14, color = AppColors.TextSub) }
        if (vm.timeError() == true) Text(stringResource(R.string.subwiz_past), style = AppTypography.Body14, color = AppColors.Danger)
    }
}

@Composable
private fun TextBox(value: String, hint: String, onChange: (String) -> Unit) {
    var v by remember(hint) { mutableStateOf(value) }
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).border(1.dp, AppColors.ChipBorder, RoundedCornerShape(12.dp)).padding(14.dp)) {
        if (v.isEmpty()) Text(hint, style = AppTypography.Body15, color = AppColors.TextHint)
        BasicTextField(v, { v = it; onChange(it) }, textStyle = TextStyle(fontSize = AppTypography.Body15.fontSize, color = AppColors.TextHead), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text, style = AppTypography.Chip14, color = if (on) AppColors.TextOnPrimary else AppColors.ChipText,
        modifier = Modifier.background(if (on) AppColors.ChipSelectedBg else AppColors.Surface, RoundedCornerShape(50))
            .border(1.dp, if (on) AppColors.ChipSelectedBg else AppColors.ChipBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** 대표 사진 1장 — 사진 고르기 → 올리기(JobPostingApi.uploadImage, 웹 SubCreate 와 같은 업로드) → 미리보기. */
@Composable
private fun ImageStep(vm: SubWizardViewModel) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val url = vm.answer(SubWizardQuestions.IMAGE).text
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        vm.uploading = true
        scope.launch {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            val r = bytes?.let { com.muyeon.app.ui.jobposting.JobPostingApi(TokenManager.getAccessToken(context)).uploadImage(it) }
            r?.onSuccess { vm.setImage(it) }?.onFailure { Toast.makeText(context, it.message ?: context.getString(R.string.subwiz_image_failed), Toast.LENGTH_SHORT).show() }
            vm.uploading = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        url?.takeIf { it.isNotBlank() }?.let {
            com.muyeon.app.ui.components.RemoteImage(it, Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(12.dp)))
        }
        QuotePrimaryButton(
            text = stringResource(when { vm.uploading -> R.string.subwiz_image_uploading; url.isNullOrBlank() -> R.string.subwiz_image_pick; else -> R.string.subwiz_image_repick }),
            modifier = Modifier.fillMaxWidth(), filled = false, enabled = !vm.uploading,
            onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}
