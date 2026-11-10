package com.muyeon.app.ui.quote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import kotlinx.coroutines.delay

/**
 * 견적 문진(채팅형 위저드) — iOS `QuoteWizardView.swift` 1:1 이식.
 *
 * 레이아웃 수치(iOS 동일):
 *  - 헤더: VStack spacing 12 / padding h20, top 8, bottom 12 / 제목 18sp bold 101116 / X 아이콘 18
 *  - 본문: VStack spacing 16 / padding h20, top 16, bottom 24
 *  - 안내카드: 14sp medium, 배경 F4F4F4, radius 12, padding 16 (첫 질문에서만)
 *  - 하단바: padding h20 v12, 버튼 높이 48 radius 12
 *  - 타이핑 시퀀스: 표시 0.9s → 페이드 0.3s → 질문 노출
 */
@Composable
fun QuoteWizardScreen(
    vm: QuoteWizardViewModel,
    token: String?,
    onClose: () -> Unit,
    onComplete: (Map<String, QuoteAnswer>) -> Unit,
    isDirect: Boolean = false,   // 지정 견적 — '평균 4개 이상' 대신 전달 문구(iOS QuoteWizardView.isDirect)
    /** ★ 2026-11-08 마지막 질문의 [보내기] 위 한 줄(원장·공연팀 '이번 달 레슨 요청 N회 남았어요'). null 이면 없음. */
    submitNote: String? = null,
) {
    var showTyping by remember { mutableStateOf(true) }
    var showRegionPicker by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    // 질문이 바뀔 때마다 '입력 중…' 0.9초 후 질문 노출 (iOS runTyping).
    LaunchedEffect(vm.currentIndex, vm.loading) {
        if (vm.loading) { showTyping = true; return@LaunchedEffect }
        showTyping = true
        scroll.animateScrollTo(scroll.maxValue)
        delay(900)
        showTyping = false
        delay(60)
        scroll.animateScrollTo(scroll.maxValue)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(QuoteColors.white)
    ) {
        Header(
            title = vm.title,
            progress = vm.progress,
            stepText = "${vm.currentIndex + 1}/${maxOf(vm.questions.size, 1)}",
            onClose = onClose,
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (vm.currentIndex == 0 && !vm.loading) InfoCard(isDirect)

            // 채팅 로그(이미 답변한 질문들)
            vm.answeredQuestions.forEach { q ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuoteChatBubble(text = q.title, isQuestion = true)
                    QuoteChatBubble(
                        text = vm.answer(q).displayText(q),
                        isQuestion = false,
                        onEdit = { vm.editStep(q.id) },
                    )
                }
            }

            // 현재 질문 — ★ 2026-11-10 설문을 받는 중이면 '입력 중…' 말풍선만.
            if (vm.loading || vm.currentQuestionOrNull == null) {
                QuoteTypingBubble()
            } else {
                CurrentQuestion(
                    vm = vm,
                    showTyping = showTyping,
                    onOpenRegionPicker = { showRegionPicker = true },
                )
            }
        }

        // ★ 2026-11-10 마지막 단계 [무료견적 받기] 위 구석 작은 체크 한 줄 '☑ 채용 공고에도 올립니다'(서버 title) — 기본 체크, 로그인 회원만.
        // 0단계뿐이거나 세트를 받기 전에는 보이지 않는다(마지막 질문에서 한 번만).
        if (vm.isLastQuestion && !vm.loading && vm.questions.size > 1 && !token.isNullOrEmpty()) {
            vm.crossPost.forEach { spec ->
                CrossPostCheckLine(
                    text = spec.title.ifEmpty { "채용 공고에도 올립니다" },
                    checked = vm.isCrossPostOn(spec.kind),
                    onToggle = { vm.setCrossPost(spec.kind, it) },
                )
            }
        }
        if (vm.isLastQuestion && !submitNote.isNullOrEmpty()) {
            Text(
                text = submitNote,
                fontFamily = customFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = QuoteColors.c6D6E71,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(QuoteColors.white).padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )
        }
        BottomBar(
            text = if (vm.isLastQuestion) "무료견적 받기" else "다음",
            enabled = vm.canProceed,
            onClick = { vm.goNextOrComplete { onComplete(vm.submitAnswers()) } },
        )
    }

    // 지역 선택 풀스크린 — iOS fullScreenCover(QuoteRegionPickerView) 대응.
    if (showRegionPicker) {
        Box(Modifier.fillMaxSize().background(QuoteColors.white)) {
            QuoteRegionPicker(
                token = token,
                onSelect = { names, codes ->
                    vm.setRegion(names, codes.ifEmpty { null })
                    showRegionPicker = false
                },
                onClose = { showRegionPicker = false },
            )
        }
    }
}

@Composable
private fun Header(title: String, progress: Float, stepText: String, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontFamily = customFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = QuoteColors.c101116,
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "닫기", tint = QuoteColors.c37383B, modifier = Modifier.size(18.dp))
            }
        }
        QuoteProgressBar(progress = progress, stepText = stepText)
    }
}

/** 첫 질문 안내 카드 — 여러 명: "…평균 4개 이상의 견적을…", 지정 견적: "…선택한 강사님께 바로 견적 요청이 전달돼요." */
@Composable
private fun InfoCard(isDirect: Boolean) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = QuoteColors.c6D6E71)) { append("몇 가지 정보만 알려 주시면 ") }
        withStyle(SpanStyle(color = QuoteColors.f58232)) { append(if (isDirect) "선택한 강사님께 바로" else "평균 4개 이상") }
        withStyle(SpanStyle(color = QuoteColors.c6D6E71)) { append(if (isDirect) " 견적 요청이 전달돼요." else "의 견적을 받을 수 있어요.") }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QuoteColors.cF4F4F4)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        // 지정 견적은 안내 화면이 없어 꼭 알아야 할 안내만 붙인다(웹·iOS 와 같은 문구).
        if (isDirect) {
            Text(
                "요청 내용은 이 강사님에게만 보여요. 원하지 않으면 언제든 취소할 수 있어요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                color = QuoteColors.c6D6E71,
            )
        }
    }
}

@Composable
private fun CurrentQuestion(
    vm: QuoteWizardViewModel,
    showTyping: Boolean,
    onOpenRegionPicker: () -> Unit,
) {
    val q = vm.currentQuestion
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(visible = showTyping, enter = fadeIn(), exit = fadeOut()) {
            QuoteTypingBubble()
        }
        AnimatedVisibility(visible = !showTyping, enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                QuoteChatBubble(text = q.title, isQuestion = true)
                // ★ 2026-11-10 서버 설문 부제(0단계 '알맞은 질문으로 안내해 드릴게요.' 등).
                q.subtitle?.let {
                    Text(
                        it, fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                        color = QuoteColors.c6D6E71, modifier = Modifier.padding(start = 4.dp),
                    )
                }
                when (q.type) {
                    QuoteAnswerType.SINGLE, QuoteAnswerType.MULTI ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { // iOS VStack spacing 10
                            q.options.forEach { option ->
                                QuoteOptionRow(
                                    label = option.label,
                                    description = option.description,
                                    isSelected = vm.isSelected(option),
                                    isMulti = q.type == QuoteAnswerType.MULTI,
                                    onTap = { vm.toggleOption(option) },
                                )
                            }
                        }
                    QuoteAnswerType.REGION -> RegionSelector(vm = vm, q = q, onOpenPicker = onOpenRegionPicker)
                    QuoteAnswerType.DATE -> DateSelector(vm = vm, q = q)
                    QuoteAnswerType.TEXT -> FreeTextInput(vm = vm, q = q)
                }
            }
        }
    }
}

/**
 * 자유 입력 — iOS textInput(padding 16, minHeight 100, radius 12, 테두리 EAEAEA 1).
 *  지역/날짜는 전용 피커가 아직 미이식이라 같은 입력칸으로 받는다(값은 서버 스펙대로 저장).
 */
@Composable
private fun FreeTextInput(vm: QuoteWizardViewModel, q: QuoteQuestion) {
    val current = vm.answer(q)
    val initial = when (q.type) {
        QuoteAnswerType.DATE -> current.dateText ?: ""
        else -> current.text ?: ""
    }
    var value by remember(q.id) { mutableStateOf(initial) }
    val hint = q.placeholder ?: when (q.type) {
        QuoteAnswerType.DATE -> "예: 2026.08.20"
        else -> "내용을 입력해 주세요."
    }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, QuoteColors.cEAEAEA, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = hint,
                fontFamily = customFontFamily,
                fontSize = 15.sp,
                color = QuoteColors.c8E8E8E,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                when (q.type) {
                    QuoteAnswerType.DATE -> vm.setDateText(it)
                    else -> vm.setText(it)
                }
            },
            textStyle = TextStyle(
                fontFamily = customFontFamily,
                fontSize = 15.sp,
                color = QuoteColors.c101116,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 날짜 질문 — iOS `DatePicker(.graphical)` 대응(인라인 캘린더).
 *  선택 즉시 저장하며 표시 포맷은 iOS 와 동일한 `yyyy.MM.dd(E)`(ko_KR).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(vm: QuoteWizardViewModel, q: QuoteQuestion) {
    val state = androidx.compose.material3.rememberDatePickerState()
    val millis = state.selectedDateMillis
    LaunchedEffect(millis) {
        millis?.let {
            val fmt = java.text.SimpleDateFormat("yyyy.MM.dd(E)", java.util.Locale.KOREAN)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC") // DatePicker 는 UTC 자정 기준
            // ★ 2026-11-10 서버로는 yyyy-MM-dd(서버 설문 whenDate → when.date 로 합친다).
            val iso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            iso.timeZone = java.util.TimeZone.getTimeZone("UTC")
            vm.setDateText(fmt.format(java.util.Date(it)), iso.format(java.util.Date(it)))
        }
    }
    // ★ 2026-11-10 인라인 캘린더는 최소 폭(약 360dp)이 위저드 좌우 여백보다 넓어 작은 폰에서 토요일 칸이 잘렸다
    //  (실기기 S20 FE). 지역 질문처럼 '선택값 + 버튼 → 날짜 창'으로 바꾼다 — 창은 화면 폭을 다 쓴다.
    var open by remember { mutableStateOf(false) }
    val shown = millis?.let {
        val f = java.text.SimpleDateFormat("yyyy.MM.dd(E)", java.util.Locale.KOREAN)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        f.format(java.util.Date(it))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (shown != null) {
            Text(text = shown, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        androidx.compose.material3.OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(text = if (shown == null) "날짜 선택하기" else "날짜 다시 선택", fontFamily = customFontFamily, fontSize = 16.sp, color = Color(0xFF111111))
        }
    }
    if (open) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { open = false }) { Text("확인", fontFamily = customFontFamily) }
            },
        ) {
            androidx.compose.material3.DatePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
            )
        }
    }
}

/**
 * 지역 질문 — iOS regionSelector: 선택값(16sp semiBold)을 위에 보여주고
 *  아래 primaryLine 버튼("지역 선택하기"/"지역 다시 선택")으로 피커 진입. VStack spacing 8.
 */
@Composable
private fun RegionSelector(vm: QuoteWizardViewModel, q: QuoteQuestion, onOpenPicker: () -> Unit) {
    val region = vm.answer(q).region
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!region.isNullOrEmpty()) {
            Text(
                text = region,
                fontFamily = customFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = QuoteColors.c101116,
            )
        }
        QuotePrimaryButton(
            text = if (!region.isNullOrEmpty()) "지역 다시 선택" else "지역 선택하기",
            modifier = Modifier.fillMaxWidth(),
            filled = false,
            onClick = onOpenPicker,
        )
    }
}

@Composable
private fun BottomBar(text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(QuoteColors.white)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        QuotePrimaryButton(
            text = text,
            modifier = Modifier.weight(1f),
            filled = true,
            enabled = enabled,
            onClick = onClick,
        )
    }
}

/** ★ 2026-11-10 같이 올리기 체크 한 줄 — 작은 글씨, 오른쪽 구석(위저드 선택지 체크박스 색). */
@Composable
private fun CrossPostCheckLine(text: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(QuoteColors.white).padding(start = 20.dp, end = 20.dp, top = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .toggleable(value = checked, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = onToggle)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (checked) QuoteColors.f58232 else QuoteColors.white)
                    .border(1.dp, if (checked) QuoteColors.f58232 else QuoteColors.cC5C4C4, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) Icon(Icons.Filled.Check, null, tint = QuoteColors.white, modifier = Modifier.size(12.dp))
            }
            Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = QuoteColors.c6D6E71)
        }
    }
}
