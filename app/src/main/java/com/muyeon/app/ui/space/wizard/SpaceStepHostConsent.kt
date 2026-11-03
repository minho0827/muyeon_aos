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
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.jobposting.SectionHead
import com.muyeon.app.ui.space.SpaceConsentDoc
import kotlinx.coroutines.launch

/**
 * 호스트 이용 동의 단계(첫 공간 등록 전, 동의 안 했을 때만 위저드가 부른다).
 *  "전체 동의" · 서버 약관 줄마다 "[필수] {제목}" + 보기 › → [동의하고 시작](모두 체크 시).
 *  항목·제목은 서버(관리자 '약관·정책')가 준 것만 그린다 — 대관 권리 확인은 호스트 이용약관 본문에 있다.
 */
@Composable
fun ColumnScope.SpaceStepHostConsent(docs: List<SpaceConsentDoc>, submitting: Boolean, onAgree: () -> Unit) =
    MarketConsentStep(
        "공간을 등록하기 전에 확인해 주세요", "아래 내용에 모두 동의해야 공간을 등록할 수 있어요.",
        docs.map { ConsentDocItem(it.docKey, it.title, it.url) }, submitting, onAgree,
    )
