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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
) {
    var showTyping by remember { mutableStateOf(true) }
    var showRegionPicker by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    // 질문이 바뀔 때마다 '입력 중…' 0.9초 후 질문 노출 (iOS runTyping).
    LaunchedEffect(vm.currentIndex) {
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
            stepText = "${vm.currentIndex + 1}/${vm.questions.size}",
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
            if (vm.currentIndex == 0) InfoCard()

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

            // 현재 질문
            CurrentQuestion(
                vm = vm,
                showTyping = showTyping,
                onOpenRegionPicker = { showRegionPicker = true },
            )
        }

        BottomBar(
            text = if (vm.isLastQuestion) "무료견적 받기" else "다음",
            enabled = vm.canProceed,
            onClick = { vm.goNextOrComplete { onComplete(vm.answers.toMap()) } },
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

/** 첫 질문 안내 카드 — "몇 가지 정보만 알려주시면 평균 4개 이상의 견적을 받을 수 있어요." */
@Composable
private fun InfoCard() {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = QuoteColors.c6D6E71)) { append("몇 가지 정보만 알려주시면 ") }
        withStyle(SpanStyle(color = QuoteColors.f58232)) { append("평균 4개 이상") }
        withStyle(SpanStyle(color = QuoteColors.c6D6E71)) { append("의 견적을 받을 수 있어요.") }
    }
    Text(
        text = text,
        fontFamily = customFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QuoteColors.cF4F4F4)
            .padding(16.dp),
    )
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
                when (q.type) {
                    QuoteAnswerType.SINGLE, QuoteAnswerType.MULTI ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { // iOS VStack spacing 10
                            q.options.forEach { option ->
                                QuoteOptionRow(
                                    label = option.label,
                                    isSelected = vm.isSelected(option),
                                    isMulti = q.type == QuoteAnswerType.MULTI,
                                    onTap = { vm.toggleOption(option) },
                                )
                            }
                        }
                    QuoteAnswerType.REGION -> RegionSelector(vm = vm, q = q, onOpenPicker = onOpenRegionPicker)
                    QuoteAnswerType.TEXT, QuoteAnswerType.DATE ->
                        FreeTextInput(vm = vm, q = q)
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
    val hint = when (q.type) {
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
