package com.muyeon.app.ui.space.reserve

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
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.space.SpaceConsentDoc
import com.muyeon.app.ui.space.SpaceDesign
import kotlinx.coroutines.launch

/**
 * 예약 동의 — 서버 bookingConsents 를 순서대로 "[필수] {title}" + "보기 ›"(url). 제목·본문은 관리자 '약관·정책'.
 *  맨 아래 회색 중개자 고지. 모두 체크해야 결제 버튼이 켜진다(판정은 시트).
 */
@Composable
fun BookingConsents(docs: List<SpaceConsentDoc>, checked: Set<String>, onToggle: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        docs.forEach { d -> BookingConsentRow(d, d.docKey in checked, { onToggle(d.docKey) }) { openPolicyPage(ctx, d.url) } }
        Text(
            "무용연은 통신판매중개자로 공간 대관 계약의 당사자가 아니에요.",
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = SpaceDesign.ink500,
            modifier = Modifier.padding(start = 32.dp),
        )
    }
}

@Composable
private fun BookingConsentRow(doc: SpaceConsentDoc, agree: Boolean, onToggle: () -> Unit, onView: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReserveCheckBox(agree)
            Text(
                "${if (doc.required) "[필수]" else "[선택]"} ${doc.title}",
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = SpaceDesign.ink900, modifier = Modifier.weight(1f),
            )
        }
        Text(
            "보기 ›", fontFamily = customFontFamily, fontSize = 13.sp, color = SpaceDesign.ink600,
            modifier = Modifier.clickable(onClick = onView).padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}
