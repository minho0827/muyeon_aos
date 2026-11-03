package com.muyeon.app.ui.space.reserve

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
import com.muyeon.app.ui.space.SpaceAvailability
import com.muyeon.app.ui.space.hourLabel
import kotlinx.coroutines.launch

/** 이용 시간 — minHours 부터 0.5시간 단위, 막힌 구간과 겹치면 흐리게. 시작 시간 전이면 안내. */
@Composable
fun DurationChips(a: SpaceAvailability, minHours: Double, start: Int?, hours: Double, onPick: (Double) -> Unit) {
    FieldGroup("이용 시간") {
        val choices = generateSequence(minHours) { it + 0.5 }.takeWhile { it <= 24.0 && (start ?: a.start) + it * 60 <= a.end }.toList()
        ChipFlow(choices.map { h -> Triple(hourLabel(h), hours == h, start == null || a.fits(start, h)) }) { i -> onPick(choices[i]) }
        if (start == null) Caption("시작 시간을 먼저 골라 주세요.")
    }
}
