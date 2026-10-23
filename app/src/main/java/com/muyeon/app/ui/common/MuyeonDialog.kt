package com.muyeon.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muyeon.app.theme.customFontFamily

/**
 * 앱 공통 팝업(10-23, PACERA `PaceraDialog` 그대로 이식 — 색만 무용연 주황). iOS `MuyeonDialog.swift` 와 같은 값.
 *  Material3 AlertDialog 를 쓰지 않는다 — OS 가 그리면 iOS·AOS 모양과 색이 달라진다.
 *
 *   카드  흰 배경 / 모서리 20 / 좌우 여백 32 / 최대 폭 320
 *   제목  17sp Bold  #101116  왼쪽 정렬
 *   본문  14sp       #6D6E71  왼쪽 정렬
 *   버튼  가로 꽉 찬 세로 스택 (높이 48, 모서리 12)
 *         주요 = 주황 채움 + 흰 글씨 / 위험 = 빨강 글씨 / 취소 = 회색 글씨
 */
enum class MuyeonDialogKind {
    /** 이 팝업이 권하는 동작. 주황으로 채운다. 한 팝업에 하나만 둔다. */
    PRIMARY,

    /** 되돌릴 수 없는 동작. 빨간 글씨. */
    DESTRUCTIVE,

    /** 머무르기. 회색 글씨. 항상 맨 아래. */
    CANCEL,
}

data class MuyeonDialogAction(
    val title: String,
    val kind: MuyeonDialogKind = MuyeonDialogKind.PRIMARY,
    val onClick: () -> Unit = {},
)

/**
 * @param force 강제 팝업. 바깥을 눌러도, 뒤로 가도 닫히지 않는다(반드시 하나를 골라야 하는 자리만).
 */
@Composable
fun MuyeonDialog(
    title: String,
    message: String? = null,
    actions: List<MuyeonDialogAction>,
    force: Boolean = false,
    /** false 면 버튼을 눌러도 onDismiss 를 먼저 부르지 않는다 — 예전 팝업(QuoteDialog)의 버튼 동작을 그대로 두려고. */
    dismissOnAction: Boolean = true,
    /** 본문을 글 대신 직접 그릴 때(목록·링크 등). message 아래에 붙는다. */
    content: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!force) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !force, dismissOnClickOutside = !force),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
        ) {
            Text(title, fontFamily = customFontFamily, color = MuyeonColors.textHead, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (!message.isNullOrEmpty()) {
                Text(message, fontFamily = customFontFamily, color = MuyeonColors.textSub, fontSize = 14.sp, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
            if (content != null) Column(Modifier.padding(top = 8.dp)) { content() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
                actions.forEach { DialogButton(it, if (dismissOnAction) onDismiss else ({})) }
            }
        }
    }
}

@Composable
private fun DialogButton(action: MuyeonDialogAction, onDismiss: () -> Unit) {
    val fill = if (action.kind == MuyeonDialogKind.PRIMARY) MuyeonColors.primary else Color.Transparent
    val text = when (action.kind) {
        MuyeonDialogKind.PRIMARY -> Color.White
        MuyeonDialogKind.DESTRUCTIVE -> Color(0xFFFF3B30)
        MuyeonDialogKind.CANCEL -> MuyeonColors.textSub
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            // 어떤 버튼을 눌러도 먼저 팝업을 닫는다. ⚠️ onDismiss 가 onClick 보다 먼저 돈다 —
            //  onClick 이 필요한 값은 클로저로 잡아서 넘긴다(PACERA 2026-09-20 러닝 삭제 무시 사고).
            .clickable { onDismiss(); action.onClick() }
            .testTag("dialog.${action.title}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(action.title, fontFamily = customFontFamily, color = text, fontSize = 16.sp,
            fontWeight = if (action.kind == MuyeonDialogKind.PRIMARY) FontWeight.Bold else FontWeight.Medium)
    }
}

/** 글쓰기 이탈 확인(PACERA WriteExitDialog) — 나가기 = 작성 내용 유지 / 삭제하고 나가기 = 버림 / 취소 = 머무름. */
@Composable
fun WriteExitDialog(onKeepAndLeave: () -> Unit, onDeleteAndLeave: () -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "이 화면을 나가시겠습니까?",
        message = "작성 중인 내용은 그대로 유지됩니다.",
        actions = listOf(
            MuyeonDialogAction("나가기", MuyeonDialogKind.PRIMARY, onKeepAndLeave),
            MuyeonDialogAction("삭제하고 나가기", MuyeonDialogKind.DESTRUCTIVE, onDeleteAndLeave),
            MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}
