package com.muyeon.app.ui.chat.dealbar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-14 채팅방 헤더 전화 버튼(당근 헤더 ☎, iOS ChatHeaderCallButton 1:1).
 *  연락처가 공개된 방(채택·일정 확정·예약 확정)에서만 보인다 — 누르면 전화 앱이 번호를 채운 채 열린다.
 */
@Composable
fun ChatHeaderCallButton(phone: String) {
    val context = LocalContext.current
    Box(
        Modifier
            .size(44.dp)
            .clickable { dialPhone(context, phone) }
            .semantics { contentDescription = "전화하기" }
            .testTag("chat.call"),
        Alignment.Center,
    ) {
        Icon(Icons.Outlined.Call, null, tint = MuyeonColors.textHead, modifier = Modifier.size(22.dp))
    }
}

/** 전화 앱을 번호가 채워진 채로 연다(바로 걸지 않는다 — 권한 불필요). */
fun dialPhone(context: android.content.Context, phone: String) {
    val digits = phone.filter { it.isDigit() || it == '+' }
    if (digits.isEmpty()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))) }
}
