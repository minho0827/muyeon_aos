package com.muyeon.app.ui.membership

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 성과 화면 공용 팔레트 + 차트 — iOS `MembershipChartKit.swift` 대응.
 *
 * ★ 색은 여기서만 정한다. 지표마다 색을 그때그때 쓰면 같은 '문의'가 카드에선 초록,
 *   차트에선 파랑으로 나와 화면이 따로 논다(iOS 주석과 같은 규칙).
 *
 * iOS 는 DGCharts 를 쓰지만 AOS 는 Compose Canvas 로 직접 그린다.
 *  MPAndroidChart 를 붙여도 DGCharts 와 기본 스타일이 달라 결국 옵션을 하나씩 맞춰야 하고,
 *  여기 필요한 네 종류(그룹막대·가로퍼널·도넛·라인)는 Canvas 가 더 정확히 재현된다.
 */
object MembershipPalette {
    /** 노출 — 브랜드 주황(#F58232). 선처럼 **가는 면적**에만 원색 그대로 쓴다. */
    val impression = MuyeonColors.primary

    /**
     * 넓게 칠하는 자리(막대·카드 배경)용 주황. 같은 주황을 큰 면적에 원색으로 깔면 눈이 아프다.
     *  면적이 커질수록 채도를 낮추는 것이 원칙이라 한 단계 어둡고 차분한 톤을 따로 둔다.
     */
    val impressionDeep = Color(0xFFCC661F)          // iOS Color(0.80, 0.40, 0.12)

    /** 히어로 카드 배경 — 아주 옅은 주황. 원색 그라데이션을 위쪽 전체에 깔지 않는다. */
    val heroTop = Color(0xFFFFF6EC)                 // iOS (1.00, 0.965, 0.925)
    val heroBottom = Color(0xFFFFECDA)              // iOS (1.00, 0.925, 0.855)

    /** 프로필 열람 — 남색 */
    val detailView = Color(0xFF405273)              // iOS (0.25, 0.32, 0.45)

    /** 문의·전환 — 초록. '늘었다'를 뜻하는 색도 같은 값이다. */
    val lead = Color(0xFF2E9E78)                    // iOS (0.18, 0.62, 0.47)

    val revenue = impressionDeep

    /** 비교 기준(결제 전) — 회색. 항상 '이전'을 뜻한다. */
    val baseline = Color(0xFFCCCCD1)                // iOS (0.80, 0.80, 0.82)

    val up = lead
    val down = Color(0xFFD94D4D)                    // iOS (0.85, 0.30, 0.30)

    fun delta(value: Double): Color = when {
        value > 0 -> up
        value < 0 -> down
        else -> MuyeonColors.textSub
    }
}

/** 막대 위 숫자 — 횟수라 소수점이 없고, 0 은 적지 않는다(빈 막대에 0 이 붙으면 지저분하다). */
private fun countLabel(v: Double): String = if (v <= 0) "" else v.roundToInt().toString()

private val axisStyle
    @Composable get() = TextStyle(
        fontFamily = customFontFamily, fontSize = 11.sp, color = MuyeonColors.textSub,
    )

private val valueStyle
    @Composable get() = TextStyle(
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
        color = MuyeonColors.textSub,
    )

/** 결제 전/후 그룹 막대 — 회색이 결제 전, 색이 있는 쪽이 결제 후. */
data class BeforeAfterItem(val label: String, val before: Double, val after: Double, val color: Color)

@Composable
fun MembershipBeforeAfterChart(items: List<BeforeAfterItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val axis = axisStyle
    val value = valueStyle
    val valueStrong = value.copy(color = MuyeonColors.textHead)
    val density = LocalDensity.current

    Canvas(modifier) {
        val axisH = with(density) { 18.dp.toPx() }
        val topPad = with(density) { 16.dp.toPx() }
        val plotH = size.height - axisH - topPad
        val maxV = max(items.maxOf { max(it.before, it.after) }, 1.0)
        val groupW = size.width / items.size
        // iOS 와 같은 비율: (barWidth .32 + barSpace .03) * 2 + groupSpace .30 = 1.0
        val barW = groupW * 0.32f
        val barSpace = groupW * 0.03f

        items.forEachIndexed { i, item ->
            val cx = groupW * i + groupW / 2f
            val left = cx - barW - barSpace / 2f
            listOf(item.before to MembershipPalette.baseline, item.after to item.color)
                .forEachIndexed { j, (v, c) ->
                    val h = (v / maxV * plotH).toFloat()
                    val x = left + j * (barW + barSpace)
                    drawRect(c, Offset(x, topPad + plotH - h), Size(barW, h))
                    countLabel(v).takeIf { it.isNotEmpty() }?.let { txt ->
                        val style = if (j == 1) valueStrong else value
                        val m = measurer.measure(txt, style)
                        drawText(
                            m, topLeft = Offset(
                                x + barW / 2f - m.size.width / 2f,
                                topPad + plotH - h - m.size.height,
                            ),
                        )
                    }
                }
            val m = measurer.measure(item.label, axis)
            drawText(m, topLeft = Offset(cx - m.size.width / 2f, size.height - m.size.height))
        }
    }
}

/**
 * 퍼널(가로 막대) — 노출 → 클릭 → 프로필 열람 → 문의.
 *  단계가 내려갈수록 색이 진해져 '좁아지는 깔때기'가 색으로도 읽힌다(iOS 와 같은 0.35~1.0 알파).
 */
data class FunnelStep(val label: String, val value: Double)

@Composable
fun MembershipFunnelChart(steps: List<FunnelStep>, color: Color, modifier: Modifier = Modifier) {
    if (steps.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val axis = axisStyle
    val value = valueStyle.copy(color = MuyeonColors.textHead, fontSize = 11.sp)
    val density = LocalDensity.current

    Canvas(modifier) {
        val labelW = with(density) { 92.dp.toPx() }
        val gap = with(density) { 8.dp.toPx() }
        val rowH = size.height / steps.size
        val barH = rowH * 0.55f
        val maxV = max(steps.maxOf { it.value }, 1.0)
        val trackW = size.width - labelW - gap - with(density) { 34.dp.toPx() }

        steps.forEachIndexed { i, step ->
            val cy = rowH * i + rowH / 2f
            // iOS 는 아래에서 위로 쌓느라 뒤집어 넣지만, 여기선 위에서 아래로 그대로 그린다.
            //  옅음→진함 방향은 iOS 와 같게(첫 단계가 가장 옅다) 맞춘다.
            val ratio = 0.35f + 0.65f * (i.toFloat() / max(steps.size - 1, 1))
            val w = (step.value / maxV * trackW).toFloat()
            val lm = measurer.measure(step.label, axis)
            drawText(lm, topLeft = Offset(labelW - lm.size.width, cy - lm.size.height / 2f))
            drawRect(
                color.copy(alpha = ratio),
                Offset(labelW + gap, cy - barH / 2f),
                Size(w, barH),
            )
            countLabel(step.value).takeIf { it.isNotEmpty() }?.let { txt ->
                val m = measurer.measure(txt, value)
                drawText(
                    m,
                    topLeft = Offset(labelW + gap + w + gap / 2f, cy - m.size.height / 2f),
                )
            }
        }
    }
}

/** 도넛(두 조각 비율) — 가운데에 퍼센트를 크게 적는다. */
@Composable
fun MembershipDonutChart(
    primaryValue: Double,
    restValue: Double,
    color: Color,
    centerText: String,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val center = TextStyle(
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color,
    )
    val density = LocalDensity.current

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val p = max(primaryValue, 0.0)
            val r = max(restValue, 0.0)
            val total = p + r
            // holeRadiusPercent 0.72 → 링 두께는 반지름의 28%
            val radius = size.minDimension / 2f
            val stroke = radius * 0.28f
            val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
            val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
            if (total <= 0) {
                drawArc(
                    MembershipPalette.baseline, -90f, 360f, false,
                    topLeft, arcSize, style = Stroke(stroke),
                )
            } else {
                val sweep = (p / total * 360f).toFloat()
                // sliceSpace 2 — 조각 사이를 살짝 띄운다
                drawArc(color, -90f, sweep - 2f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(
                    MembershipPalette.baseline, -90f + sweep, 360f - sweep - 2f, false,
                    topLeft, arcSize, style = Stroke(stroke),
                )
            }
            val m = measurer.measure(centerText, center)
            drawText(
                m,
                topLeft = Offset(
                    size.width / 2f - m.size.width / 2f,
                    size.height / 2f - m.size.height / 2f,
                ),
            )
        }
    }
}
