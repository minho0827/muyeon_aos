package com.muyeon.app.ui.transfer.wizard

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
import com.muyeon.app.ui.jobposting.SectionHead
import com.muyeon.app.ui.transfer.TransferForm
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

/** ⑦ 연락 — 담당자명 · 전화번호 · 회원에게 전화번호 공개 · 매물과의 관계(필수). */
@Composable
fun TransferStepContact(form: TransferForm, onChange: (TransferForm) -> Unit) {
    SectionHead("연락", "문의는 채팅으로 받아요. 전화번호는 공개를 켠 경우에만 회원에게 보여요.")
    FormField("담당자명") { FormTextField(form.contactName, { onChange(form.copy(contactName = it)) }, "예) 김원장", 30) }
    FormField("전화번호") { FormTextField(form.contactPhone, { v -> onChange(form.copy(contactPhone = v.filter { it.isDigit() || it == '-' })) }, "예) 010-1234-5678", 20, keyboardType = KeyboardType.Phone) }
    FormToggle("회원에게 전화번호 공개", form.phoneVisible) { onChange(form.copy(phoneVisible = it)) }
    // 매물과의 관계(필수) — 예전 글(null)은 선택 안 됨으로 열려 [다음]이 꺼진 채 고르게 한다.
    FormField("매물과의 관계", required = true) {
        FormChipWrap(TransferLabels.ownerRelation, { form.ownerRelation == it }) { onChange(form.copy(ownerRelation = it)) }
    }
}
