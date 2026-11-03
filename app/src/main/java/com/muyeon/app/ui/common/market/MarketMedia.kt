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


/** 사진 갤러리 — 좌우 넘김 + "1 / N", 탭하면 전체 화면(ImageViewerActivity). 사진이 없으면 기본 이미지. */
@Composable
fun MarketGallery(images: List<String>, height: Dp = 280.dp, placeholderRes: Int? = null) {
    val ctx = LocalContext.current
    Box(Modifier.fillMaxWidth().height(height).background(MuyeonList.ink100), Alignment.Center) {
        if (images.isEmpty()) {
            // ★ 2026-11-03 기능별 기본 썸네일(양도양수 = 건물 그림)을 주면 칸을 꽉 채운다
            if (placeholderRes != null) Image(painterResource(placeholderRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Image(painterResource(R.drawable.icon_no_image), null, Modifier.size(96.dp), contentScale = ContentScale.Fit)
        } else {
            val pager = rememberPagerState { images.size }
            HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                AsyncImage(
                    MarketFormat.imageUrl(images[i]), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable {
                        ImageViewerActivity.start(ctx, JSONArray(images).toString(), pager.currentPage, false)
                    },
                )
            }
            if (images.size > 1) {
                Text(
                    "${pager.currentPage + 1} / ${images.size}", style = ts(FontWeight.Medium, 12).merge(TabularNums), color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** 카드 썸네일 — 없으면 기본 이미지. */
@Composable
fun MarketThumb(path: String?, modifier: Modifier, placeholderRes: Int? = null) {
    Box(modifier.background(MuyeonList.ink100), Alignment.Center) {
        val url = MarketFormat.imageUrl(path)
        if (url == null && placeholderRes != null) Image(painterResource(placeholderRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (url == null) Image(painterResource(R.drawable.icon_no_image), null, Modifier.size(40.dp), contentScale = ContentScale.Fit)
        else AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
