package com.muyeon.app.ui.membership

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlin.math.max
import kotlin.math.roundToInt

/** 최근 N일 추세 한 점. 축 표기는 "2026-08-29" → "8/29". */
data class MembershipTrendPoint(val date: String, val value: Double) {
    val shortLabel: String
        get() {
            val parts = date.split("-")
            if (parts.size != 3) return date
            return "${parts[1].toIntOrNull() ?: 0}/${parts[2].toIntOrNull() ?: 0}"
        }
}

/**
 * 추세 차트 — iOS `MembershipTrendChart.swift` 1:1.
 *  이전/최근 구간 pill + 탭 선택 라벨 + 라인·영역 그라데이션.
 */
@Composable
fun MembershipTrendChart(
    points: List<MembershipTrendPoint>,
    color: Color,
    unit: String = "회",
    chartHeight: androidx.compose.ui.unit.Dp = 190.dp,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    // 지표를 바꾸면 이전 지표에서 고른 날짜가 그대로 남아 엉뚱한 값을 가리킨다.
    LaunchedEffect(points) { selectedIndex = null }

    /** 비교 구간 길이 — 홀수 개면 가운데 하루를 버리고 같은 길이끼리 견준다. */
    val halfCount = points.size / 2
    val previousSum = points.take(halfCount).sumOf { it.value }
    val recentSum = points.takeLast(halfCount).sumOf { it.value }
    val diff = recentSum - previousSum
    val deltaText = when {
        // 이전 구간이 0 이면 몇 % 늘었다고 말할 수 없다 — 늘어난 횟수를 그대로 보여준다.
        previousSum > 0 -> {
            val ratio = (recentSum - previousSum) / previousSum * 100
            "${if (ratio > 0) "+" else ""}${ratio.roundToInt()}%"
        }
        diff > 0 -> "+${diff.roundToInt()}$unit"
        else -> "—"
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (halfCount >= 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("이전 ${halfCount}일", "${previousSum.roundToInt()}$unit", MuyeonColors.textHead)
                StatPill("최근 ${halfCount}일", "${recentSum.roundToInt()}$unit", MuyeonColors.textHead)
                StatPill("변화", deltaText, MembershipPalette.delta(diff))
            }
        }

        // 선택 전에는 안내 문구로 자리를 지켜 차트가 위아래로 튀지 않게 한다.
        val selected = selectedIndex?.let { points.getOrNull(it) }
        Text(
            selected?.let { "${it.shortLabel} · ${it.value.roundToInt()}$unit" }
                ?: "그래프를 눌러 날짜별 수치를 볼 수 있어요",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            lineHeight = 16.sp,
            color = if (selected == null) MuyeonColors.textSub else color,
            modifier = Modifier.height(16.dp),
        )

        if (points.size < 2) {
            Box(
                Modifier.fillMaxWidth().height(chartHeight)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F2F7)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "데이터가 모이면 추세가 표시돼요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    color = MuyeonColors.textSub,
                )
            }
        } else {
            TrendCanvas(points, color, chartHeight, selectedIndex) { selectedIndex = it }
        }
    }
}

@Composable
private fun RowScope.StatPill(label: String, value: String, color: Color) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F2F7)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
            lineHeight = 13.sp, color = MuyeonColors.textSub,
        )
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            lineHeight = 21.sp, color = color, maxLines = 1,
        )
    }
}

@Composable
private fun TrendCanvas(
    points: List<MembershipTrendPoint>,
    color: Color,
    chartHeight: androidx.compose.ui.unit.Dp,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(
        fontFamily = customFontFamily, fontSize = 10.sp, color = MuyeonColors.textSub,
    )
    val density = LocalDensity.current
    // 축·여백은 iOS setExtraOffsets(left:4, top:12, right:8, bottom:4) + 좌축 라벨 폭에 맞춘다.
    val leftAxis = with(density) { 28.dp.toPx() }
    val bottomAxis = with(density) { 18.dp.toPx() }
    val topPad = with(density) { 12.dp.toPx() }

    Canvas(
        Modifier.fillMaxWidth().height(chartHeight)
            .pointerInput(points) {
                detectTapGestures { pos ->
                    val plotW = size.width - leftAxis
                    if (plotW <= 0) return@detectTapGestures
                    val step = plotW / max(points.size - 1, 1)
                    val idx = ((pos.x - leftAxis) / step).roundToInt().coerceIn(0, points.lastIndex)
                    // 같은 점을 다시 누르면 선택 해제(iOS chartValueNothingSelected 대응)
                    onSelect(if (selectedIndex == idx) null else idx)
                }
            },
    ) {
        val plotW = size.width - leftAxis
        val plotH = size.height - bottomAxis - topPad
        // 횟수 지표의 바닥은 0 이다(iOS leftAxis.axisMinimum = 0).
        val maxV = max(points.maxOf { it.value }, 1.0)
        val step = plotW / max(points.size - 1, 1)

        fun px(i: Int) = leftAxis + step * i
        fun py(v: Double) = topPad + plotH - (v / maxV * plotH).toFloat()

        // y 눈금 3개 + 점선 그리드(iOS gridLineDashLengths [3,4])
        val dash = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f)
        repeat(3) { k ->
            val v = maxV * k / 2.0
            val y = py(v)
            drawLine(
                MuyeonColors.border, Offset(leftAxis, y), Offset(size.width, y),
                strokeWidth = 1f, pathEffect = dash,
            )
            val m = measurer.measure(v.roundToInt().toString(), axisStyle)
            drawText(m, topLeft = Offset(leftAxis - m.size.width - 4f, y - m.size.height / 2f))
        }

        // 영역(그라데이션) + 라인
        val line = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(px(i), py(p.value)) else lineTo(px(i), py(p.value)) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(px(points.lastIndex), topPad + plotH)
            lineTo(px(0), topPad + plotH)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)),
                startY = topPad, endY = topPad + plotH,
            ),
        )
        drawPath(line, color, style = Stroke(width = with(density) { 2.5.dp.toPx() }))

        // 선택 하이라이트 — 세로 점선(iOS highlightLineDashLengths [4,2], 가로선 없음)
        selectedIndex?.let { i ->
            points.getOrNull(i)?.let { p ->
                val x = px(i)
                drawLine(
                    MuyeonColors.textHead, Offset(x, topPad), Offset(x, topPad + plotH),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 2f), 0f),
                )
                drawCircle(color, radius = with(density) { 4.dp.toPx() }, center = Offset(x, py(p.value)))
            }
        }

        // x축 라벨 — iOS labelCount 4
        val stride = max(points.size / 4, 1)
        points.forEachIndexed { i, p ->
            if (i % stride != 0 && i != points.lastIndex) return@forEachIndexed
            val m = measurer.measure(p.shortLabel, axisStyle)
            val x = (px(i) - m.size.width / 2f).coerceIn(0f, size.width - m.size.width)
            drawText(m, topLeft = Offset(x, size.height - m.size.height))
        }
    }
}
