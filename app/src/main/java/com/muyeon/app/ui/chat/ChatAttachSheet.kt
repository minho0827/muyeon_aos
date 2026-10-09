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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    onCameraDenied: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    // 사진 최대 10장 — iOS appMediaPicker(maxCount: 10) 와 같다.
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_IMAGES),
    ) { uris -> if (uris.isNotEmpty()) { onPickImages(uris); onDismiss() } }

    // ── 카메라 — 촬영 결과를 앨범 사진과 같은 전송 경로(압축 → 업로드 → 한 건 전송)로 보낸다 ──
    //  촬영 파일 경로는 화면 회전·프로세스 재생성에도 남도록 문자열로 저장한다.
    //  결과가 올 때까지 시트를 닫지 않는다(닫으면 런처가 해제되어 결과를 받지 못한다).
    var captureUri by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = captureUri?.let(android.net.Uri::parse)
        captureUri = null
        if (ok && uri != null) { onPickImages(listOf(uri)); onDismiss() }
    }
    fun launchCamera() {
        val uri = newCaptureUri(context) ?: return
        captureUri = uri.toString()
        runCatching { cameraLauncher.launch(uri) }.onFailure { captureUri = null }
    }
    // CAMERA 권한을 매니페스트에 선언한 앱은 촬영 인텐트 전에 런타임 권한이 있어야 한다(없으면 SecurityException).
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else onCameraDenied()
    }

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
            AttachRow(Icons.Filled.PhotoLibrary, "사진", "최대 ${MAX_IMAGES}장") {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            AttachRow(Icons.Filled.PhotoCamera, "카메라", "바로 찍어서 보내기") {
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (granted) launchCamera() else cameraPermission.launch(android.Manifest.permission.CAMERA)
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

private const val MAX_IMAGES = 10

/**
 * 촬영 파일 Uri — cacheDir/camera 에 새 파일을 만들고 FileProvider 로 카메라 앱에 넘긴다.
 *  10분이 지난 이전 촬영 파일은 업로드가 끝났으므로 새로 찍을 때 지운다(캐시 누적 방지).
 */
private fun newCaptureUri(context: android.content.Context): android.net.Uri? = runCatching {
    val dir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
    val expired = System.currentTimeMillis() - 10 * 60 * 1000L
    dir.listFiles()?.filter { it.lastModified() < expired }?.forEach { it.delete() }
    val file = java.io.File(dir, "chat_${System.currentTimeMillis()}.jpg")
    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}.getOrNull()
