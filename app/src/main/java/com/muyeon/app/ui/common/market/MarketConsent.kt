package com.muyeon.app.ui.common.market

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
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
import com.muyeon.app.utils.Constants
import kotlinx.coroutines.launch

/**
 * 약관(정책) 열기 — 앱 약관 화면(PolicyActivity). 지금 화면·체크 상태가 그대로 남고 뒤로 가면 돌아온다.
 *  path 는 서버가 주는 "/policy?doc=…&embed=1" 그대로. 약관 주소가 아닐 때만 외부 브라우저.
 */
fun openPolicyPage(context: Context, path: String) {
    if (com.muyeon.app.ui.policy.PolicyActivity.startPath(context, path)) return
    val base = Constants.getBaseUrl(context).trimEnd('/')
    val url = if (path.startsWith("http")) path else base + (if (path.startsWith("/")) path else "/$path")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "약관을 열 수 있는 브라우저가 없어요.", Toast.LENGTH_SHORT).show()
    }
}

/** 주황 체크 상자(22dp, r6) — 환불 규정 동의와 같은 모양. */
@Composable
fun MarketCheckBox(checked: Boolean) {
    Box(
        Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
            .background(if (checked) MuyeonColors.primary else Color.White)
            .border(1.dp, if (checked) MuyeonColors.primary else MuyeonList.ink300, RoundedCornerShape(6.dp)),
        Alignment.Center,
    ) { if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
}

/** 동의 줄 — 체크 상자 + 글. */
@Composable
fun ConsentCheckRow(checked: Boolean, label: String, bold: Boolean = false, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarketCheckBox(checked)
        Text(
            label, fontFamily = customFontFamily, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (bold) 16.sp else 14.sp, lineHeight = 20.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
        )
    }
}

/** 약관 동의 줄 — 체크 상자 + 글 + 오른쪽 "보기 ›"(약관 페이지). */
@Composable
fun ConsentDocRow(checked: Boolean, label: String, onToggle: () -> Unit, onView: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { ConsentCheckRow(checked, label, onToggle = onToggle) }
        Text(
            "보기 ›", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
            modifier = Modifier.clickable(onClick = onView).padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}
