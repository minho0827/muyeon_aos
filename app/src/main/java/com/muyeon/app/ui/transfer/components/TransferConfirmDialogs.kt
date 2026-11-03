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
import kotlinx.coroutines.launch

/** 허위 매물 신고 확인. */
@Composable
fun TransferReportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "허위 매물로 신고할까요?",
        message = "운영팀이 확인 후 조치해요.",
        actions = listOf(
            MuyeonDialogAction("신고하기", MuyeonDialogKind.DESTRUCTIVE) { onConfirm() },
            MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}

/** 내 글 삭제 확인. */
@Composable
fun TransferDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "양도 글을 삭제할까요?",
        message = "삭제한 글은 되돌릴 수 없어요.",
        actions = listOf(
            MuyeonDialogAction("삭제", MuyeonDialogKind.DESTRUCTIVE) { onConfirm() },
            MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}
