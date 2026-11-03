package com.muyeon.app.ui.common.market

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
import kotlinx.coroutines.launch

/** 동의 문서 한 건(서버가 준 키·제목·웹 경로) — 화면은 제목만 쓴다. */
data class ConsentDocItem(val key: String, val title: String, val url: String)

/**
 * 위저드 앞 동의 단계(공간 호스트·양도양수 글쓰기 공용) — 제목 · "전체 동의" · 서버 문서마다 "[필수] {제목}" + 보기 ›
 *  → [동의하고 시작](모두 체크 시). 항목·제목은 서버(관리자 '약관·정책')가 준 것만 그린다.
 */
@Composable
fun ColumnScope.MarketConsentStep(title: String, subtitle: String, docs: List<ConsentDocItem>, submitting: Boolean, onAgree: () -> Unit) {
    val ctx = LocalContext.current
    var checkedDocs by remember(docs) { mutableStateOf<Set<String>>(emptySet()) }
    val allChecked = docs.all { it.key in checkedDocs }

    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHead(title, subtitle)
        ConsentCheckRow(allChecked, "전체 동의", bold = true) { checkedDocs = if (!allChecked) docs.map { it.key }.toSet() else emptySet() }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))
        docs.forEach { d ->
            ConsentDocRow(
                checked = d.key in checkedDocs, label = "[필수] ${d.title}",
                onToggle = { checkedDocs = if (d.key in checkedDocs) checkedDocs - d.key else checkedDocs + d.key },
                onView = { openPolicyPage(ctx, d.url) },
            )
        }
    }
    Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        StageBottomButton("동의하고 시작", enabled = allChecked, busy = submitting, onClick = onAgree)
    }
}

/**
 * 동의가 필요할 때 띄우는 고정 높이 시트(양도양수 문의 채팅 등) — 제목 · 문서마다 "[필수] {제목} ›"(누르면 약관) · [동의하기].
 *  시트를 닫으면(onDismiss) 아무것도 하지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketConsentSheet(title: String, docs: List<ConsentDocItem>, submitting: Boolean, onAgree: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead, maxLines = 2)
            docs.forEach { d -> ConsentLinkRow("[필수] ${d.title}") { openPolicyPage(ctx, d.url) } }
            Box(Modifier.padding(top = 4.dp, bottom = 16.dp)) { StageBottomButton("동의하기", busy = submitting, onClick = onAgree) }
        }
    }
}

/** "[필수] {제목} ›" — 누르면 약관 페이지. */
@Composable
fun ConsentLinkRow(label: String, onView: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MuyeonList.ink100).clickable(onClick = onView).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("›", fontFamily = customFontFamily, fontSize = 18.sp, color = MuyeonColors.textSub)
    }
}
