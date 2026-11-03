package com.muyeon.app.ui.common.market

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.lesson.LessonPlace
import com.muyeon.app.ui.lesson.LocationPreviewCard
import com.muyeon.app.ui.lesson.openLessonNaverMap
import com.muyeon.app.ui.quote.QuoteUi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray


/**
 * 위치 카드 — 레슨 장소 미리보기(LocationPreviewCard + 네이버 미니 지도) 재사용.
 *  카드를 탭하면 길찾기 앱 선택(카카오맵·네이버지도·구글 지도). approxNote 가 있으면 카드 아래 안내.
 */
@Composable
fun MarketMapCard(name: String, address: String?, lat: Double?, lng: Double?, approxNote: String? = null) {
    val ctx = LocalContext.current
    var choose by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.clickable { choose = true }) {
            LocationPreviewCard(LessonPlace(name = name, address = address, lat = lat, lng = lng)) { choose = true }
        }
        approxNote?.let { Text(it, style = ts(FontWeight.Medium, 13), color = MuyeonColors.textSub) }
    }
    if (choose) {
        MuyeonDialog(
            title = "길찾기 앱 선택",
            actions = listOf(
                MuyeonDialogAction("카카오맵", MuyeonDialogKind.CANCEL) { openDirections(ctx, "kakao", name, lat, lng) },
                MuyeonDialogAction("네이버지도", MuyeonDialogKind.CANCEL) { openDirections(ctx, "naver", name, lat, lng) },
                MuyeonDialogAction("구글 지도", MuyeonDialogKind.CANCEL) { openDirections(ctx, "google", name, lat, lng) },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { choose = false },
        )
    }
}

/** 길찾기 — 앱이 없으면 웹 지도로. */
fun openDirections(ctx: Context, app: String, name: String, lat: Double?, lng: Double?) {
    val q = Uri.encode(name)
    fun open(uri: String): Boolean = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }.isSuccess
    when (app) {
        "kakao" -> {
            val ok = lat != null && lng != null && open("kakaomap://look?p=$lat,$lng")
            if (!ok) open(if (lat != null && lng != null) "https://map.kakao.com/link/map/$q,$lat,$lng" else "https://map.kakao.com/link/search/$q")
        }
        "naver" -> openLessonNaverMap(ctx, name, lat, lng)
        else -> {
            val ok = open(if (lat != null && lng != null) "geo:$lat,$lng?q=$lat,$lng($q)" else "geo:0,0?q=$q")
            if (!ok) open("https://www.google.com/maps/search/?api=1&query=" + (if (lat != null && lng != null) "$lat,$lng" else q))
        }
    }
}
