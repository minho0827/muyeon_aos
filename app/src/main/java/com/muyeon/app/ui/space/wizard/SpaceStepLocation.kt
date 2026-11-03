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
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.jobposting.SectionHead
import com.muyeon.app.ui.quote.RegionPickRow
import kotlinx.coroutines.launch

/** ② 위치 — 주소 검색 · 상세 주소 · 지역(필수). */
@Composable
fun SpaceStepLocation(form: SpaceForm, token: String?, onSearch: () -> Unit, onChange: (SpaceForm) -> Unit) {
    SectionHead("위치", "주소를 검색하면 지도에 위치가 표시돼요.")
    AddressBlock(form.placeName, form.roadAddress, form.lat, form.lng, onSearch)
    FormField("상세 주소") { FormTextField(form.addressDetail, { onChange(form.copy(addressDetail = it)) }, "예) 지하 1층 B홀", 100) }
    FormField("지역", required = true, hint = "(목록 지역 필터에 쓰여요)") {
        RegionPickRow(token = token, placeholder = "지역을 선택하세요", name = form.region, allowSidoAll = false) { name, code ->
            onChange(form.copy(region = name, regionCode = code))
        }
    }
}
