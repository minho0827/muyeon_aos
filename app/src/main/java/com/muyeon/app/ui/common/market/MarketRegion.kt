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
 * 주소 → 앱 지역 코드표(/api/regions) 항목. 주소 앞부분이 코드표 이름("서울 강서구", "경기 수원시 팔달구")과
 *  가장 길게 겹치는 것. 카카오 주소가 "서울특별시 …"처럼 길게 와도 앞 두 글자 시도명으로 맞춘다. 못 찾으면 null.
 */
object MarketRegion {
    private var cache: List<com.muyeon.app.ui.quote.RegionDTO>? = null

    suspend fun match(address: String, token: String?): com.muyeon.app.ui.quote.RegionDTO? {
        val list = cache ?: com.muyeon.app.ui.quote.RegionRepo.fetchRegions(token).also { if (it.isNotEmpty()) cache = it }
        val words = address.trim().split(Regex("\\s+"))
        if (words.size < 2) return null
        val sido = shortSido(words[0])
        val rest = words.drop(1).joinToString(" ")
        return list.filter { it.sido == sido && (rest == it.sigungu || rest.startsWith(it.sigungu + " ")) }
            .maxByOrNull { it.sigungu.length }
    }

    /** "서울 강서구 공항대로…" → "서울 강서구"(앞 두 단어, 시도는 짧은 이름). */
    fun regionName(address: String): String {
        val w = address.trim().split(Regex("\\s+"))
        return if (w.size >= 2) "${shortSido(w[0])} ${w[1]}" else address.trim()
    }

    private fun shortSido(s: String): String = when {
        s.startsWith("서울") -> "서울"; s.startsWith("부산") -> "부산"; s.startsWith("대구") -> "대구"
        s.startsWith("인천") -> "인천"; s.startsWith("광주") -> "광주"; s.startsWith("대전") -> "대전"
        s.startsWith("울산") -> "울산"; s.startsWith("세종") -> "세종"; s.startsWith("경기") -> "경기"
        s.startsWith("강원") -> "강원"; s.startsWith("충청북") || s == "충북" -> "충북"; s.startsWith("충청남") || s == "충남" -> "충남"
        s.startsWith("전라북") || s.startsWith("전북") -> "전북"; s.startsWith("전라남") || s == "전남" -> "전남"
        s.startsWith("경상북") || s == "경북" -> "경북"; s.startsWith("경상남") || s == "경남" -> "경남"
        s.startsWith("제주") -> "제주"; else -> s
    }
}
