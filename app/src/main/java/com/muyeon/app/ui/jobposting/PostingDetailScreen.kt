package com.muyeon.app.ui.jobposting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.DialogAction
import com.muyeon.app.ui.quote.DialogMessage
import com.muyeon.app.ui.quote.DialogTitle
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.quote.stringList
import com.muyeon.app.ui.quote.stringOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 내 공고 관리 → 공고 상세(네이티브) — iOS `PostingDetailView` 와 같은 구성. ★ 2026-10-04
 *  종전엔 카드 탭·'공고 보기'가 웹 상세로 나가(openWebAndFinish) 내 공고 관리가 통째로 닫혔다.
 *
 *  뼈대(위→아래): 내비바(뒤로·종류명·⋮) → 대표 이미지 → 종류·상태 칩·D-day → 제목·단체명 → 지원/조회
 *   → 종류별 본문(JOB 채용 / SUB 대타 + 긴급 발송 현황 / CASTING 공연) → '받은 지원자 N명'(웹 목록)
 *   → 하단 버튼(채용중: [보류][마감하기] / 그 외: [다시 열기]).
 *  데이터: 목록 응답의 preview 로 즉시 그리고, GET /{jobs|subs|casting}/:id 로 갱신.
 *   ⚠️ 지원·조회 수와 상태는 상세 API 에 없어 /me/postings 카드에서 다시 읽는다.
 */
@Composable
fun PostingDetailScreen(
    api: JobPostingApi,
    kind: String,
    id: Int,
    initial: MyPosting?,
    onBack: () -> Unit,
    onEdit: (String, Int) -> Unit,
    onApplicants: (String, Int) -> Unit,
    onOpenMembership: () -> Unit,
    onChanged: () -> Unit,
) {
    var summary by remember { mutableStateOf(initial) }
    var detail by remember { mutableStateOf(initial?.preview) }
    var loading by remember { mutableStateOf(initial?.preview == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        api.myPostings().onSuccess { list -> list.firstOrNull { it.kind == kind && it.id == id }?.let { summary = it } }
        api.loadPosting(kind, id)
            .onSuccess { detail = it }
            .onFailure { if (detail == null) error = it.message ?: "공고를 불러오지 못했어요." }
        loading = false
    }

    LaunchedEffect(kind, id) { reload() }

    val actions = rememberPostingActions(api) { _, change ->
        onChanged()
        if (change == PostingChange.DELETED) onBack() else scope.launch { reload() }
    }
    val ref = PostingRef(kind, id)
    val kindLabel = JobPostingOptions.kindLabel[kind] ?: kind
    val status = summary?.status ?: detail?.stringOrNull("status")

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(
            title = "${kindLabel}공고",
            onBack = onBack,
            trailing = {
                Box {
                    Icon(
                        Icons.Filled.MoreVert, "더보기", tint = MuyeonColors.textHead,
                        modifier = Modifier.size(44.dp).clickable { menuOpen = true }.padding(12.dp),
                    )
                    // 수정은 채용(네이티브 위저드)·대타(웹 수정)만 — 공연은 수정 화면이 없다.
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        val menu = buildList<Pair<String, () -> Unit>> {
                            if (kind != "CASTING") add("수정" to { onEdit(kind, id) })
                            add("복사" to { actions.duplicate(ref) })
                            add("삭제" to { actions.requestDelete(ref) })
                        }
                        menu.forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(label, fontFamily = customFontFamily, fontSize = 14.sp) },
                                onClick = { menuOpen = false; action() },
                            )
                        }
                    }
                }
            },
        )

        val d = detail
        when {
            d == null && loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            d == null -> Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), Alignment.Center) {
                Text(
                    error ?: "공고를 찾을 수 없어요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DetailHeader(kind, d, summary, status)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
                        when (kind) {
                            "SUB" -> {
                                SubStatusLine(api, id, d, status)
                                SubBody(d)
                                if (SubDispatch.isDispatch(d)) {
                                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
                                    DispatchCard(
                                        api = api, id = id, s = d, status = status,
                                        onApplicants = { onApplicants(kind, id) },
                                        onOpenMembership = onOpenMembership,
                                        onChanged = { onChanged(); scope.launch { reload() } },
                                    )
                                }
                            }
                            "CASTING" -> CastingBody(d)
                            else -> JobPostingDetailContent(JobForm.from(d), showPlaceholders = false)
                        }
                        Spacer(Modifier.height(4.dp))
                        ApplicantsCard(summary?.applicants ?: 0) { onApplicants(kind, id) }
                    }
                }
                // 하단 버튼 — 채용중: [보류][마감하기] / 보류·마감·임시저장: [다시 열기]
                Column(Modifier.fillMaxWidth().background(MuyeonColors.surface)) {
                    HorizontalDivider(color = MuyeonColors.border)
                    Row(
                        Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (status == "OPEN") {
                            JobButton("보류", filled = false, enabled = !actions.busy, modifier = Modifier.weight(1f)) {
                                actions.setStatus(ref, "HOLD")
                            }
                            JobButton("마감하기", filled = true, enabled = !actions.busy, modifier = Modifier.weight(1f)) {
                                actions.requestClose(ref)
                            }
                        } else {
                            JobButton("다시 열기", filled = true, enabled = !actions.busy, modifier = Modifier.weight(1f)) {
                                actions.setStatus(ref, "OPEN")
                            }
                        }
                    }
                }
            }
        }
    }

    actions.Dialogs()
}

// MARK: 공통 머리

@Composable
private fun DetailHeader(kind: String, d: JSONObject, summary: MyPosting?, status: String?) {
    // 대표 이미지가 없으면 상세 이미지 첫 장(웹 ImageCarousel 과 동일 폴백). 없으면 자리를 차지하지 않는다.
    val hero = d.stringOrNull("imageUrl") ?: d.stringOrNull("image") ?: d.stringList("images")?.firstOrNull()
    hero?.let {
        AsyncImage(
            QuoteUi.imageUrl(it), null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFF7F7F7)),
        )
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Chip(JobPostingOptions.kindLabel[kind] ?: kind, MuyeonColors.primary, MuyeonColors.primary.copy(alpha = 0.12f))
            Chip(
                JobPostingOptions.statusLabel(status),
                if (status == "OPEN") MuyeonColors.green else MuyeonColors.secondary,
                Color(0xFFF2F2F7),
            )
            if (kind == "SUB" && d.optBoolean("urgent")) Chip("긴급", MuyeonColors.danger, MuyeonColors.danger.copy(alpha = 0.10f))
            summary?.dday?.let { dd ->
                Text(
                    if (dd < 0) "마감 지남" else if (dd == 0) "오늘 마감" else "D-$dd",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    lineHeight = 13.sp, color = MuyeonColors.orange,
                )
            }
        }
        Text(
            d.stringOrNull("title") ?: summary?.title ?: "(제목 없음)",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
            lineHeight = 27.sp, color = MuyeonColors.textHead,
        )
        val org = when (kind) {
            "CASTING" -> d.stringOrNull("team")
            "SUB" -> listOfNotNull(d.stringOrNull("academy"), d.stringOrNull("address") ?: d.stringOrNull("region"))
                .joinToString(" · ").ifEmpty { null }
            else -> d.stringOrNull("academy")
        }
        org?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textSub,
            )
        }
        summary?.let { sm ->
            // 대타는 조회수를 집계하지 않는다(목록 카드와 같은 기준).
            val counts = listOfNotNull(
                "지원 ${sm.applicants ?: 0}명",
                if (kind != "SUB") "조회 ${sm.views ?: 0}" else null,
            ).joinToString(" · ")
            Text(
                counts,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.secondary,
            )
        }
    }
}

@Composable
private fun Chip(text: String, fg: Color, bg: Color) = Text(
    text,
    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp,
    color = fg,
    modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 7.dp, vertical = 3.dp),
)

/** '받은 지원자 N명' — 지원자 목록은 웹(이력서 열람은 거기서 네이티브로 넘어간다). */
@Composable
private fun ApplicantsCard(count: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "받은 지원자",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "${count}명",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.primary,
                )
            }
            Text(
                "지원자 이력서와 진행 상태를 확인해보세요.",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.textSub,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron,
            modifier = Modifier.size(22.dp),
        )
    }
}

// MARK: 대타(SUB) 본문 — 웹 SubDetail '수업 정보 · 준비/안내' 와 같은 항목

@Composable
private fun SubBody(s: JSONObject) {
    val genre = s.stringOrNull("genre") ?: s.stringOrNull("field")?.let { SubOptions.fieldLabel(it) }
    val careerText = JobFormOptions.careerLevelsLabel(s.stringList("careerLevels")).ifEmpty {
        s.stringOrNull("career")?.let { JobFormOptions.careerLevelLabel(it) }.orEmpty()
    }
    PostingHead("수업 정보")
    PostingRow("학원 위치", s.stringOrNull("address") ?: s.stringOrNull("location") ?: s.stringOrNull("region"))
    PostingRow("기간", SubOptions.scheduleLabel(s.stringOrNull("scheduleType")))
    PostingRow("타임 수", SubOptions.classCountLabel(s.num("classCount")))
    PostingRow("수업 날짜", s.stringOrNull("classDate")?.let(::tbd))
    PostingRow("수업 시간", s.stringOrNull("classTime"))
    PostingRow("수업 분야", genre)
    PostingRow("세부 분야", s.stringList("fields")?.joinToString(", ") { SubOptions.fieldLabel(it) })
    PostingRow("수업 대상", s.stringOrNull("target")?.let { SubOptions.targetLabel(it) })
    PostingRow("급여", JobFormOptions.salaryLabel(s.stringOrNull("salary")).ifEmpty { s.stringOrNull("pay").orEmpty() })
    PostingRow("필요 경력", careerText)

    val materials = s.stringOrNull("materials") ?: s.stringOrNull("prepare")
    val notice = s.stringOrNull("notice") ?: s.stringOrNull("caution")
    val desc = s.stringOrNull("description") ?: s.stringOrNull("content")
    if (materials != null || notice != null || desc != null) {
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
        PostingHead("준비 · 안내")
        PostingRow("준비물", materials)
        notice?.let { LabeledBody("주의사항", it) }
        desc?.let { LabeledBody("수업 내용", it) }
    }
    s.stringList("images")?.takeIf { it.isNotEmpty() }?.let {
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
        PostingHead("상세 이미지")
        PostingImageStack(it)
    }
}

// MARK: 대타 지금 상태 문장 — 웹 SubDetail(올린 분) 상태 안내와 같은 우선순위. ★ 2026-10-05

@Composable
private fun SubStatusLine(api: JobPostingApi, id: Int, s: JSONObject, status: String?) {
    val confirmedId = s.num("confirmedApplicationId")?.takeIf { it > 0 }
    var confirmedName by remember(confirmedId) { mutableStateOf<String?>(null) }
    LaunchedEffect(confirmedId) {
        if (confirmedId != null) api.subApplicantName(id, confirmedId).onSuccess { confirmedName = it }
    }
    val agreedPay = s.num("agreedPay")?.takeIf { it > 0 }
    val pendingOffers = s.optJSONObject("dispatchStats")?.num("pendingOffers") ?: 0
    val started = QuoteUi.parseDate(s.stringOrNull("classStartAt"))?.let { it <= System.currentTimeMillis() } ?: false

    val text = when {
        confirmedId != null -> listOfNotNull(
            confirmedName?.let { "$it 강사로 확정" } ?: "강사 확정 완료",
            agreedPay?.let { "타임당 ${SubDispatch.won(it)}" },
        ).joinToString(" · ")
        status == "HOLD" -> "잠시 보류한 공고예요. 다시 열면 이어서 진행돼요"
        status == "CLOSED" || status == "ARCHIVED" || started -> "마감된 공고예요"
        pendingOffers > 0 -> "지원한 강사 ${pendingOffers}명 · 확정해 주세요"
        else -> null
    } ?: return
    val highlight = confirmedId != null || pendingOffers > 0 && status == "OPEN"
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp,
        color = if (highlight) MuyeonColors.primary else MuyeonColors.textHead,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (highlight) MuyeonColors.primary.copy(alpha = 0.08f) else Color(0xFFF7F7F7))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

// MARK: 공연(CASTING) 본문 — 웹 CastingDetail 과 같은 항목(등록폼 확장 필드는 details 안)

@Composable
private fun CastingBody(c: JSONObject) {
    val d = c.optJSONObject("details") ?: JSONObject()
    val req = d.optJSONObject("requirements") ?: JSONObject()
    val reqList = listOfNotNull(
        "영상".takeIf { req.optBoolean("video") },
        "프로필 사진".takeIf { req.optBoolean("photo") },
        "수상경력".takeIf { req.optBoolean("award") },
        "공연경력".takeIf { req.optBoolean("career") },
        "자기소개".takeIf { req.optBoolean("intro") },
    )
    PostingHead("공연 정보")
    PostingRow("장르", c.stringOrNull("genre"))
    PostingRow("공연 일정", c.stringOrNull("schedule")?.let(::tbd))
    PostingRow("리허설 기간", d.stringOrNull("rehearsal")?.let(::tbd))
    PostingRow("공연 장소", c.stringOrNull("place"))
    PostingRow("지원 마감일", d.stringOrNull("deadline")?.let(::tbd))

    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
    PostingHead("모집 조건")
    PostingRow(
        "모집 역할",
        listOfNotNull(c.stringOrNull("role"), c.num("headcount")?.let { "${it}명" }).joinToString(" ").ifEmpty { null },
    )
    PostingRow(
        "신체 조건",
        listOfNotNull(c.stringOrNull("gender"), d.stringOrNull("age"), c.stringOrNull("height")).joinToString(" · "),
    )
    PostingRow("전공 조건", d.stringOrNull("major"))
    PostingRow("수상경력 우대", if (c.optBoolean("awardPreferred")) "예" else "아니오")
    PostingRow("영상 제출", if (c.optBoolean("videoRequired")) "필수" else "선택")
    PostingRow("제출물 필수", reqList.joinToString(", "))

    val fee = d.stringOrNull("fee")
    val rehearsalFee = d.optBoolean("rehearsalFee")
    val transportFee = d.optBoolean("transportFee")
    if (fee != null || rehearsalFee || transportFee) {
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
        PostingHead("비용")
        PostingRow("출연료", fee)
        PostingRow("리허설비 지급", if (rehearsalFee) "지급" else null)
        PostingRow("교통비 지급", if (transportFee) "지급" else null)
    }
    (c.stringOrNull("description") ?: c.stringOrNull("desc"))?.let {
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
        PostingHead("상세 설명")
        PostingBody(it)
    }
    val apply = d.stringOrNull("applyMethod")
    val contact = d.stringOrNull("contact")
    if (apply != null || contact != null) {
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
        PostingHead("지원 방법")
        PostingRow("지원 방법", apply)
        PostingRow("문의 방법", contact)
    }
    c.stringList("images")?.takeIf { it.isNotEmpty() }?.let {
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
        PostingHead("상세 이미지")
        PostingImageStack(it)
    }
}

@Composable
private fun LabeledBody(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
        PostingBody(text)
    }
}

// MARK: 긴급 발송 현황 카드 — 웹 SubDetail '긴급 발송 현황' + constants/subDispatch.js 이식

@Composable
private fun DispatchCard(
    api: JobPostingApi,
    id: Int,
    s: JSONObject,
    status: String?,
    onApplicants: () -> Unit,
    onOpenMembership: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var raiseOpen by remember { mutableStateOf(false) }
    var raiseAmount by remember { mutableStateOf("") }
    var stopOpen by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    var errorDialog by remember { mutableStateOf<SubDispatch.ErrorDialog?>(null) }

    val classCount = s.num("classCount")
    val currentPay = s.num("currentPay")?.takeIf { it > 0 } ?: s.num("payPerSession")
    // ★ 2026-10-05 최대 금액·인상폭은 더 이상 받지 않는다(자동 인상 없음). payMax 는 옛 글에만 남아 있어 상한으로만 쓴다.
    val payMax = s.num("payMax")?.takeIf { it > 0 }
    val agreedPay = s.num("agreedPay")
    val stats = s.optJSONObject("dispatchStats")
    val pendingOffers = stats?.num("pendingOffers") ?: 0
    val enabled = s.optBoolean("dispatchEnabled")
    val confirmed = (s.num("confirmedApplicationId") ?: 0) > 0
    val closed = status == "CLOSED"
    val active = enabled && !confirmed && !closed   // 발송 중지 = dispatchEnabled false
    val atMax = payMax != null && (currentPay ?: 0) > 0 && (currentPay ?: 0) >= payMax
    val hasOffers = pendingOffers > 0
    // ★ 2026-10-04 수락이 한 명이라도 있으면 금액 인상이 멈춘다 — 서버 DISPATCH_HAS_OFFERS 와 같은 규칙.
    // ★ 2026-10-05 아무도 수락하지 않으면 서버가 dispatchNudge 를 켠다 — 금액을 올려 보라고 권한다.
    //  nextRaiseAt 은 이제 다음 권유 시각이라 화면에 보이지 않는다('다음 재발송' 행 제거).
    val nudge = active && !hasOffers && !atMax && s.optBoolean("dispatchNudge")

    fun handle(e: Throwable, fallback: String) {
        val dialog = SubDispatch.errorDialog(e)
        if (dialog != null) errorDialog = dialog else info = e.message ?: fallback
    }

    PostingHead("긴급 발송 현황")
    PostingRow("현재 금액", SubDispatch.payText(currentPay, classCount))
    if ((agreedPay ?: 0) > 0) PostingRow("확정 금액", SubDispatch.payText(agreedPay, classCount))
    stats?.let {
        PostingRow("알림 받은 강사", "${it.num("recipients") ?: 0}명")
        PostingRow("지원한 강사", "${it.num("accepts") ?: 0}명")
    }
    s.stringOrNull("lastDispatchedAt")?.let { at ->
        Text(
            "${(s.num("dispatchRound") ?: 0).coerceAtLeast(1)}번째 발송 · ${SubDispatch.kstDateTime(at)}",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
    // ★ 2026-10-05 수락한 강사 보기는 발송을 멈춘 뒤에도(확정 전 수락자가 남아 있으면) 보인다 — iOS 와 동일.
    if (hasOffers && !confirmed) {
        if (active) {
            Text(
                "지원한 강사가 있어 금액을 올릴 수 없어요. 지원한 강사 중에서 확정해 주세요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 18.sp, color = MuyeonColors.primary,
            )
        }
        JobButton("지원한 강사 보기", filled = false, enabled = true, modifier = Modifier.fillMaxWidth(), onClick = onApplicants)
    }
    // '다음 재발송' 행이 빠져 멈춘 상태를 따로 알린다(확정·마감은 상단 상태 문장이 알린다).
    if (!enabled && !confirmed && !closed) {
        Text(
            "발송을 멈췄어요",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
    if (nudge) {
        Text(
            "아직 지원한 강사가 없어요. 금액을 올려 보시는 건 어때요?",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 18.sp, color = MuyeonColors.primary,
        )
    }
    if (active) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            JobButton("발송 중지", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) { stopOpen = true }
            JobButton(
                "금액 올려 다시 보내기", filled = true, enabled = !busy && !atMax && !hasOffers,
                // 잠긴 상태가 눈에 보이게 흐리게 — JobButton 은 비활성 모양이 따로 없다.
                modifier = Modifier.weight(1f).alpha(if (atMax || hasOffers) 0.4f else 1f),
            ) {
                // ★ 2026-10-05 기본값 = 옛 글의 인상폭, 없으면 5,000원.
                raiseAmount = (s.num("payStep")?.takeIf { it > 0 } ?: 5000).toString()
                raiseOpen = true
            }
        }
    }

    if (raiseOpen) {
        val amount = raiseAmount.toIntOrNull()
        // ★ 2026-10-05 1,000원 단위 양수만 — 상한은 없다(옛 글에 payMax 가 있으면 그것만 상한).
        val validAmount = amount?.takeIf { it > 0 && it % 1000 == 0 }
        val preview = when {
            amount == null -> "올릴 금액을 입력해 주세요."
            validAmount == null -> "1,000원 단위로 입력해 주세요."
            else -> {
                val next = (currentPay ?: 0) + validAmount
                "${SubDispatch.payText(payMax?.let { minOf(next, it) } ?: next, classCount)}으로 다시 보내요."
            }
        }
        AlertDialog(
            onDismissRequest = { if (!busy) raiseOpen = false },
            title = { DialogTitle("금액을 올려 다시 보낼까요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount?.let { SubDispatch.number(it) }.orEmpty(),
                        onValueChange = { v -> raiseAmount = v.filter { it.isDigit() }.take(9).trimStart('0') },
                        singleLine = true,
                        label = { Text("올릴 금액 (타임당, 원)", fontFamily = customFontFamily, fontSize = 13.sp) },
                        placeholder = { Text("예: 5,000", fontFamily = customFontFamily, fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DialogMessage(preview)
                    DialogMessage("직전 발송 30분 뒤부터 다시 보낼 수 있어요.")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && validAmount != null,
                    onClick = {
                        busy = true
                        scope.launch {
                            api.raiseDispatch(id, validAmount)
                                .onSuccess { raiseOpen = false; info = "금액을 올려 다시 보냈어요."; onChanged() }
                                .onFailure { raiseOpen = false; handle(it, "다시 보내지 못했어요. 잠시 후 다시 시도해 주세요.") }
                            busy = false
                        }
                    },
                ) { DialogAction("다시 보내기") }
            },
            dismissButton = { TextButton(onClick = { raiseOpen = false }) { DialogAction("취소") } },
        )
    }
    if (stopOpen) {
        QuoteDialog(
            title = "긴급 발송을 멈출까요?",
            message = "더 이상 강사에게 알림을 보내지 않아요. 이미 지원한 강사는 그대로 확정할 수 있어요.",
            confirmText = "발송 중지",
            onConfirm = {
                stopOpen = false
                busy = true
                scope.launch {
                    api.stopDispatch(id)
                        .onSuccess { info = "긴급 발송을 멈췄어요."; onChanged() }
                        .onFailure { handle(it, "발송을 멈추지 못했어요. 잠시 후 다시 시도해 주세요.") }
                    busy = false
                }
            },
            onDismiss = { stopOpen = false },
        )
    }
    info?.let { PostingInfoDialog("알림", it) { info = null } }
    errorDialog?.let { e ->
        if (e.membership) {
            PostingConfirmDialog(
                title = e.title, message = e.body, confirmText = "멤버십 보기", dismissText = "다음에",
                onConfirm = { errorDialog = null; onOpenMembership() },
                onDismiss = { errorDialog = null },
            )
        } else {
            PostingInfoDialog(e.title, e.body) { errorDialog = null }
        }
    }
}

// MARK: 옵션·헬퍼

/** "-" = 미정(웹 util/date tbd 와 동일). */
private fun tbd(v: String): String = if (v == "-") "미정" else v

/** 숫자 필드 — 서버가 숫자/문자열(decimal) 어느 쪽으로 줘도 정수로. */
internal fun JSONObject.num(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optString(key).toDoubleOrNull()?.toInt()
}

/** 대타공고 옵션 — 웹 constants/subOptions.js 와 **값 계약**(채용 TEACHING_FIELDS 와 다른 표). */
internal object SubOptions {
    private val fields = mapOf(
        // 이전에 저장된 글은 기존 이름으로 계속 읽는다(웹 LEGACY_FIELD_MAP).
        "BALLET_KIDS" to "유아반", "BALLET_ELEM" to "초등반", "BALLET_TEEN" to "중등반",
        "BALLET_MAJOR" to "전공반", "BALLET_EXAM" to "입시반", "BALLET_ADULT" to "성인취미",
        "KOREAN" to "한국무용", "MODERN" to "현대무용", "PRACTICAL" to "실용무용",
        "BALLET_FIT" to "발레핏", "BARRE" to "바레",
        "BASIC" to "기초반", "HOBBY" to "취미반", "MAJOR" to "전공반", "EXAM" to "입시반",
        "REPERTOIRE" to "작품지도", "CONCOURS" to "콩쿠르지도", "AUDITION" to "오디션준비",
        "PILATES" to "필라테스", "STRETCH" to "스트레칭", "OTHER" to "기타",
    )
    private val targets = mapOf(
        "KIDS" to "유아", "ELEM" to "초등", "TEEN" to "중고등", "ADULT" to "성인", "EXAM" to "입시생", "MAJOR" to "전공",
    )
    private val schedules = mapOf("ONE_DAY" to "하루", "SHORT" to "단기", "REGULAR" to "정기")

    fun fieldLabel(v: String) = fields[v] ?: v
    fun targetLabel(v: String) = targets[v] ?: v
    fun scheduleLabel(v: String?) = v?.let { schedules[it] }.orEmpty()
    fun classCountLabel(n: Int?) = if ((n ?: 0) > 0) "${n}타임" else ""
}

/** 긴급 대타 화면 헬퍼 — 웹 constants/subDispatch.js 이식. 판정은 서버가 하고 화면은 문구만. */
internal object SubDispatch {
    fun isDispatch(s: JSONObject) = s.optBoolean("dispatchEnabled") || (s.num("dispatchRound") ?: 0) > 0

    fun number(n: Int): String = NumberFormat.getInstance(Locale.KOREA).format(n)
    fun won(n: Int): String = "${number(n)}원"

    /** "타임당 50,000원 (3타임 150,000원)" — 서버가 pay 글자로 채우는 형식과 같게. */
    fun payText(perSession: Int?, classCount: Int?): String {
        val p = perSession?.takeIf { it > 0 } ?: return ""
        val c = classCount ?: 0
        return if (c > 0) "타임당 ${won(p)} (${c}타임 ${won(p * c)})" else "타임당 ${won(p)}"
    }

    /** ISO → "10월 5일 (일) 오후 3:20" (KST). */
    fun kstDateTime(iso: String?): String {
        val ms = QuoteUi.parseDate(iso) ?: return ""
        return SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREAN)
            .apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }.format(Date(ms))
    }

    data class ErrorDialog(val title: String, val body: String, val membership: Boolean = false)

    /** 서버 오류 코드 → 안내. 해당 코드가 아니면 null. 문구는 서버 메시지를 우선한다. */
    fun errorDialog(e: Throwable): ErrorDialog? {
        val ex = e as? PostingApiException ?: return null
        val msg = ex.message?.takeIf { it.isNotBlank() && it != "요청에 실패했어요." }
        val extra = ex.data
        return when (ex.code) {
            "SUB_DISPATCH_MEMBERSHIP_REQUIRED" -> ErrorDialog(
                "긴급 발송은 유료 멤버십 전용이에요",
                msg ?: "멤버십에 가입하면 조건에 맞는 강사에게 바로 알림을 보내고, 안 잡히면 금액을 올려 다시 보낼 수 있어요.",
                membership = true,
            )
            "SUB_DISPATCH_RESTRICTED" -> {
                val until = kstDateTime(extra?.stringOrNull("restrictedUntil"))
                ErrorDialog(
                    "긴급 대타 이용이 잠시 정지됐어요",
                    msg ?: "확정 취소 누적으로 ${if (until.isNotEmpty()) "${until}까지 " else ""}긴급 대타를 이용할 수 없어요.",
                )
            }
            "DISPATCH_COOLDOWN" -> {
                val at = kstDateTime(extra?.stringOrNull("nextAvailableAt"))
                ErrorDialog(
                    "조금 뒤에 다시 보낼 수 있어요",
                    msg ?: "직전 발송 30분 뒤부터 다시 보낼 수 있어요.${if (at.isNotEmpty()) " (${at}부터)" else ""}",
                )
            }
            "DISPATCH_MAX_REACHED" -> ErrorDialog("최대 금액이에요", msg ?: "정해 둔 최대 금액까지 올렸어요.")
            "DISPATCH_HAS_OFFERS" -> ErrorDialog(
                "지원한 강사가 있어요",
                msg ?: "지원한 강사가 있어 금액을 올릴 수 없어요. 지원한 강사 중에서 확정해 주세요.",
            )
            "SUB_DISPATCH_LIMIT" -> ErrorDialog("이번 달 긴급 발송 한도를 다 썼어요", msg ?: "다음 달에 다시 이용할 수 있어요.")
            else -> null
        }
    }
}
