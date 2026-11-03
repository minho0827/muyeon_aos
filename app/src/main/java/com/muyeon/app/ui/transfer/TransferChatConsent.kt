package com.muyeon.app.ui.transfer

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
import kotlinx.coroutines.launch

/**
 * 문의 채팅 전 양도양수 이용약관 동의(7087ee5) — 목록 종이비행기·상세 [채팅하기]·로그인 후 이어서(then=chat) 모두 이 길을 탄다.
 *  · 이번 실행에서 한 번 동의(또는 서버가 agreed)했으면 메모리 캐시로 바로 연다.
 *  · 아니면 GET consent → agreed=false 면 시트([TransferChatConsentSheet]) → [동의하기] = POST consent{source:CHAT} → 바로 연다.
 *  · 채팅 열기가 403 TRANSFER_CONSENT_REQUIRED 면 같은 시트(onRequired).
 *  · 확인 자체가 실패하면(배포 전 서버 등) 그냥 연다 — 서버가 403 으로 다시 막으면 시트가 뜬다.
 */
class TransferChatConsent(private val api: TransferApi) {
    var sheetDocs by mutableStateOf<List<ConsentDocItem>?>(null)
        private set
    var submitting by mutableStateOf(false)
        private set
    private var pending: (() -> Unit)? = null

    suspend fun ensure(open: () -> Unit) {
        if (agreedThisRun) { open(); return }
        api.consent()
            .onSuccess { c -> if (c.agreed) { agreedThisRun = true; open() } else show(c.docs, open) }
            .onFailure { open() }
    }

    /** 서버가 채팅 열기를 동의 필요로 막음 — 문서를 다시 읽어 시트. */
    suspend fun onRequired(open: () -> Unit) {
        agreedThisRun = false
        api.consent().onSuccess { show(it.docs, open) }.onFailure { show(emptyList(), open) }
    }

    private fun show(docs: List<ConsentDocItem>, open: () -> Unit) { pending = open; sheetDocs = docs }

    fun dismiss() { sheetDocs = null; pending = null }

    /** [동의하기] — 저장되면 시트를 닫고 하려던 채팅을 바로 연다. 실패하면 서버 문구. */
    suspend fun agree(): String? {
        submitting = true
        val r = api.agreeConsent("CHAT")
        submitting = false
        return r.fold(
            onSuccess = { agreedThisRun = true; val open = pending; dismiss(); open?.invoke(); null },
            onFailure = { it.message ?: "동의를 저장하지 못했어요." },
        )
    }

    companion object {
        /** 이번 실행에서 동의 완료(메모리 캐시). */
        @Volatile var agreedThisRun = false
    }
}

/** 문의 채팅 동의 시트 — 뜰 때만 그린다(시트 안 내용은 받은 문서로 고정). */
@Composable
fun TransferChatConsentSheet(gate: TransferChatConsent, onError: (String) -> Unit) {
    val docs = gate.sheetDocs ?: return
    val scope = rememberCoroutineScope()
    MarketConsentSheet(
        title = "무용센터 양도양수 이용약관 동의가 필요해요",
        docs = docs, submitting = gate.submitting,
        onAgree = { scope.launch { gate.agree()?.let(onError) } },
        onDismiss = { gate.dismiss() },
    )
}
