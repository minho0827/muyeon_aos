package com.muyeon.app.ui.common.market

import android.content.Context
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.style.TextAlign
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
import com.muyeon.app.result.findActivity
import com.muyeon.app.result.finishWithResult
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


/** 단계형 위저드 머리 — "2 / 7 · 위치" + 진행 막대(채용 위저드 스텝 인디케이터와 같은 주황·회색). */
@Composable
fun WizardProgress(titles: List<String>, step: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${step + 1}", style = ts(FontWeight.Bold, 14).merge(TabularNums), color = MuyeonColors.primary)
            Text(" / ${titles.size}", style = ts(FontWeight.Medium, 14).merge(TabularNums), color = MuyeonColors.chevron)
            Text("  ${titles.getOrElse(step) { "" }}", style = ts(FontWeight.SemiBold, 14), color = MuyeonColors.textHead)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            titles.indices.forEach { i ->
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if (i <= step) MuyeonColors.primary else MuyeonColors.border))
            }
        }
    }
}

/** 위저드 제목줄 — 가운데 제목 + 왼쪽 ✕. */
@Composable
fun WizardTopBar(title: String, onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(
            title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
            textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center),
        )
        Icon(
            Icons.Filled.Close, "닫기", tint = MuyeonColors.body,
            modifier = Modifier.align(Alignment.CenterStart).size(18.dp).clickable(onClick = onClose),
        )
    }
}

/**
 * 위저드 머리 전체 — 제목줄 + 진행 + 구분선. showProgress=false(동의 단계 등)여도 진행 줄 자리는 그대로 두고
 *  안 보이게만 한다 — 동의 단계 → 1단계로 넘어갈 때 머리 높이가 바뀌지 않게.
 */
@Composable
fun WizardHeader(title: String, steps: List<String>, step: Int, onClose: () -> Unit, showProgress: Boolean = true) {
    WizardTopBar(title, onClose)
    Box(Modifier.alpha(if (showProgress) 1f else 0f)) { WizardProgress(steps, step) }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))
}

/** 위저드 아래 [이전](첫 단계 제외) [다음/등록]. */
@Composable
fun WizardNavBar(showPrev: Boolean, nextLabel: String, nextEnabled: Boolean, busy: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // [이전] 자리는 첫 단계에도 비워 둔다 — 단계가 바뀔 때 [다음] 버튼 폭이 변하지 않게.
        if (showPrev) WizardPrevButton(onPrev) else Spacer(Modifier.weight(1f).height(56.dp))
        Box(Modifier.weight(2f)) { StageBottomButton(nextLabel, enabled = nextEnabled, busy = busy, onClick = onNext) }
    }
}

@Composable
private fun RowScope.WizardPrevButton(onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        Alignment.Center,
    ) { Text("이전", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MuyeonColors.textHead) }
}

/** 작성 중 이탈 확인. */
@Composable
fun WizardExitDialog(onLeave: () -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "작성을 그만두고 나갈까요?",
        message = "작성 중인 내용은 저장되지 않아요.",
        actions = listOf(
            MuyeonDialogAction("나가기", MuyeonDialogKind.DESTRUCTIVE) { onLeave() },
            MuyeonDialogAction("계속 작성", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}

/** 비로그인 안내 문구 — 이 문구면 팝업이 [취소]/[로그인]으로 바뀐다(로그인 화면으로 이동). */
const val LOGIN_REQUIRED_MESSAGE = "로그인 후 이용할 수 있어요."

/**
 * 한 줄 안내 팝업([확인]). 비로그인 안내([LOGIN_REQUIRED_MESSAGE])면 [취소]/[로그인] —
 *  ★ 2026-11-03 [로그인]은 화면을 결과(LOGIN_REQUIRED)와 함께 닫는다 → 부모가 결과 콜백으로 받아
 *   차례로 닫히고, 웹뷰가 받으면 로그인 화면으로 이동한다(타이머·지연 이동 없음).
 */
@Composable
fun MarketMessageDialog(message: String, onDismiss: () -> Unit) {
    if (message == LOGIN_REQUIRED_MESSAGE) {
        val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
        MuyeonDialog(
            title = "로그인이 필요해요", message = message,
            actions = listOf(
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
                MuyeonDialogAction("로그인") { activity?.finishWithResult(com.muyeon.app.result.ResultKeys.LOGIN_REQUIRED) },
            ),
            onDismiss = onDismiss,
        )
    } else {
        MuyeonDialog(title = message, actions = listOf(MuyeonDialogAction("확인")), onDismiss = onDismiss)
    }
}

/** 위저드 로딩(수정 글 불러오는 중). */
@Composable
fun ColumnScope.WizardLoading() {
    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary) }
}

/** 주소 검색 버튼 + 고른 주소 미리보기(레슨 장소 카드 재사용). */
@Composable
fun AddressBlock(name: String, address: String, lat: Double?, lng: Double?, onSearch: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AddressSearchButton(hasAddress = address.isNotBlank(), onSearch)
        // 미리보기 자리는 주소를 고르기 전에도 같은 높이로 잡아 둔다(안 보이고 눌리지 않음) — 고른 뒤 아래 칸이 밀리지 않게.
        if (address.isNotBlank()) AddressPreview(name.ifBlank { address }, address, lat, lng)
        else Box(Modifier.alpha(0f).blockTouches()) { AddressPreview(" ", " ", null, null) }
    }
}

@Composable
private fun AddressSearchButton(hasAddress: Boolean, onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MuyeonList.ink100).clickable(onClick = onSearch).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Search, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
        Text(
            if (!hasAddress) "주소 검색" else "다른 주소로 바꾸기", fontFamily = customFontFamily, fontSize = 15.sp,
            color = if (!hasAddress) MuyeonColors.textSub else MuyeonColors.textHead,
        )
    }
}

@Composable
private fun AddressPreview(name: String, address: String, lat: Double?, lng: Double?) {
    val ctx = LocalContext.current
    LocationPreviewCard(LessonPlace(name = name, address = address, lat = lat, lng = lng)) { openLessonNaverMap(ctx, name, lat, lng) }
}

/** 숫자만 받는 입력칸. */
@Composable
fun NumberField(value: String, placeholder: String, max: Int = 9, onChange: (String) -> Unit) {
    FormTextField(value, { v -> onChange(v.filter { it.isDigit() }) }, placeholder, max, keyboardType = KeyboardType.Number)
}
