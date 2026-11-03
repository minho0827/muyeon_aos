package com.muyeon.app.ui.transfer.components

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
import androidx.compose.material.icons.filled.Tune
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
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

/** 지역 칩(전체·시도) + 오른쪽 필터 아이콘(필터가 걸려 있으면 주황). */
@Composable
fun TransferRegionChips(region: String?, filterActive: Boolean, onRegion: (String?) -> Unit, onFilter: () -> Unit) {
    MuyeonPillTabs(
        TransferLabels.regions.map { (name, code) -> MuyeonPillTab(code ?: "", name) },
        region ?: "", { onRegion(it.ifEmpty { null }) },
        tagPrefix = "transfer.region", compact = true,
    ) {
        Box(
            Modifier.padding(end = 8.dp).size(40.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onFilter),
            Alignment.Center,
        ) {
            Icon(Icons.Filled.Tune, "필터", tint = if (filterActive) MuyeonColors.primary else MuyeonColors.textHead, modifier = Modifier.size(22.dp))
        }
    }
}
