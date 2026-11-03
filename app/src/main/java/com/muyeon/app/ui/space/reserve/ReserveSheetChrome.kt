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
import androidx.compose.material.icons.filled.Close
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
import com.muyeon.app.ui.space.SpaceDesign
import kotlinx.coroutines.launch

/** 시트 제목줄 — "예약하기" + ✕. */
@Composable
fun ReserveSheetTitle(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(SpaceDesign.headerHeight).padding(horizontal = SpaceDesign.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "예약하기", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            lineHeight = 22.sp, color = SpaceDesign.ink900, modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.Close, "닫기", tint = SpaceDesign.ink900, modifier = Modifier.size(16.dp).clickable(onClick = onClose))
    }
}

/** 오류 한 줄(빨강). */
@Composable
fun ReserveError(text: String) {
    Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFFE64747))
}

/** 하단 고정 [결제하기]. */
@Composable
fun ReservePayButton(submitting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpaceDesign.ink200))
        Text(
            if (submitting) "예약 중…" else "결제하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp,
            color = if (enabled) Color.White else SpaceDesign.ink100, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = SpaceDesign.gutter).padding(top = 12.dp, bottom = 20.dp)
                .fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp))
                .background(if (enabled) SpaceDesign.primary else SpaceDesign.ink300)
                .clickable(enabled = enabled, onClick = onClick)
                .wrapContentHeight(Alignment.CenterVertically),
        )
    }
}
