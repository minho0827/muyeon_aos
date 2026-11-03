package com.muyeon.app.ui.space.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.jobposting.SectionHead
import com.muyeon.app.ui.space.SpaceConsentDoc
import kotlinx.coroutines.launch

/** 서버 약관 외에 앱이 받는 필수 확인(서버 문서 아님). */
const val SPACE_HOST_RIGHTS_CONFIRM = "[필수] 실제로 존재하고 제가 대관할 권리(소유·임대차 등)가 있는 공간만 등록할게요"

/**
 * 호스트 이용 동의 단계(첫 공간 등록 전, 동의 안 했을 때만 위저드가 부른다).
 *  "전체 동의" · 서버 약관 줄마다 "[필수] {제목}" + 보기 › · 대관 권리 확인 줄 → [동의하고 시작](모두 체크 시).
 */
@Composable
fun ColumnScope.SpaceStepHostConsent(docs: List<SpaceConsentDoc>, submitting: Boolean, onAgree: () -> Unit) {
    val ctx = LocalContext.current
    var checkedDocs by remember(docs) { mutableStateOf<Set<String>>(emptySet()) }
    var rights by remember { mutableStateOf(false) }
    val allChecked = rights && docs.all { it.docKey in checkedDocs }

    fun setAll(on: Boolean) {
        checkedDocs = if (on) docs.map { it.docKey }.toSet() else emptySet()
        rights = on
    }

    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHead("공간을 등록하기 전에 확인해 주세요", "아래 내용에 모두 동의해야 공간을 등록할 수 있어요.")
        ConsentCheckRow(allChecked, "전체 동의", bold = true) { setAll(!allChecked) }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))
        docs.forEach { d ->
            ConsentDocRow(
                checked = d.docKey in checkedDocs, label = "[필수] ${d.title}",
                onToggle = { checkedDocs = if (d.docKey in checkedDocs) checkedDocs - d.docKey else checkedDocs + d.docKey },
                onView = { openPolicyPage(ctx, d.url) },
            )
        }
        ConsentCheckRow(rights, SPACE_HOST_RIGHTS_CONFIRM) { rights = !rights }
    }
    Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        StageBottomButton("동의하고 시작", enabled = allChecked, busy = submitting, onClick = onAgree)
    }
}
