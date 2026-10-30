package com.muyeon.app.webview

import android.app.Activity
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors
import org.json.JSONArray

/**
 * 2026-10-31 웹 하단 MY 탭 길게 누르기 → 내 유형 바텀시트(인스타그램 계정 전환 시트와 같은 구성, iOS 와 같은 모양).
 *  가진 유형 + 일반을 한 카드에 줄로 보이고 지금 유형에 체크, 맨 아래 '유형 추가'.
 *  고르기만 한다 — 전환(서버 저장·헤더 동기·전환 토스트)은 웹 AuthContext.setActiveType 이 한다.
 *  types = [{ type, label }] JSON 문자열, current = 지금 유형.
 *  결과: window.__onActiveTypePicked('TEACHER' | 'MANAGE'). 지금 유형·닫기는 알리지 않는다.
 */
object ActiveTypeSheet {
    fun show(activity: Activity, webView: WebView, typesJson: String, current: String) {
        val rows = runCatching {
            val arr = JSONArray(typesJson)
            (0 until arr.length()).map { arr.getJSONObject(it) }
                .map { it.optString("type") to it.optString("label") }
                .filter { it.first.isNotEmpty() }
        }.getOrNull().orEmpty()
        if (rows.isEmpty()) return
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val host = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        var closed = false
        val finish: (String?) -> Unit = { picked ->
            host.post {
                if (!closed) {
                    closed = true
                    root.removeView(host)
                    picked?.let { p ->
                        // ["TEACHER"] 배열로 넘겨 따옴표·특수문자 걱정 없이 전달한다.
                        val arg = JSONArray().put(p).toString()
                        webView.evaluateJavascript("window.__onActiveTypePicked && window.__onActiveTypePicked($arg[0])", null)
                    }
                }
            }
        }
        host.setContent {
            Sheet(rows, current, onPick = { t -> finish(if (t == current) null else t) }, onDismiss = { finish(null) })
        }
        root.addView(host, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun iconOf(type: String): ImageVector = when (type) {
        "TEACHER" -> Icons.Filled.School
        "DANCER" -> Icons.Filled.DirectionsRun
        "ACADEMY" -> Icons.Filled.Apartment
        "TEAM" -> Icons.Filled.TheaterComedy
        else -> Icons.Filled.Person
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Sheet(rows: List<Pair<String, String>>, current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Color.White) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(16.dp)),
            ) {
                rows.forEachIndexed { i, (type, label) ->
                    TypeRow(label, iconOf(type), filled = type == current, checked = type == current) { onPick(type) }
                    if (i < rows.size - 1) HorizontalDivider(Modifier.padding(start = 74.dp), color = MuyeonColors.border)
                }
                HorizontalDivider(Modifier.padding(start = 74.dp), color = MuyeonColors.border)
                TypeRow("유형 추가", Icons.Filled.Add, filled = false, checked = false) { onPick("MANAGE") }
            }
        }
    }

    @Composable
    private fun TypeRow(label: String, icon: ImageVector, filled: Boolean, checked: Boolean, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(if (filled) MuyeonColors.primary else MuyeonColors.border),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (filled) Color.White else MuyeonColors.textHead, modifier = Modifier.size(22.dp))
            }
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MuyeonColors.textHead)
            Spacer(Modifier.weight(1f))
            if (checked) Icon(Icons.Filled.CheckCircle, contentDescription = "지금 유형", tint = MuyeonColors.primary, modifier = Modifier.size(24.dp))
        }
    }
}
