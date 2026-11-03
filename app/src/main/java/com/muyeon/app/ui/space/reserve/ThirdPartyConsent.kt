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
import com.muyeon.app.ui.space.SPACE_BOOKING_THIRD_PARTY_POLICY
import com.muyeon.app.ui.space.SpaceDesign
import kotlinx.coroutines.launch

/**
 * 예약 정보 호스트 제공 동의(필수) — 체크 + "보기 ›"(약관) + 아래 회색 중개자 고지.
 *  환불 규정 동의와 같은 체크 상자.
 */
@Composable
fun ThirdPartyConsent(agree: Boolean, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReserveCheckBox(agree)
                Text(
                    "[필수] 예약 정보(이름·일시·인원·요청사항)를 호스트에게 제공하는 데 동의해요",
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = SpaceDesign.ink900, modifier = Modifier.weight(1f),
                )
            }
            Text(
                "보기 ›", fontFamily = customFontFamily, fontSize = 13.sp, color = SpaceDesign.ink600,
                modifier = Modifier.clickable { openPolicyPage(ctx, SPACE_BOOKING_THIRD_PARTY_POLICY) }.padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
            )
        }
        Text(
            "무용연은 통신판매중개자로 공간 대관 계약의 당사자가 아니에요.",
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = SpaceDesign.ink500,
            modifier = Modifier.padding(start = 32.dp),
        )
    }
}
