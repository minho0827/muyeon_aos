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

/** 호스트의 다른 공간 — 가로로 넘기는 카드. 다른 공간이 있을 때만 부른다. */
@Composable
internal fun HostOtherSpacesRow(hostName: String?, spaces: List<SpaceListItem>, onOpen: (Int) -> Unit) = WhiteSection(
    hostName?.let { "${it}의 다른 공간" } ?: "호스트의 다른 공간",
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        spaces.forEach { o ->
            Column(Modifier.width(150.dp).clickable { onOpen(o.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.muyeon.app.ui.common.market.MarketThumb(o.image, Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp)))
                Text(o.name, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SpaceDesign.ink900,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                o.priceLine?.let { Text(it, fontFamily = customFontFamily, fontSize = 12.sp, color = SpaceDesign.ink600, maxLines = 1) }
            }
        }
    }
}
