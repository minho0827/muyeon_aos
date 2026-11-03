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

/** 위치 — 좌표가 있으면 지도 카드(레슨 장소 카드 재사용, 탭하면 길찾기 앱 선택). */
@Composable
internal fun SpaceMapLocationSection(name: String, address: String?, lat: Double, lng: Double) = WhiteSection("위치") {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MarketMapCard(name, address, lat, lng)
    }
}

/** 위치 — 좌표가 없을 때 주소 글자 + (주소가 있으면) [지도 앱으로 열기]. */
@Composable
internal fun SpaceAddressLocationSection(address: String?, onOpenMap: () -> Unit) = WhiteSection("위치") {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReadField("주소", address ?: "-")
        address?.let { OpenMapButton(onOpenMap) }
    }
}

@Composable
private fun OpenMapButton(onOpenMap: () -> Unit) {
    Text(
        "지도 앱으로 열기",
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        lineHeight = 18.sp, color = SpaceDesign.ink900, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, SpaceDesign.ink200, RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenMap)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
