package com.muyeon.app.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import kotlinx.coroutines.launch

/**
 * 내 후기 [수정] · [삭제] — 내 후기(mine) 옆 어디든 이 한 줄만 붙인다(레슨 상세·후기 목록·후기 상세·강사 프로필).
 *  수정 = onEdit(작성 화면 수정 모드 ReviewEditSheet 로 보내기), 삭제 = 확인창 → DELETE …/reviews/mine → onDeleted.
 *  iOS 도 같은 이름·같은 문구.
 */
@Composable
fun ReviewOwnerActions(
    api: ReviewApi,
    teacherId: Int,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Row(
        modifier.testTag("review.ownerActions"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionText("수정", MuyeonColors.textSub, "review.edit") { onEdit() }
        Text("·", fontSize = 12.sp, color = MuyeonColors.hint)
        ActionText("삭제", MuyeonColors.textSub, "review.delete") { if (!busy) confirm = true }
    }

    if (confirm) {
        MuyeonDialog(
            title = "후기를 삭제할까요?",
            message = "삭제한 후기는 되돌릴 수 없어요.",
            actions = listOf(
                MuyeonDialogAction("삭제", MuyeonDialogKind.DESTRUCTIVE) {
                    busy = true
                    scope.launch {
                        api.removeMine(teacherId)
                            .onSuccess { onDeleted() }
                            .onFailure { error = it.message ?: "후기를 삭제하지 못했어요." }
                        busy = false
                    }
                },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { confirm = false },
        )
    }
    error?.let { msg ->
        MuyeonDialog(
            title = "안내", message = msg,
            actions = listOf(MuyeonDialogAction("확인")),
            onDismiss = { error = null },
        )
    }
}

@Composable
private fun ActionText(text: String, color: androidx.compose.ui.graphics.Color, tag: String, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
        lineHeight = 14.sp, color = color,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp).testTag(tag),
    )
}
