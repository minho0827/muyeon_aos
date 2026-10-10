package com.muyeon.app.ui.jobposting

import com.muyeon.app.result.launchScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.resume.ResumeActivity
import com.muyeon.app.ui.resume.ResumeOptions
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * 내 공고 관리 + 공고 등록 — iOS `JobPosting/MyJobPostingsView` · `JobPostingWizardView` 이식.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostingsScreen(
    api: JobPostingApi,
    onClose: () -> Unit,
    onEdit: (String, Int) -> Unit,
    onCreate: () -> Unit,
    onApplicants: (String, Int) -> Unit,
    onView: (MyPosting) -> Unit,
    onChanged: () -> Unit = {},
) {
    var postings by remember { mutableStateOf<List<MyPosting>>(emptyList()) }
    var tab by remember { mutableStateOf("ALL") }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 보관함 — 삭제가 소프트(ARCHIVED)라 되살릴 경로가 반드시 있어야 한다.
    var archived by remember { mutableStateOf<List<MyPosting>>(emptyList()) }
    var showArchived by remember { mutableStateOf(false) }

    suspend fun load() {
        api.myPostings()
            .onSuccess { postings = it }
            .onFailure { toast = it.message ?: "공고 목록을 불러오지 못했어요." }
        api.archived()
            .onSuccess { archived = it }
            .onFailure { toast = it.message ?: "보관함을 불러오지 못했어요." }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    // 상태 변경·마감·복사·삭제 — 상세와 같은 PostingActions(2026-10-04).
    val actions = rememberPostingActions(api) { _, _ -> onChanged(); scope.launch { load() } }

    // 인증 대기 공고는 status=OPEN 이지만 아직 모집 전 — '채용중' 탭에서 뺀다(전체에만 보임).
    val filtered = (if (tab == "ALL") postings else postings.filter { it.status == tab && !(tab == "OPEN" && it.pendingVerification) })
        .sortedWith(
            compareBy<MyPosting> { JobPostingOptions.statusPriority(it.status) }
                .thenByDescending { it.updatedAt ?: it.createdAt.orEmpty() }
        )

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "내 공고", onBack = onClose)

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            JobPostingOptions.tabs.forEach { (key, label) ->
                val on = tab == key
                Text(
                    label,
                    fontFamily = customFontFamily,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                    color = if (on) Color.White else MuyeonColors.textSub,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                        .clickable { tab = key }.padding(vertical = 8.dp),
                )
            }
        }

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            filtered.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                QuoteEmptyState(Icons.Outlined.WorkOutline, "등록한 공고가 없어요", "공고를 올리면 지원자를 받을 수 있어요.")
            }
            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.uid }) { p ->
                        PostingCard(
                            p = p,
                            // ★ 2026-10-04 카드 탭 = 네이티브 상세(종전: 채용만 수정 위저드, 나머지는 무반응).
                            onClick = { onView(p) },
                            onApplicants = { onApplicants(p.kind, p.id) },
                            onView = { onView(p) },
                            onEdit = { onEdit(p.kind, p.id) },
                            onDuplicate = { actions.duplicate(p.ref) },
                            onDelete = { actions.requestDelete(p.ref) },
                            onStatus = { s ->
                                if (s == "CLOSED") actions.requestClose(p.ref) else actions.setStatus(p.ref, s)
                            },
                        )
                    }
                    if (archived.isNotEmpty()) {
                        item(key = "archived-toggle") {
                            Text(
                                if (showArchived) "보관함 닫기" else "보관함 (${archived.size})",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(20.dp))
                                    .clickable { showArchived = !showArchived }
                                    .padding(vertical = 10.dp),
                            )
                        }
                        if (showArchived) {
                            items(archived, key = { "arc-${it.uid}" }) { p ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(MuyeonColors.groupedBg).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            p.title ?: "(제목 없음)",
                                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                            lineHeight = 17.sp, color = MuyeonColors.textHead,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${JobPostingOptions.kindLabel[p.kind] ?: p.kind} · 삭제 전 상태로 되돌아갑니다",
                                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                                            color = MuyeonColors.textSub,
                                        )
                                    }
                                    Text(
                                        "복원",
                                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                        lineHeight = 16.sp, color = MuyeonColors.primary,
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                api.restore(p.kind, p.id).onSuccess { toast = "공고를 복원했어요." }
                                                    .onFailure { toast = it.message }
                                                load()
                                            }
                                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            "공고 등록",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.primary)
                .clickable(onClick = onCreate).padding(vertical = 16.dp),
        )
    }

    actions.Dialogs()
    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

@Composable
private fun PostingCard(
    p: MyPosting,
    onClick: () -> Unit,
    onApplicants: () -> Unit,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onStatus: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                JobPostingOptions.kindLabel[p.kind] ?: p.kind,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp,
                color = MuyeonColors.primary,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(MuyeonColors.primary.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                if (p.pendingVerification) JobPostingOptions.PENDING_LABEL else JobPostingOptions.statusLabel(p.status),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp,
                color = if (p.status == "OPEN" && !p.pendingVerification) MuyeonColors.green else MuyeonColors.secondary,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF2F2F7))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            p.dday?.let { d ->
                Text(
                    if (d < 0) "마감 지남" else if (d == 0) "오늘 마감" else "D-$d",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    lineHeight = 12.sp, color = MuyeonColors.orange,
                )
            }
            Spacer(Modifier.weight(1f))
            Box {
                Icon(
                    Icons.Filled.MoreVert, "더보기", tint = MuyeonColors.chevron,
                    modifier = Modifier.size(30.dp).clickable { menuOpen = true }.padding(7.dp),
                )
                // 순서 고정(iOS actionSheet 와 동일): 공고 보기 → 수정 → 보류/다시 열기 → 복사 → 삭제 → 마감하기.
                //  ★ 삭제가 여기 없어서 종전엔 '공고 보기'로 상세까지 들어가야 지울 수 있었다.
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val menu = buildList<Pair<String, () -> Unit>> {
                        add("공고 보기" to onView)
                        // 공연(CASTING)은 수정 화면이 없다 — 숨긴다.
                        // 인증 대기 공고는 승인 전이라 수정·보류·복사·마감이 의미 없다 → 보기·삭제만.
                        val pending = p.pendingVerification
                        if (p.kind != "CASTING" && !pending) add("수정" to onEdit)
                        if (!pending) {
                            if (p.status == "OPEN") add("보류" to { onStatus("HOLD") })
                            else add("다시 열기" to { onStatus("OPEN") })
                            add("복사" to onDuplicate)
                        }
                        add("삭제" to onDelete)
                        if (p.status == "OPEN" && !pending) add("마감하기" to { onStatus("CLOSED") })
                    }
                    menu.forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label, fontFamily = customFontFamily, fontSize = 14.sp) },
                            onClick = { menuOpen = false; action() },
                        )
                    }
                }
            }
        }
        Text(
            p.title ?: "(제목 없음)",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(p.region, p.subLine.ifEmpty { null }).joinToString(" · "),
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
            color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "지원자 ${p.applicants ?: 0}",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                lineHeight = 15.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable(onClick = onApplicants),
            )
            Text(
                "조회 ${p.views ?: 0}",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.secondary,
            )
        }
    }
}

@Composable
internal fun JobField(
    label: String,
    value: String,
    placeholder: String = "",
    required: Boolean = false,
    // 모집 인원처럼 숫자만 받는 칸 — 기본은 일반 키보드.
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MuyeonColors.textHead)
            if (required) Text("*", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MuyeonColors.primary)
        }
        OutlinedTextField(
            value = value,
            onValueChange = { if (keyboard == KeyboardType.Number) onChange(it.filter { c -> c.isDigit() }) else onChange(it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontFamily = customFontFamily, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun JobChips(
    label: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MuyeonColors.textHead)
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (v, l) ->
                    val on = selected.contains(v)
                    Text(
                        l,
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1, textAlign = TextAlign.Center,
                        color = if (on) Color.White else MuyeonColors.textSub,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                            .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .clickable { onToggle(v) }.padding(vertical = 8.dp, horizontal = 2.dp),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 예/아니오 2칩 — 웹 YESNO BaseSelector 와 같은 의미. 미선택(null) = 조건 없음. */
@Composable
internal fun JobYesNo(label: String, value: Boolean?, onPick: (Boolean?) -> Unit) {
    JobChips(
        label = label,
        options = listOf("Y" to "예", "N" to "아니오"),
        selected = when (value) { true -> setOf("Y"); false -> setOf("N"); null -> emptySet() },
    ) { v ->
        val want = v == "Y"
        onPick(if (value == want) null else want)   // 같은 걸 다시 누르면 해제
    }
}

/**
 * 시작~종료 시간 — 시·분을 따로 고른다(분 5분 단위). value = "HH:MM ~ HH:MM".
 *  ⚠️ 레슨 개설에는 쓰지 않는다. 레슨은 30분 격자로 예약 회차를 만들어 5분 단위와 맞지 않는다.
 */
@Composable
internal fun JobTimeRange(label: String, value: String, onChange: (String) -> Unit) {
    val hours = remember { (0..23).map { "%02d".format(it) } }
    val minutes = remember { (0..55 step 5).map { "%02d".format(it) } }
    val parts = value.split("~").map { it.trim() }
    fun at(i: Int): String {
        val t = parts.getOrNull(i).orEmpty()
        return if (Regex("^\\d{2}:\\d{2}$").matches(t)) t else ""
    }
    val start = at(0)
    val end = at(1)
    // 시만 고르고 분을 안 고른 상태에서도 값이 남도록 분 기본값은 "00".
    fun join(h: String, m: String) = if (h.isEmpty()) "" else "$h:${m.ifEmpty { "00" }}"
    fun emit(s: String, e: String) = onChange(if (s.isEmpty() && e.isEmpty()) "" else "$s ~ $e".trim())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MuyeonColors.textHead)
        listOf("시작" to start, "종료" to end).forEach { (cap, t) ->
            val h = t.take(2)
            val m = if (t.length >= 5) t.takeLast(2) else ""
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(cap, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.width(30.dp))
                JobTimeMenu(h.ifEmpty { "시" }, hours) { v ->
                    if (cap == "시작") emit(join(v, m), end) else emit(start, join(v, m))
                }
                Text(":", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub)
                JobTimeMenu(m.ifEmpty { "분" }, minutes) { v ->
                    val hh = h.ifEmpty { "00" }
                    if (cap == "시작") emit(join(hh, v), end) else emit(start, join(hh, v))
                }
            }
        }
    }
}

@Composable
private fun JobTimeMenu(title: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            color = MuyeonColors.textHead,
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF2F2F7)).clickable { open = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o, fontFamily = customFontFamily, fontSize = 14.sp) },
                    onClick = { open = false; onPick(o) },
                )
            }
        }
    }
}

@Composable
internal fun JobButton(text: String, filled: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp,
        color = if (filled) Color.White else MuyeonColors.primary, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(if (filled) MuyeonColors.primary else MuyeonColors.primary.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 16.dp),
    )
}

/** 웹 `openMyJobPostings` 브릿지 진입점. 결과: JOB_POSTINGS. */
class JobPostingActivity : com.muyeon.app.result.ResultActivity() {

    override val defaultResultKeys = setOf(com.muyeon.app.result.ResultKeys.JOB_POSTINGS)

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_ID = "id"

        fun listIntent(context: Context) = intent(context, "list")
        fun formIntent(context: Context, jobId: Int?) = intent(context, "form").putExtra(EXTRA_ID, jobId ?: 0)

        fun startList(context: Context) = context.launchScreen(listIntent(context))
        fun startForm(context: Context, jobId: Int?) = context.launchScreen(formIntent(context, jobId))

        private fun intent(context: Context, route: String) =
            Intent(context, JobPostingActivity::class.java).putExtra(EXTRA_ROUTE, route)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "list"
        val id = intent.getIntExtra(EXTRA_ID, 0)

        setContent {
            val nav = rememberNavController()
            val api = remember { JobPostingApi(TokenManager.getAccessToken(this)) }

            fun back() { if (!nav.popBackStack()) finish() }

            // 공고 한도(멤버) → 내 공고 관리. 목록에서 들어왔으면 그 목록으로 돌아가고,
            //  웹에서 폼으로 바로 들어왔으면(startForm) 폼을 걷어내고 목록을 띄운다(같은 Activity 의 list 라우트).
            fun openMyPostings() {
                if (!nav.popBackStack("list", inclusive = false)) {
                    nav.navigate("list") { popUpTo(nav.graph.id) { inclusive = true } }
                }
            }
            fun openMembership() = com.muyeon.app.ui.membership.MembershipActivity.start(this@JobPostingActivity)

            // 목록 → 상세로 넘길 카드(preview·지원/조회 수). 상세는 이걸로 먼저 그리고 서버로 갱신한다.
            val postingCache = remember { HashMap<String, MyPosting>() }

            // 공고가 바뀌면 웹(공고 목록)도 다시 읽게 — 기본 키와 같지만 '바뀌었다'를 명시해 둔다.
            fun changed() = addResultKeys(com.muyeon.app.result.ResultKeys.JOB_POSTINGS)

            // 수정 — 채용은 네이티브 위저드, 대타는 웹 수정 화면, 공연은 수정 화면이 없다(메뉴에서 숨김).
            fun openEdit(kind: String, pid: Int) {
                when (kind) {
                    "JOB" -> nav.navigate("form/$pid")
                    "SUB" -> com.muyeon.app.webview.NativeWebRoute.openWebAndFinish(this@JobPostingActivity, "/subs/$pid/edit")
                }
            }

            // 받은 지원자 — 목록은 웹(이력서 열람은 네이티브). 공고 하나로 걸러 연다(웹 SubDetail 과 같은 쿼리).
            fun openApplicants(kind: String, pid: Int) =
                com.muyeon.app.webview.NativeWebRoute.openWebAndFinish(
                    this@JobPostingActivity, "/receivedApplications?kind=$kind&postingId=$pid",
                )

            NavHost(nav, startDestination = route) {
                composable("list") {
                    MyPostingsScreen(
                        api = api,
                        onClose = { finish() },
                        onEdit = { kind, pid -> openEdit(kind, pid) },
                        onCreate = { nav.navigate("form/0") },
                        onApplicants = { kind, pid -> openApplicants(kind, pid) },
                        // ★ 2026-10-04 공고 보기 = 네이티브 상세(종전엔 웹 상세로 나가며 이 화면을 닫았다).
                        onView = { p ->
                            postingCache[p.uid] = p
                            nav.navigate("detail/${p.kind}/${p.id}")
                        },
                        onChanged = { changed() },
                    )
                }
                composable("detail/{kind}/{id}") { e ->
                    val kind = e.arguments?.getString("kind") ?: "JOB"
                    val pid = e.arguments?.getString("id")?.toIntOrNull() ?: 0
                    PostingDetailScreen(
                        api = api, kind = kind, id = pid,
                        initial = postingCache["$kind-$pid"],
                        onBack = { back() },
                        onEdit = { k, i -> openEdit(k, i) },
                        onApplicants = { k, i -> openApplicants(k, i) },
                        onOpenMembership = { openMembership() },
                        onChanged = { changed() },
                    )
                }
                composable("form") {
                    JobPostingWizardScreen(
                        api, id.takeIf { it > 0 }, onClose = { back() }, onSaved = { back() },
                        onOpenMembership = { openMembership() },
                        onOpenMyPostings = { openMyPostings() },
                    )
                }
                composable("form/{id}") { e ->
                    val jid = e.arguments?.getString("id")?.toIntOrNull()?.takeIf { it > 0 }
                    JobPostingWizardScreen(
                        api, jid, onClose = { back() }, onSaved = { back() },
                        onOpenMembership = { openMembership() },
                        onOpenMyPostings = { openMyPostings() },
                    )
                }
            }
        }
    }
}
