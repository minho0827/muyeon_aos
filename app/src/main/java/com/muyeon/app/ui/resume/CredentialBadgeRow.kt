package com.muyeon.app.ui.resume

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.boolOrNull
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.stringOrNull
import org.json.JSONObject

/**
 * ★ 2026-10-12 본인 인증·전문 정보 확인 배지(전공·경력·자격·지도경력) — iOS `CredentialBadgeRow` 1:1.
 *  서버 badges = { identity, items: [{ kind, label, desc, title, years }], careerYears }
 *  '인증 강사·실력 보장' 같은 표현은 쓰지 않는다 — 무엇을 확인했는지만 보여 준다.
 */
data class CredentialBadges(
    val identity: Boolean,
    val items: List<Item>,
    val careerYears: Int?,
) {
    data class Item(val kind: String, val label: String, val desc: String?, val title: String?, val years: Int?)

    /** 표시 순서: 본인 인증 → 전공·경력·자격·지도경력 */
    val all: List<Item>
        get() = (if (identity) listOf(Item("IDENTITY", "본인인증", "휴대폰 본인인증으로 실명을 확인한 회원이에요.", null, null)) else emptyList()) + items

    companion object {
        fun from(o: JSONObject?): CredentialBadges? {
            if (o == null) return null
            val arr = o.optJSONArray("items")
            val items = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
                val it = arr?.optJSONObject(i) ?: return@mapNotNull null
                val kind = it.stringOrNull("kind") ?: return@mapNotNull null
                Item(kind, it.stringOrNull("label") ?: kind, it.stringOrNull("desc"), it.stringOrNull("title"), it.intOrNull("years"))
            }
            return CredentialBadges(o.boolOrNull("identity") == true, items, o.intOrNull("careerYears"))
        }
    }
}

@Composable
fun CredentialBadgeRow(badges: CredentialBadges?, modifier: Modifier = Modifier) {
    val list = badges?.all.orEmpty()
    if (list.isEmpty()) return
    var shown by remember { mutableStateOf<CredentialBadges.Item?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            list.forEach { b ->
                val strong = b.kind != "IDENTITY"
                Text(
                    "✓ ${b.label}",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = if (strong) MuyeonColors.primary else MuyeonColors.textSub,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (strong) MuyeonColors.primary.copy(alpha = 0.08f) else Color.White)
                        .border(1.dp, if (strong) MuyeonColors.primary else MuyeonColors.border, RoundedCornerShape(4.dp))
                        .clickable { shown = b }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        badges?.careerYears?.takeIf { it > 0 }?.let {
            Text("경력 ${it}년 확인", fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.textSub)
        }
    }
    shown?.let { b ->
        AlertDialog(
            onDismissRequest = { shown = null },
            confirmButton = { TextButton(onClick = { shown = null }) { Text("확인") } },
            title = { Text(b.label, fontFamily = customFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    listOfNotNull(b.desc, listOfNotNull(b.title, b.years?.let { "${it}년" }).joinToString(" · ").takeIf { it.isNotEmpty() }).joinToString("\n"),
                    fontFamily = customFontFamily,
                )
            },
        )
    }
}
