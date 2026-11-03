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
import com.muyeon.app.ui.lesson.LessonPlace
import com.muyeon.app.ui.transfer.TransferForm
import kotlinx.coroutines.launch

/** 단계 제목 — ① 사진 ② 위치 ③ 시설·면적 ④ 금액 ⑤ 옵션 ⑥ 소개 ⑦ 연락 → 미리보기. */
val TRANSFER_STEPS = listOf("사진", "위치", "시설·면적", "금액", "옵션", "소개", "연락", "미리보기")
const val TRANSFER_MAX_IMAGES = 10

/** 위저드 상태 — 폼·단계·진행 표시. 화면 밖 로직(검증·다음 버튼 문구)을 여기 모은다. */
class TransferWizardState(val editId: Int?) {
    var form by mutableStateOf(TransferForm())
    var baseline by mutableStateOf(TransferForm())
    var step by mutableIntStateOf(0)
    var loading by mutableStateOf(editId != null)
    var saving by mutableStateOf(false)
    var uploading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val dirty: Boolean get() = form != baseline
    val isLast: Boolean get() = step == TRANSFER_STEPS.lastIndex
    val canNext: Boolean get() = when (step) {
        2 -> form.title.isNotBlank()
        else -> !uploading
    }
    val nextLabel: String
        get() = if (isLast) (if (editId != null) "수정 완료" else "등록") else if (step == TRANSFER_STEPS.lastIndex - 1) "미리보기" else "다음"
    val title: String get() = if (editId != null) "양도 글 수정" else "양도 글 등록"

    fun update(block: TransferForm.() -> TransferForm) { form = form.block() }

    /** 장소 선택 → 도로명·좌표·지역 이름(앞 두 단어). 지역 코드는 코드표 매칭으로 따로 채운다. */
    fun applyPlace(p: LessonPlace) {
        val addr = p.address ?: p.name
        form = form.copy(roadAddress = addr, placeName = p.name, lat = p.lat, lng = p.lng, region = MarketRegion.regionName(addr), regionCode = null)
    }
}
