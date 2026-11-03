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
import com.muyeon.app.ui.lesson.LessonPlace
import kotlinx.coroutines.launch

/** 공간 위저드 상태 — 폼·단계·진행 표시와 검증. */
class SpaceWizardState(val editId: Int?) {
    var form by mutableStateOf(SpaceForm())
    var baseline by mutableStateOf(SpaceForm())
    var step by mutableIntStateOf(0)
    var loading by mutableStateOf(editId != null)
    var saving by mutableStateOf(false)
    var uploading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val dirty: Boolean get() = form != baseline
    val isLast: Boolean get() = step == SPACE_STEPS.lastIndex
    val title: String get() = if (editId != null) "공간 수정" else "공간 등록"
    val nextLabel: String get() = if (isLast) (if (editId != null) "수정 완료" else "등록") else "다음"
    val canNext: Boolean get() = when (step) {
        0 -> !uploading
        1 -> form.region.isNotBlank()
        2 -> form.name.isNotBlank()
        3 -> (form.pricePerHour.toIntOrNull() ?: 0) > 0 || form.options.any { it.label.isNotBlank() && (it.price.toIntOrNull() ?: 0) > 0 }
        4 -> form.openDays.isNotEmpty() && form.closeHour > form.openHour
        else -> true
    }

    fun update(block: SpaceForm.() -> SpaceForm) { form = form.block() }

    /** 저장 직전 폼 — 세부공간만 적고 기본 시간당을 비우면 가장 낮은 세부공간 가격을 기본값으로(서버 필수 칸). */
    fun formForSave(): SpaceForm = if ((form.pricePerHour.toIntOrNull() ?: 0) <= 0) {
        form.copy(pricePerHour = form.options.mapNotNull { it.price.toIntOrNull() }.filter { it > 0 }.minOrNull()?.toString() ?: "0")
    } else form

    fun applyPlace(p: LessonPlace) {
        val addr = p.address ?: p.name
        form = form.copy(roadAddress = addr, placeName = p.name, lat = p.lat, lng = p.lng, region = MarketRegion.regionName(addr), regionCode = null)
    }
}
