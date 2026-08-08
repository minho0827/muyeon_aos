package com.muyeon.app.ui.jobposting

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
import androidx.compose.material3.Text
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
@Composable
fun MyPostingsScreen(
    api: JobPostingApi,
    onClose: () -> Unit,
    onEdit: (Int) -> Unit,
    onCreate: () -> Unit,
    onApplicants: (Int) -> Unit,
) {
    var postings by remember { mutableStateOf<List<MyPosting>>(emptyList()) }
    var tab by remember { mutableStateOf("ALL") }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        postings = api.myPostings().getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    val filtered = if (tab == "ALL") postings else postings.filter { it.status == tab }

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
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.uid }) { p ->
                    PostingCard(
                        p = p,
                        onClick = { if (p.kind == "JOB") onEdit(p.id) },
                        onApplicants = { onApplicants(p.id) },
                        onDuplicate = {
                            scope.launch {
                                api.duplicate(p.kind, p.id).onSuccess { toast = "임시저장으로 복사했어요." }
                                    .onFailure { toast = it.message }
                                load()
                            }
                        },
                        onStatus = { s ->
                            scope.launch {
                                api.setStatus(p.kind, p.id, s).onFailure { toast = it.message }
                                load()
                            }
                        },
                    )
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

    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

@Composable
private fun PostingCard(
    p: MyPosting,
    onClick: () -> Unit,
    onApplicants: () -> Unit,
    onDuplicate: () -> Unit,
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
                JobPostingOptions.statusLabel(p.status),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp,
                color = if (p.status == "OPEN") MuyeonColors.green else MuyeonColors.secondary,
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
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("복사", fontFamily = customFontFamily, fontSize = 14.sp) },
                        onClick = { menuOpen = false; onDuplicate() },
                    )
                    listOf("OPEN" to "채용중으로", "HOLD" to "보류로", "CLOSED" to "마감으로")
                        .filter { it.first != p.status }
                        .forEach { (s, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontFamily = customFontFamily, fontSize = 14.sp) },
                                onClick = { menuOpen = false; onStatus(s) },
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

/** 공고 등록/수정 — iOS JobPostingWizardView 를 단일 폼으로 압축(단계 대신 섹션). */
@Composable
fun JobPostingFormScreen(
    api: JobPostingApi,
    jobId: Int?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    var form by remember { mutableStateOf(JobForm()) }
    var loading by remember { mutableStateOf(jobId != null) }
    var saving by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(jobId) {
        jobId?.let { api.loadJob(it).onSuccess { f -> form = f } }
        loading = false
    }

    fun save(asDraft: Boolean) {
        if (form.title.isBlank()) { toast = "공고 제목을 입력해 주세요."; return }
        saving = true
        scope.launch {
            api.saveJob(jobId, form.copy(status = if (asDraft) "DRAFT" else "OPEN"))
                .onSuccess { onSaved() }.onFailure { toast = it.message }
            saving = false
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = if (jobId != null) "공고 수정" else "공고 등록", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            JobField("공고 제목", form.title, required = true) { form = form.copy(title = it) }
            JobField("학원·단체명", form.academy.orEmpty()) { form = form.copy(academy = it) }
            JobChips("장르", JobFormOptions.genres.map { it to it }, setOfNotNull(form.genre)) {
                form = form.copy(genre = if (form.genre == it) null else it)
            }
            JobChips("모집 분야", ResumeOptions.teachingFields, (form.fields ?: emptyList()).toSet()) { v ->
                val cur = form.fields ?: emptyList()
                form = form.copy(fields = if (cur.contains(v)) cur - v else cur + v)
            }
            JobChips("수업 대상", ResumeOptions.classTargets, setOfNotNull(form.target)) {
                form = form.copy(target = if (form.target == it) null else it)
            }
            JobField("지역", form.region.orEmpty(), "예: 서울 강남구") { form = form.copy(region = it) }
            JobField("주소", form.address.orEmpty()) { form = form.copy(address = it) }
            JobField("가까운 역", form.subway.orEmpty()) { form = form.copy(subway = it) }
            JobField("근무 요일", form.days.orEmpty(), "예: 월·수·금") { form = form.copy(days = it) }
            JobField("근무 시간", form.time.orEmpty(), "예: 19:00~21:00") { form = form.copy(time = it) }
            JobChips("고용 형태", JobFormOptions.employments, setOfNotNull(form.employment)) {
                form = form.copy(employment = if (form.employment == it) null else it)
            }
            JobChips("급여", JobFormOptions.salaryRanges, setOfNotNull(form.salary)) {
                form = form.copy(salary = if (form.salary == it) null else it)
            }
            JobChips("요구 경력", JobFormOptions.careerLevels, (form.careerLevels ?: emptyList()).toSet()) { v ->
                val cur = form.careerLevels ?: emptyList()
                form = form.copy(careerLevels = if (cur.contains(v)) cur - v else cur + v)
            }
            JobField("마감일", form.deadline.orEmpty(), "yyyy-MM-dd (미정이면 -)") { form = form.copy(deadline = it) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("상세 설명", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MuyeonColors.textHead)
                OutlinedTextField(
                    value = form.description.orEmpty(), onValueChange = { form = form.copy(description = it) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JobButton("임시저장", filled = false, enabled = !saving, modifier = Modifier.weight(1f)) { save(true) }
            JobButton(if (saving) "저장 중…" else "공고 게시", filled = true, enabled = !saving, modifier = Modifier.weight(1f)) { save(false) }
        }
    }

    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

@Composable
private fun JobField(
    label: String,
    value: String,
    placeholder: String = "",
    required: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MuyeonColors.textHead)
            if (required) Text("*", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MuyeonColors.primary)
        }
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontFamily = customFontFamily, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun JobChips(
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

@Composable
private fun JobButton(text: String, filled: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp,
        color = if (filled) Color.White else MuyeonColors.primary, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(if (filled) MuyeonColors.primary else MuyeonColors.primary.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 16.dp),
    )
}

/** 웹 `openMyJobPostings` 브릿지 진입점. */
class JobPostingActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_ID = "id"

        fun startList(context: Context) = context.go(intent(context, "list"))
        fun startForm(context: Context, jobId: Int?) =
            context.go(intent(context, "form").putExtra(EXTRA_ID, jobId ?: 0))

        private fun intent(context: Context, route: String) =
            Intent(context, JobPostingActivity::class.java).putExtra(EXTRA_ROUTE, route)

        private fun Context.go(i: Intent) {
            if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "list"
        val id = intent.getIntExtra(EXTRA_ID, 0)

        setContent {
            val nav = rememberNavController()
            val api = remember { JobPostingApi(TokenManager.getAccessToken(this)) }

            fun back() { if (!nav.popBackStack()) finish() }

            NavHost(nav, startDestination = route) {
                composable("list") {
                    MyPostingsScreen(
                        api = api,
                        onClose = { finish() },
                        onEdit = { jid -> nav.navigate("form/$jid") },
                        onCreate = { nav.navigate("form/0") },
                        // 지원자 목록은 미이식 — 지원자 상세(C)로 바로 가려면 applicationId 가 필요해 웹 폴백.
                        onApplicants = {
                            com.muyeon.app.webview.NativeWebRoute.openWebAndFinish(this@JobPostingActivity, "/receivedApplications")
                        },
                    )
                }
                composable("form") {
                    JobPostingFormScreen(api, id.takeIf { it > 0 }, onClose = { back() }, onSaved = { back() })
                }
                composable("form/{id}") { e ->
                    val jid = e.arguments?.getString("id")?.toIntOrNull()?.takeIf { it > 0 }
                    JobPostingFormScreen(api, jid, onClose = { back() }, onSaved = { back() })
                }
            }
        }
    }
}
