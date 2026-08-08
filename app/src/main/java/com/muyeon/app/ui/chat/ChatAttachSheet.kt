package com.muyeon.app.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * 채팅 첨부 바텀시트 — iOS `ChatAttachSheet.swift` 대응.
 *
 *  ⚠️ 의도적 차이: iOS 는 시트 안에 최근 앨범 썸네일 스트립을 직접 그린다(PHAsset 접근).
 *   Android 는 **시스템 Photo Picker**(ActivityResultContracts.PickMultipleVisualMedia)를 쓴다 —
 *   READ_MEDIA_IMAGES 권한이 아예 필요 없고(안드 13+ 권장 방식) 앨범 UI 는 OS 가 제공한다.
 *   권한 거부 분기(iOS permissionView)가 불필요해지는 대신, 시트에는 액션 리스트만 남는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAttachSheet(
    showSurvey: Boolean,       // 강사 측일 때만 설문지 노출
    showProposal: Boolean,     // 강사 측일 때만 '레슨 약속잡기' 노출
    onPickImages: (List<android.net.Uri>) -> Unit,
    onPickVideo: (android.net.Uri) -> Unit,
    onSurvey: () -> Unit,
    onProposal: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
    ) { uris -> if (uris.isNotEmpty()) { onPickImages(uris); onDismiss() } }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) { onPickVideo(uri); onDismiss() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "첨부",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 12.dp),
            )
            AttachRow(Icons.Filled.PhotoLibrary, "사진", "최대 5장") {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            AttachRow(Icons.Filled.Videocam, "동영상", null) {
                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
            if (showProposal) {
                AttachRow(Icons.Filled.EventNote, "레슨 약속잡기", "날짜·시간 제안") { onProposal(); onDismiss() }
            }
            if (showSurvey) {
                AttachRow(Icons.Filled.Assignment, "설문지", "레슨 전 문진 보내기") { onSurvey(); onDismiss() }
            }
        }
    }
}

@Composable
private fun AttachRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MuyeonColors.primary, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            subtitle?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub,
                )
            }
        }
    }
}
