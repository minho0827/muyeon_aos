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
import androidx.compose.foundation.layout.RowScope
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


/** 두 버튼 하단 바(글쓴이·호스트용 [수정] [상태 변경] 등). */
@Composable
fun MarketOwnerBar(left: String, onLeft: () -> Unit, right: String, onRight: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
                    .clickable(onClick = onLeft),
                Alignment.Center,
            ) { Text(left, style = ts(FontWeight.Bold, 17), color = MuyeonColors.textHead) }
            Box(Modifier.weight(1f)) { StageBottomButton(right, onClick = onRight) }
        }
    }
}

/** 고정 하단 바 틀 — 위 구분선 + 가로 줄(간격 8). 안에 놓을 버튼은 아래 조각들을 골라 쓴다. */
@Composable
fun MarketBottomBarFrame(content: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** 하단 바 하트(26dp). */
@Composable
fun MarketBarHeart(liked: Boolean, onLike: () -> Unit) = MarketHeart(liked, 26.dp, onLike)

/** 하단 바 전화 버튼 — 누르면 전화 앱(다이얼). */
@Composable
fun MarketBarPhone(phone: String) {
    val ctx = LocalContext.current
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
            .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))) } }
            .semantics { contentDescription = "전화하기" },
        Alignment.Center,
    ) { Icon(Icons.Filled.Call, null, tint = MuyeonColors.textHead, modifier = Modifier.size(22.dp)) }
}

/** 하단 바 보조 버튼(테두리, 글자 폭). */
@Composable
fun MarketBarSecondary(label: String, onClick: () -> Unit) {
    Box(
        Modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        Alignment.Center,
    ) { Text(label, style = ts(FontWeight.Bold, 16), color = MuyeonColors.textHead) }
}

/** 하단 바 주 버튼 — 남은 폭을 채운다. */
@Composable
fun RowScope.MarketBarPrimary(label: String, busy: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.weight(1f)) { StageBottomButton(label, busy = busy, onClick = onClick) }
}

/** 시트 하단 [초기화] [적용]. */
@Composable
fun SheetActions(onReset: () -> Unit, onApply: () -> Unit, applyLabel: String = "적용") {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(MuyeonList.ink100).clickable(onClick = onReset),
            Alignment.Center,
        ) { Text("초기화", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MuyeonColors.textHead) }
        Box(Modifier.weight(2f)) { StageBottomButton(applyLabel, onClick = onApply) }
    }
}

/** 목록 카드 아래 작은 테두리 버튼(수정·상태 변경·삭제). */
@Composable
fun MarketRowButton(label: String, modifier: Modifier, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, MuyeonList.ink200, RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        Alignment.Center,
    ) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (danger) MuyeonColors.danger else MuyeonColors.textHead)
    }
}
