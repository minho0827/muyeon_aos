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

@Composable
internal fun SpaceFacilitySection(space: SpaceDetail) = WhiteSection("시설 안내") {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReadField("수용인원", space.capacityText)
        space.spaceType?.takeIf { it.isNotEmpty() }?.let { ReadField("공간유형", it) }
        space.areaText?.let { ReadField("공간면적", it) }
        ReadField("영업시간", space.businessHoursText)
        ReadField("휴무일", space.holidaysText)

        val items = space.enabledFacilities
        if (items.isNotEmpty()) {
            // LazyVerticalGrid 는 스크롤 컨테이너 안에서 높이가 무한이라 못 쓴다 — 4열로 직접 쪼갠다.
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { f ->
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(SpaceDesign.ink100).padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(f.icon, null, tint = SpaceDesign.primaryText, modifier = Modifier.size(20.dp))
                                Text(
                                    f.label,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                                    lineHeight = 14.sp, color = SpaceDesign.ink700,
                                    textAlign = TextAlign.Center, maxLines = 2,
                                )
                            }
                        }
                        // 마지막 줄이 4개가 안 되면 빈 칸으로 채워 열 폭을 유지한다.
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        space.facilityList?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }?.let {
            NumberedBlock("시설 상세", it)
        }
    }
}
