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
import com.muyeon.app.ui.quote.QuotePrimaryButton
import com.muyeon.app.ui.quote.QuoteRegionPicker
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

import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.lesson.LessonMultiDateCalendar
import com.muyeon.app.ui.subwizard.components.SubChipGroup
import com.muyeon.app.ui.subwizard.components.SubFormSection
import com.muyeon.app.ui.subwizard.components.SubTimeField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

/**
 * 대타공고 등록 — ★ 2026-11-08 사용자 요청으로 설문형에서 **한 화면 입력 양식**으로 바꿨다.
 *   날짜는 기존 공용 달력(LessonMultiDateCalendar — 레슨 개설과 같은 달력, 지난 날 막음), 시간은 시·분 휠(10분 단위).
 *   맨 아래 [대타공고 올리기] — 빠진 칸이 있으면 그 칸 이름을 알려 준다. 등록되면 [onCreated](목록 새로고침·안내).
 */
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
    var regionPicker by remember { mutableStateOf(false) }
    val required = com.muyeon.app.ui.common.rememberRequiredForm()
    BackHandler(onBack = onClose)
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is SubWizardEvent.Created -> { Toast.makeText(context, R.string.subwiz_done, Toast.LENGTH_SHORT).show(); onCreated(e.id) }
                is SubWizardEvent.Failed -> Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(stringResource(R.string.subwiz_title), primary = false, onBack = onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(stringResource(R.string.subwiz_form_intro), style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.padding(top = 12.dp))
            vm.fields.forEach { f -> FieldSection(vm, f, required) { regionPicker = true } }
            // 제목(자동 — 고칠 수 있음) · 학원 이름
            SubFormSection(stringResource(R.string.subwiz_f_title), required = true, check = required to "title", filled = vm.title.isNotBlank()) {
                TextBox(vm.title, stringResource(R.string.subwiz_title_hint), onChange = vm::editTitle)
            }
            SubFormSection(stringResource(R.string.subwiz_f_academy), required = false) {
                TextBox(vm.academy, stringResource(R.string.subwiz_academy_hint)) { vm.academy = it }
            }
            if (vm.timeError() == true) Text(stringResource(R.string.subwiz_past), style = AppTypography.Body14, color = AppColors.Danger, modifier = Modifier.padding(top = 16.dp))
        }
        Column(Modifier.fillMaxWidth().background(AppColors.Surface)) {
            HorizontalDivider(color = AppColors.Divider)
            QuotePrimaryButton(
                text = stringResource(R.string.subwiz_submit),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), filled = true,
                enabled = !vm.submitting && !vm.uploading,
                onClick = {
                    // 공통 필수값 확인 — 빠진 칸으로 스크롤·강조·토스트(RequiredForm).
                    when {
                        !required.validate(context) -> Unit
                        vm.timeError() == true -> Toast.makeText(context, R.string.subwiz_past, Toast.LENGTH_SHORT).show()
                        else -> vm.submit()
                    }
                },
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

/** 항목 하나 — 종류에 맞는 입력(칩·달력·시간·지역·글·사진). */
@Composable
private fun FieldSection(vm: SubWizardViewModel, f: SubField, required: com.muyeon.app.ui.common.RequiredForm, onPickRegion: () -> Unit) {
    val filled = when (f.type) {
        SubFieldType.TIME -> vm.classTime() != null
        SubFieldType.TEXT, SubFieldType.IMAGE -> true
        else -> !vm.answer(f.id).isEmpty
    }
    SubFormSection(sectionLabel(f.id), required = f.required, check = required to f.id, filled = filled) {
        when (f.type) {
            SubFieldType.CHIPS, SubFieldType.SINGLE, SubFieldType.MULTI ->
                SubChipGroup(f.options, vm.answer(f.id).values, { optionLabel(f.id, it) }) { vm.toggle(f, it) }
            SubFieldType.DATE -> Box(Modifier.fillMaxWidth().border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp)).padding(8.dp)) {
                // 기존 레슨 달력(오늘~6개월, 지난 날 막음) — 한 날만 고른다.
                LessonMultiDateCalendar(selected = vm.answer(f.id).values) { vm.setDate(it) }
            }
            SubFieldType.TIME -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubTimeField(vm.start, vm.end, vm::setStart, vm::setEnd)
                if (vm.timeOrderError()) Text(stringResource(R.string.subwiz_time_order), style = AppTypography.Caption12, color = AppColors.Danger)
            }
            SubFieldType.REGION -> Row(
                Modifier.fillMaxWidth().border(1.dp, if (vm.answer(f.id).isEmpty) AppColors.ChipBorder else AppColors.TextHead, RoundedCornerShape(12.dp))
                    .clickable(onClick = onPickRegion).padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(vm.answer(f.id).values.firstOrNull() ?: stringResource(R.string.subwiz_region_pick),
                    style = AppTypography.Body15, color = if (vm.answer(f.id).isEmpty) AppColors.TextHint else AppColors.TextHead, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.TextHint, modifier = Modifier.size(20.dp))
            }
            SubFieldType.TEXT -> TextBox(vm.answer(f.id).text.orEmpty(), stringResource(R.string.subwiz_desc_hint), minHeight = 96, onChange = vm::setText)
            SubFieldType.IMAGE -> ImageStep(vm)
        }
    }
}

@Composable
private fun sectionLabel(id: String): String = stringResource(
    when (id) {
        SubWizardQuestions.GENRE -> R.string.subwiz_s_genre
        SubWizardQuestions.TARGET -> R.string.subwiz_s_target
        SubWizardQuestions.FIELDS -> R.string.subwiz_s_fields
        SubWizardQuestions.SCHEDULE -> R.string.subwiz_s_schedule
        SubWizardQuestions.DATE -> R.string.subwiz_s_date
        SubWizardQuestions.TIME -> R.string.subwiz_s_time
        SubWizardQuestions.COUNT -> R.string.subwiz_s_count
        SubWizardQuestions.REGION -> R.string.subwiz_s_region
        SubWizardQuestions.SALARY -> R.string.subwiz_s_salary
        SubWizardQuestions.CAREER -> R.string.subwiz_s_career
        SubWizardQuestions.DESC -> R.string.subwiz_s_desc
        else -> R.string.subwiz_s_image
    },
)

@Composable
private fun optionLabel(id: String, v: String): String = when (id) {
    SubWizardQuestions.TARGET -> RecruitLabels.target(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.FIELDS -> LessonLabels.field(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.SCHEDULE -> RecruitLabels.schedule(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.COUNT -> stringResource(R.string.recruit_class_count, v.toInt())
    SubWizardQuestions.SALARY -> HomeLabels.salary(v)?.let { stringResource(it) } ?: v
    SubWizardQuestions.CAREER -> if (v == "ANY") stringResource(R.string.career_any) else LessonLabels.career(v)?.let { stringResource(it) } ?: v
    else -> v
}

@Composable
private fun TextBox(value: String, hint: String, minHeight: Int = 52, onChange: (String) -> Unit) {
    var v by remember(hint) { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != v) v = value }
    Box(Modifier.fillMaxWidth().heightIn(min = minHeight.dp).border(1.dp, AppColors.ChipBorder, RoundedCornerShape(12.dp)).padding(14.dp)) {
        if (v.isEmpty()) Text(hint, style = AppTypography.Body15, color = AppColors.TextHint)
        BasicTextField(v, { v = it; onChange(it) }, textStyle = TextStyle(fontSize = AppTypography.Body15.fontSize, color = AppColors.TextHead), modifier = Modifier.fillMaxWidth())
    }
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
