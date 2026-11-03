package com.muyeon.app.ui.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.space.*
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * 환불 규정 박스(요금 카드 안) — 서버 refund 그대로: 위 "환불 단계: {presetLabel}", 줄마다 "when · refund",
 *  아래 freeCancelNote, "무용연 취소·환불 정책 보기 ›"(policyUrl). 앱은 글자를 만들지 않는다.
 */
@Composable
internal fun RefundBox(refund: SpaceRefundView) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SpaceDesign.primaryTint)
            .border(1.dp, SpaceDesign.primaryTintLine, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "환불 규정",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                lineHeight = 19.sp, color = SpaceDesign.ink800, modifier = Modifier.weight(1f),
            )
            refund.presetLabel?.let { RefundPresetLabel(it) }
        }
        refund.rows.forEach { DetailRow(SpaceFeeRow(null, it.whenText, it.refundText), color = SpaceDesign.ink700) }
        refund.freeCancelNote?.let { RefundNote(it) }
        RefundPolicyLink { openPolicyPage(ctx, refund.policyUrl) }
    }
}

@Composable
private fun RefundPresetLabel(label: String) {
    Text(
        "환불 단계: $label",
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
        lineHeight = 15.sp, color = SpaceDesign.ink900,
    )
}

/** 결제 직후 무료 취소 안내(서버 문구). */
@Composable
internal fun RefundNote(text: String) {
    Text(text, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = SpaceDesign.ink600)
}

/** "무용연 취소·환불 정책 보기 ›". */
@Composable
internal fun RefundPolicyLink(onClick: () -> Unit) {
    Text(
        "무용연 취소·환불 정책 보기 ›",
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = SpaceDesign.primaryText,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 2.dp),
    )
}
