package com.muyeon.app.ui.membership

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.utils.TokenManager
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 멤버십 성과 — iOS `MembershipPerformanceView.swift` 1:1.
 *  히어로(결제 전/후) + 최근 30일 활동 + 14일 추세 + 퍼널 + 비율 도넛 + 지표 표.
 *
 * ⚠️ 화면 표기는 앱에서 실제로 쓰는 말을 그대로 쓴다(강사 목록 / 강사 프로필 / 레슨 견적요청).
 *   "상세 열람" 같은 내부 용어를 쓰면 무엇을 센 수치인지 알 수 없다(iOS 주석과 동일).
 */
@Composable
fun MembershipPerformanceScreen(
    api: MembershipPerformanceApi,
    memberType: String,
    onClose: () -> Unit,
) {
    var data by remember { mutableStateOf<MembershipPerformance?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failedMessage by remember { mutableStateOf<String?>(null) }
    var selectedMetric by remember { mutableStateOf(PerfMetric.IMPRESSIONS) }

    val isAcademy = memberType == "ACADEMY"

    LaunchedEffect(memberType) {
        loading = true
        api.load(memberType)
            .onSuccess { data = it; failedMessage = null }
            .onFailure { failedMessage = it.message ?: "성과를 불러오지 못했어요." }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(title = if (isAcademy) "학원 성과" else "강사 성과", onBack = onClose)

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            data == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    failedMessage ?: "성과를 불러오지 못했어요.",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    lineHeight = 21.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
            else -> PerformanceContent(
                d = data!!, isAcademy = isAcademy,
                selectedMetric = selectedMetric, onSelectMetric = { selectedMetric = it },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 지표 탭 — 색은 MembershipPalette 한 곳에서만 정한다(카드와 차트가 같은 색을 쓰도록). */
enum class PerfMetric {
    IMPRESSIONS, DETAIL_VIEWS, LEADS;

    fun title(isAcademy: Boolean) = when (this) {
        IMPRESSIONS -> "노출"
        DETAIL_VIEWS -> "프로필 열람"
        LEADS -> if (isAcademy) "상담·예약" else "견적요청·채팅"
    }

    val color: Color
        get() = when (this) {
            IMPRESSIONS -> MembershipPalette.impression
            DETAIL_VIEWS -> MembershipPalette.detailView
            LEADS -> MembershipPalette.lead
        }

    fun valueOf(row: MembershipDailyMetric) = when (this) {
        IMPRESSIONS -> row.impressions
        DETAIL_VIEWS -> row.detailViews
        LEADS -> row.leads
    }
}

/**
 * 문의로 치는 이벤트 — 서버 leads 정의와 같은 목록.
 *  CHAT_INTENT 는 실제로 적재되지 않아(설계만 남음) 나머지가 실수치를 만든다.
 */
private val LEAD_EVENTS = listOf(
    "CHAT_INTENT", "QUOTE_REQUEST_SUBMITTED", "CHAT_CUSTOMER_STARTED",
    "CLASS_INQUIRY_SUBMITTED", "LESSON_RESERVED",
)

@Composable
private fun PerformanceContent(
    d: MembershipPerformance,
    isAcademy: Boolean,
    selectedMetric: PerfMetric,
    onSelectMetric: (PerfMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recent = d.daily.takeLast(14)
    val trendPoints = recent.map { MembershipTrendPoint(it.date, selectedMetric.valueOf(it).toDouble()) }
    val chartTotal = recent.sumOf { selectedMetric.valueOf(it) }

    /** 단계 이름 = 회원이 실제로 거치는 화면·버튼 이름. */
    val discovery = listOf(
        (if (isAcademy) "학원 목록 노출" else "강사 목록 노출") to (d.discovery["IMPRESSION"] ?: 0),
        "카드 클릭" to (d.discovery["CARD_CLICK"] ?: 0),
        "프로필 열람" to (d.discovery["DETAIL_OPEN"] ?: 0),
        (if (isAcademy) "상담문의·예약" else "레슨 견적요청·채팅") to LEAD_EVENTS.sumOf { d.discovery[it] ?: 0 },
    )

    val f = d.funnel
    val funnelRows = if (isAcademy) {
        listOf(
            "상담문의" to f.classInquiries, "예약" to f.reservations, "출석" to f.attended,
            "노쇼" to f.noShows, "받은 지원" to f.jobApplications, "재예약 고객" to f.repeatCustomers,
        )
    } else {
        listOf(
            "받은 직접 견적요청" to f.directQuoteRequests, "선택된 견적" to f.quotesAccepted,
            "연결된 채팅" to f.chatRooms, "레슨 약속" to f.lessonAppointments,
            "완료 레슨" to f.lessonsCompleted, "받은 리뷰" to f.reviewsCreated,
            "반복 레슨 고객" to f.repeatCustomers,
        )
    }.map { it.first to (it.second ?: 0) }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 헤더
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (isAcademy) "학원을 찾은 회원의 반응이에요" else "회원들이 강사님을 찾고 있어요",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                lineHeight = 30.sp, color = MuyeonColors.textHead,
            )
            Text(
                "최근 30일 동안 노출된 횟수부터 실제 ${if (isAcademy) "문의와 예약" else "견적과 레슨"}으로 이어진 흐름을 확인해보세요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 20.sp, color = MuyeonColors.textSub,
            )
        }

        d.impact?.let { impact ->
            MembershipImpactCard(impact, isAcademy)
            val b = impact.before
            val a = impact.after
            if (impact.ready && b != null && a != null) {
                val windowDays = impact.windowDays ?: impact.daysSince
                CardShell {
                    CardTitle(
                        "멤버십 결제 전 ${windowDays}일 vs 결제 후 ${windowDays}일",
                        "회색이 결제 전, 색이 있는 쪽이 결제 후예요.",
                    )
                    MembershipBeforeAfterChart(
                        items = listOf(
                            BeforeAfterItem("노출", b.impressions.toDouble(), a.impressions.toDouble(), MembershipPalette.impressionDeep),
                            BeforeAfterItem("클릭", b.clicks.toDouble(), a.clicks.toDouble(), MembershipPalette.impressionDeep.copy(alpha = 0.65f)),
                            BeforeAfterItem("열람", b.detailViews.toDouble(), a.detailViews.toDouble(), MembershipPalette.detailView),
                            BeforeAfterItem("문의", b.leads.toDouble(), a.leads.toDouble(), MembershipPalette.lead),
                        ),
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                    )
                }
            }
        }

        // 최근 30일 활동
        CardShell {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "최근 30일 활동",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                Text(
                    "오늘 기준",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.textSub,
                )
            }
            discovery.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { (label, value) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                label,
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                                lineHeight = 16.sp, color = MuyeonColors.textSub,
                            )
                            CountUpText(value, 24.sp, MuyeonColors.textHead)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // 14일 추세
        CardShell {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "최근 14일 ${selectedMetric.title(isAcademy)}",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textSub,
                )
                CountUpText(chartTotal, 28.sp, selectedMetric.color)
            }
            // 지표 전환 — 세그먼트
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F2F7)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PerfMetric.entries.forEach { m ->
                    val on = selectedMetric == m
                    Text(
                        m.title(isAcademy),
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = if (on) MuyeonColors.textHead else MuyeonColors.textSub,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                            .background(if (on) MuyeonColors.surface else Color.Transparent)
                            .clickable { onSelectMetric(m) }
                            .padding(vertical = 9.dp),
                    )
                }
            }
            MembershipTrendChart(points = trendPoints, color = selectedMetric.color)
        }

        // 퍼널
        CardShell {
            CardTitle(
                if (isAcademy) "상담문의까지 오는 과정" else "레슨 견적요청까지 오는 과정",
                if (isAcademy) "학원 목록에 뜨고 → 카드를 누르고 → 학원 프로필을 보고 → 상담문의·예약까지 간 횟수예요."
                else "강사 목록에 뜨고 → 카드를 누르고 → 강사 프로필을 보고 → 레슨 견적요청·채팅까지 간 횟수예요.",
            )
            MembershipFunnelChart(
                steps = discovery.map { FunnelStep(it.first, it.second.toDouble()) },
                color = MembershipPalette.impressionDeep,
                modifier = Modifier.fillMaxWidth().height(190.dp),
            )
        }

        // 비율 도넛 — 분모가 0 이면 빈 원이 되어 오해를 부른다. 아예 카드를 감춘다.
        ratioData(f, isAcademy)?.let { ratio ->
            CardShell {
                CardTitle(ratio.title, ratio.subtitle)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    MembershipDonutChart(
                        primaryValue = ratio.primary, restValue = ratio.rest,
                        color = ratio.color, centerText = ratio.centerText,
                        modifier = Modifier.size(130.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendRow(ratio.color, ratio.primaryLabel, ratio.primary.roundToInt())
                        LegendRow(MembershipPalette.baseline, ratio.restLabel, ratio.rest.roundToInt())
                    }
                }
            }
        }

        // 지표 표
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (isAcademy) "문의·예약·채용" else "견적부터 재레슨까지",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                lineHeight = 24.sp, color = MuyeonColors.textHead,
            )
            Column(Modifier.clip(RoundedCornerShape(22.dp)).background(MuyeonColors.surface)) {
                funnelRows.forEachIndexed { i, (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textSub, modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${value}회",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            lineHeight = 20.sp, color = MuyeonColors.textHead,
                        )
                    }
                    if (i != funnelRows.lastIndex) {
                        HorizontalDivider(Modifier.padding(start = 20.dp), color = MuyeonColors.border)
                    }
                }
            }
        }
    }
}

/** 카드 공통 껍데기 — 카드마다 배경·모서리를 따로 적으면 값이 조금씩 어긋난다. */
@Composable
private fun CardShell(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MuyeonColors.surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun CardTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        Text(
            subtitle,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
            lineHeight = 17.sp, color = MuyeonColors.textSub,
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textSub,
        )
        Text(
            "${value}회",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
    }
}

/**
 * 0 에서 올라가는 숫자. 값이 바뀌면(지표 전환) 0 부터 다시 세지 않고 현재 값에서 이어간다 —
 *  탭을 누를 때마다 0 부터 다시 세면 읽기 전에 화면이 요동친다(iOS CountUpStat 주석과 동일).
 */
@Composable
private fun CountUpText(value: Int, size: androidx.compose.ui.unit.TextUnit, color: Color) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val shown by animateFloatAsState(
        if (started) value.toFloat() else 0f,
        tween(if (started) 450 else 900),
        label = "countUp",
    )
    Text(
        "${shown.roundToInt()}회",
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = size,
        color = color,
    )
}

private class RatioData(
    val title: String, val subtitle: String,
    val primaryLabel: String, val restLabel: String,
    val primary: Double, val rest: Double,
    val color: Color,
) {
    val centerText: String
        get() {
            val total = primary + rest
            return if (total <= 0) "0%" else "${(primary / total * 100).roundToInt()}%"
        }
}

private fun ratioData(f: MembershipFunnel, isAcademy: Boolean): RatioData? {
    if (isAcademy) {
        val attended = (f.attended ?: 0).toDouble()
        val noShows = (f.noShows ?: 0).toDouble()
        if (attended + noShows <= 0) return null
        return RatioData(
            "예약한 회원이 실제로 왔나요", "출석과 노쇼 비율이에요.",
            "출석", "노쇼", attended, noShows, MembershipPalette.lead,
        )
    }
    val sent = (f.quotesSent ?: 0).toDouble()
    val accepted = (f.quotesAccepted ?: 0).toDouble()
    if (sent <= 0) return null
    return RatioData(
        "보낸 견적이 선택된 비율", "보낸 견적 중 회원이 고른 비율이에요.",
        "선택됨", "미선택", accepted, max(sent - accepted, 0.0), MembershipPalette.lead,
    )
}

/**
 * 웹 `openMembershipPerformance` 진입점.
 *  memberType 이 비면 저장된 활동유형을 쓴다 — iOS `MembershipView` 가
 *  `memberType.isEmpty ? RoleGate.activeType : memberType` 로 넘기는 것과 같은 규칙.
 */
class MembershipPerformanceActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_MEMBER_TYPE = "memberType"

        fun start(context: Context, memberType: String?) {
            val i = Intent(context, MembershipPerformanceActivity::class.java)
                .putExtra(EXTRA_MEMBER_TYPE, memberType ?: "")
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requested = intent.getStringExtra(EXTRA_MEMBER_TYPE).orEmpty()
        val memberType = requested.ifEmpty { ActiveRole.current(this) }
        setContent {
            val api = remember { MembershipPerformanceApi(TokenManager.getAccessToken(this)) }
            MembershipPerformanceScreen(api = api, memberType = memberType, onClose = { finish() })
        }
    }
}
