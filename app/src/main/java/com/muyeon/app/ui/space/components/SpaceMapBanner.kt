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

/** banner_h51_member → 지도 배너. */
@Composable
internal fun SpaceMapBanner(onClick: () -> Unit) {
    // 시안은 51pt 박스 안 40pt 막대(y=11) 구조지만, 일러스트가 위아래로 삐져나오는 용도라
    //  아이콘 1개만 쓰는 여기서는 막대(40dp)를 그대로 컨테이너로 쓴다
    //  → 아이콘·텍스트·꺾쇠가 모두 막대 기준 세로 중앙에 정렬된다.
    Row(
        Modifier.padding(horizontal = SpaceDesign.gutter).padding(top = 16.dp)
            .fillMaxWidth().height(40.dp)
            .clip(RoundedCornerShape(8.dp)).background(SpaceDesign.bannerBg)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(start = 8.dp).width(61.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Map, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append("지도 앱에서 ") }
                withStyle(SpanStyle(color = SpaceDesign.bannerAccent)) { append("위치·길찾기") }
                withStyle(SpanStyle(color = Color.White)) { append(" 확인") }
            },
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        )
        Box(Modifier.padding(end = 15.dp)) { SpaceChevronRight() }
    }
}
