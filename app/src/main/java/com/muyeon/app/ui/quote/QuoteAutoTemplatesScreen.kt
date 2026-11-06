package com.muyeon.app.ui.quote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.muyeon.app.result.OnChildResult
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.result.resultKeys
import com.muyeon.app.result.resultPayload
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.common.MuyeonEmptyState
import com.muyeon.app.ui.common.MuyeonHeaderIcon
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.common.MuyeonListHeader
import com.muyeon.app.ui.membership.MembershipActivity
import com.muyeon.app.ui.resume.ResumeActivity
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute

/**
 * 견적 자동응답 — iOS `QuoteAutoTemplatesView.swift`·웹 quoteAuto 와 같은 구성(★ 2026-11-06 개편).
 *  맨 위 '자동 견적 발송' 스위치(+이번 달 사용량) → 안 나가는 이유 → 내 자동응답 메시지(라디오 선택).
 *  바뀐 건 그 자리에서 서버에 저장된다('완료' 버튼 없음). 웹 `openAutoQuoteTemplates` 브릿지 / 개인레슨 관리의 '자동응답'으로 진입.
 *  나눈 파일: AutoQuoteApi · AutoQuoteSettingsState · AutoQuoteStatusSection · AutoQuoteBlockerList ·
 *   AutoQuoteTemplateList · AutoQuoteEditorSheet · AutoQuotePreviewSheet.
 */
class QuoteAutoTemplatesActivity : com.muyeon.app.result.ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.AUTO_QUOTE)

    companion object {
        /** ★ 2026-11-06 true 면 열자마자 '새 메시지' 편집을 띄운다(자동견적 발송 내역 [+]). */
        const val EXTRA_START_NEW = "autoQuote.startNew"

        fun intent(context: Context, startNew: Boolean = false): Intent =
            Intent(context, QuoteAutoTemplatesActivity::class.java)
                .apply { if (startNew) putExtra(EXTRA_START_NEW, true) }

        fun start(context: Context) = context.launchScreen(intent(context))

        fun start(context: Context, startNew: Boolean) = context.launchScreen(intent(context, startNew))
    }

    /** 새 메시지 편집을 열라는 신호 — 값이 바뀔 때마다 한 번 연다(0 = 안 연다). */
    private var newEditorSignal by mutableIntStateOf(0)

    /** ★ 2026-11-06 발송 내역에서 돌아오면 이번 달 사용량을 다시 읽는다. */
    private var reloadSignal by mutableIntStateOf(0)

    /**
     * ★ 2026-11-06 '발송 내역 보기'는 전용 런처로 띄운다 — 내역의 [+]가 START_NEW 플래그를 들고 닫히면
     *  자동응답 화면을 하나 더 쌓지 않고 여기서 새 메시지 편집을 연다. 결과 키는 childLauncher 와 똑같이 합친다.
     */
    private val historyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val keys = r.data.resultKeys()
            if (keys.isNotEmpty()) absorbChildResult(keys, r.data.resultPayload())
            if (ResultKeys.LOGIN_REQUIRED in keys) { finish(); return@registerForActivityResult }
            reloadSignal++
            if (r.data?.getBooleanExtra(EXTRA_START_NEW, false) == true) newEditorSignal++
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 자동 견적 템플릿은 강사·학원 전용 — 무용수 유형이면 안내 후 종료(2026-09-26 정책).
        if (!ActiveRole.allowLessonProvider(this)) { finish(); return }
        // 처음 열릴 때만(회전 등 재생성 땐 다시 안 연다).
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_START_NEW, false)) newEditorSignal = 1
        setContent {
            val api = remember { AutoQuoteApi(TokenManager.getAccessToken(this)) }
            QuoteAutoTemplatesScreen(
                api = api,
                // 닫기 = 결과(AUTO_QUOTE)를 싣고 부모로 돌아간다(iOS presentAutoQuoteTemplates 닫힘 콜백 대응).
                //  ★ 예전 notifyWebAndFinish 는 CLEAR_TOP 이라 네이티브 부모(개인레슨 관리)까지 죽였다.
                onClose = { finish() },
                newEditorSignal = newEditorSignal,
                reloadSignal = reloadSignal,
                onOpenHistory = {
                    historyLauncher.launch(
                        AutoQuoteHistoryActivity.intent(this, fromTemplates = true)
                            .putExtra(ResultKeys.EXTRA_FROM_NATIVE, true),
                    )
                },
                onOpenNative = ::openNative,
                // ★ 2026-11-06 레슨 설정·장르처럼 웹에만 있는 화면 — 이 화면을 닫고 웹뷰가 그 경로로 간다(QuoteDashboardActivity 와 같은 길).
                onOpenWebPath = { path -> NativeWebRoute.openWebAndFinish(this, path) },
            )
        }
    }

    /** ★ 2026-11-06 막힌 이유의 앱 화면 열기 — 모르는 이름이면 false(웹 경로로 넘긴다). */
    private fun openNative(name: String): Boolean {
        when (name) {
            "openMembership" -> MembershipActivity.start(this)
            "openFieldVisibility" -> ResumeActivity.startVisibility(this, "teacher")
            else -> return false
        }
        return true
    }
}

@Composable
fun QuoteAutoTemplatesScreen(
    api: AutoQuoteApi,
    onClose: () -> Unit,
    /** ★ 2026-11-06 값이 바뀌면(>0) '새 메시지 추가'와 같은 빈 편집을 연다. */
    newEditorSignal: Int = 0,
    /** ★ 2026-11-06 값이 바뀌면 상태·목록을 다시 읽는다(발송 내역에서 복귀 등). */
    reloadSignal: Int = 0,
    onOpenHistory: (() -> Unit)? = null,
    /** 막힌 이유의 nativeAction — 열었으면 true. */
    onOpenNative: (String) -> Boolean = { false },
    /** 막힌 이유의 webPath — 이 화면을 닫고 웹뷰를 그 경로로. */
    onOpenWebPath: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val vm = rememberAutoQuoteSettingsState(api) { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    val openHistory: () -> Unit = { onOpenHistory?.invoke() ?: AutoQuoteHistoryActivity.start(context) }
    var previewing by remember { mutableStateOf<AutoQuoteEditorState?>(null) }

    LaunchedEffect(Unit) { vm.reload(showSpinner = true) }
    LaunchedEffect(reloadSignal) { if (reloadSignal > 0) vm.reload() }
    LaunchedEffect(newEditorSignal) { if (newEditorSignal > 0) vm.openNew() }
    // 멤버십·공개 설정 화면에서 돌아오면 막힌 이유가 풀렸을 수 있다 — 결과 키로 다시 읽는다.
    OnChildResult { vm.reload() }

    val onBlocker: (AutoQuoteBlocker) -> Unit = onBlocker@{ b ->
        if (b.code == "NO_TEMPLATE") { vm.openNew(); return@onBlocker }
        val a = b.action ?: return@onBlocker
        if (a.nativeAction != null && onOpenNative(a.nativeAction)) return@onBlocker
        a.webPath?.let { onOpenWebPath?.invoke(it) }
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("견적 자동응답", onClose = onClose) {
            MuyeonHeaderIcon(Icons.Outlined.History, "발송 내역", onClick = openHistory)
        }

        when (val load = vm.load) {
            AutoQuoteLoad.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            AutoQuoteLoad.Failed -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                MuyeonEmptyState(
                    title = "불러오지 못했어요",
                    message = "네트워크 상태를 확인하고 다시 시도해 주세요.",
                    icon = Icons.Outlined.ErrorOutline,
                    actionTitle = "다시 시도",
                    onAction = { vm.reload(showSpinner = true) },
                )
            }
            is AutoQuoteLoad.Ready -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = MuyeonList.gutter).padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                AutoQuoteStatusSection(
                    status = load.status,
                    switchOn = vm.switchOn,
                    onToggle = vm::toggle,
                    onOpenHistory = openHistory,
                )
                val blockers = visibleAutoQuoteBlockers(load.status.blockers)
                if (blockers.isNotEmpty()) AutoQuoteBlockerList(blockers, onBlocker)
                AutoQuoteTemplateList(
                    templates = load.templates,
                    activeId = load.status.activeId,
                    onAdd = vm::openNew,
                    onActivate = vm::activate,
                    onPreview = { t -> previewing = AutoQuoteEditorState(t.id, t.title.orEmpty(), t.message, t.price.orEmpty()) },
                    onEdit = vm::openEdit,
                    onDelete = { vm.confirmDelete = it },
                )
            }
        }
    }

    vm.editing?.let { st ->
        AutoQuoteEditorSheet(
            state = st,
            busy = vm.busy,
            onSave = { title, message, price -> vm.save(st, title, message, price) },
            onCancel = { vm.editing = null },
            onPreview = { title, message, price -> previewing = AutoQuoteEditorState(st.id, title, message, price) },
        )
    }

    // ★ 2026-11-06 미리보기 — 목록 카드(저장된 값)·편집 시트(저장 전 값) 둘 다 여기로. 편집 시트 위에 뜬다.
    previewing?.let { p -> AutoQuotePreviewSheet(value = p, onDismiss = { previewing = null }) }

    vm.confirmDelete?.let { t ->
        val active = (vm.load as? AutoQuoteLoad.Ready)?.status?.activeId == t.id
        MuyeonDialog(
            title = "이 메시지를 삭제할까요?",
            message = if (active) "지금 자동 견적에 쓰는 메시지예요. 삭제하면 다음 메시지로 바뀌고, 남은 메시지가 없으면 자동 견적이 꺼져요."
            else "삭제한 메시지는 되돌릴 수 없어요.",
            actions = listOf(
                MuyeonDialogAction("삭제", MuyeonDialogKind.DESTRUCTIVE) { vm.delete(t) },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { vm.confirmDelete = null },
        )
    }
}
