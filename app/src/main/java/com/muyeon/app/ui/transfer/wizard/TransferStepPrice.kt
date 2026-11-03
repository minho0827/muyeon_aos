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
import kotlinx.coroutines.launch

/** ④ 금액(만원) — 보증금·월세·관리비·권리금 · 권리금 협의 · 부가세 별도 · 시간당 대관(원). */
@Composable
fun TransferStepPrice(form: TransferForm, onChange: (TransferForm) -> Unit) {
    SectionHead("금액", "금액은 만원 단위예요. 비워 두면 '협의'로 보여요.")
    FormField("보증금", hint = "(만원)") { NumberField(form.deposit, "예) 1800") { onChange(form.copy(deposit = it)) } }
    FormField("월세", hint = "(만원)") { NumberField(form.monthlyRent, "예) 140") { onChange(form.copy(monthlyRent = it)) } }
    FormField("관리비", hint = "(만원)") { NumberField(form.maintenanceFee, "예) 15") { onChange(form.copy(maintenanceFee = it)) } }
    FormField("권리금", hint = "(만원)") { NumberField(form.premium, "예) 1000") { onChange(form.copy(premium = it)) } }
    FormToggle("권리금 협의가능", form.premiumNegotiable) { onChange(form.copy(premiumNegotiable = it)) }
    FormToggle("부가세 별도", form.vatSeparate) { onChange(form.copy(vatSeparate = it)) }
    FormField("시간당 대관", hint = "(원, 선택)") { NumberField(form.hourlyPrice, "예) 15000") { onChange(form.copy(hourlyPrice = it)) } }
}
