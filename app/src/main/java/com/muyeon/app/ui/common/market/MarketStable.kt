package com.muyeon.app.ui.common.market

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.launch

/**
 * 화면 흔들림(레이아웃 이동) 방지 공용 조각 — 2026-11-03.
 *  · 숫자는 고정폭 숫자(tnum)로 — 금액·인원·"1 / 5"가 바뀌어도 글자 폭이 그대로.
 *  · 로딩은 작은 스피너 대신 실제 모양·크기의 회색 자리(스켈레톤).
 *  · 상태 전환은 크기 애니메이션 없이 겹쳐 바꾸기(Crossfade)만.
 */
val TabularNums = TextStyle(fontFeatureSettings = "tnum")

/** 스켈레톤 회색 면. */
val SkeletonColor = Color(0xFFEFEFF1)

fun Modifier.skeleton(radius: Int = 6): Modifier = this.clip(RoundedCornerShape(radius.dp)).background(SkeletonColor)

/** 글자 한 줄 자리 — 실제 글자와 같은 줄 높이(같은 크기·줄 수)의 투명 글자 뒤에 회색 막대. */
@Composable
fun SkeletonLine(fontSize: Int, widthFraction: Float = 1f, lines: Int = 1, lineHeight: Int? = null) {
    Box(Modifier.fillMaxWidth(widthFraction)) {
        Text(
            " ", fontFamily = customFontFamily, fontSize = fontSize.sp,
            lineHeight = lineHeight?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
            minLines = lines, maxLines = lines, color = Color.Transparent,
            modifier = Modifier.fillMaxWidth().skeleton(4),
        )
    }
}

/** 상태 바꾸기 — 크기 애니메이션 없이 겹쳐 바꾸기만(자리 크기는 바깥이 정한다). */
@Composable
fun <T> StableSwap(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    Crossfade(target, modifier, animationSpec = tween(180), label = "stable") { content(it) }
}

/**
 * 목록 끝 "더 읽는 중" 자리 — 더 읽을 게 있으면 늘 같은 높이로 자리를 잡아 두고, 읽는 중일 때만 스피너를 그린다.
 */
@Composable
fun ListMoreFooter(loading: Boolean) {
    Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) {
        if (loading) CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.size(24.dp))
    }
}

/** 하단 바 자리(불러오는 중) — 실제 하단 바와 같은 높이. */
@Composable
fun MarketBottomBarSkeleton() {
    MarketBottomBarFrame {
        Box(Modifier.weight(1f).height(56.dp).skeleton(16))
    }
}

/** 목록 화면의 상태 — 한 번에 하나만 그린다(겹쳐 바꾸기). */
enum class ListPhase { Loading, Error, Empty, Items }

/** 상세 화면의 상태. */
enum class DetailPhase { Loading, Error, Content }

/** 하단 바 높이(구분선 1 + 위아래 10 + 버튼 56)만큼의 빈 자리 — 오류 화면에서도 본문 영역 크기를 그대로 둔다. */
@Composable
fun BottomBarSpace() = Spacer(Modifier.fillMaxWidth().height(77.dp))

/** 자리만 잡는(안 보이는) 칸이 눌리지 않게 — 안쪽보다 먼저 눌림을 받아 없앤다. */
fun Modifier.blockTouches(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}
