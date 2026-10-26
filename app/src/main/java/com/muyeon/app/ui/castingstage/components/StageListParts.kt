package com.muyeon.app.ui.castingstage.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.screens.*

/** 목록 부품 — 상단바·한 줄(iOS StageListBar·StageListRow). */

@Composable
internal fun StageListBar(title: String, onBack: () -> Unit, onCreate: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable(onClick = onBack).semantics { contentDescription = "stage.detail.back" }, Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead)
        }
        Text(title, style = Bold, fontSize = 20.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
        // 공연 공고 등록(공연팀만). null 이면 숨김.
        if (onCreate != null) {
            Box(Modifier.size(44.dp).clickable(onClick = onCreate).semantics { contentDescription = "공연 공고 등록" }, Alignment.Center) {
                Icon(Icons.Filled.Add, null, tint = MuyeonColors.textHead)
            }
        }
    }
}

@Composable
internal fun StageListRow(image: String?, icon: ImageVector, tint: Color, bg: Color, kicker: String?, title: String, sub: String?, placeholder: Int? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(104.dp).height(136.dp).clip(RoundedCornerShape(12.dp)).background(bg), Alignment.Center) {
            if (placeholder != null && stageImageUrl(image) == null)
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(placeholder), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            stageImageUrl(image)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            kicker?.let { Text(it, style = Bold, fontSize = 14.sp, color = Blue) }
            Text(title, style = Semi, fontSize = 17.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
            sub?.takeIf { it.isNotBlank() }?.let { Text(it, style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub, maxLines = 2) }
        }
    }
}
