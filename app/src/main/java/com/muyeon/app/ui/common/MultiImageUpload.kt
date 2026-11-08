package com.muyeon.app.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-08 묶음 2 — 여러 장 사진 고르기 → 줄이기(UploadImagePreparer) → 올리기(NativeHttp.uploadImage = POST /uploads/image).
 *   웹 components/muyeon/ImageUploader(multi) 와 같다: 남은 자리([room])만큼만 올리고, 다 올린 주소를 한 번에 [onUploaded].
 *   양도·채용 위저드가 화면 안에 쓰던 같은 순서를 학원 정보·자료 업로드가 함께 쓰게 꺼냈다. 반환값을 부르면 사진 고르기가 열린다.
 */
@Composable
fun rememberMultiImageUpload(
    max: Int,
    room: () -> Int,
    onUploading: (Boolean) -> Unit,
    onUploaded: (List<String>) -> Unit,
    onError: (String?) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val roomNow = rememberUpdatedState(room)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = max)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        onUploading(true)
        scope.launch {
            val http = NativeHttp(context)
            val out = mutableListOf<String>()
            for (u in uris.take(roomNow.value().coerceAtLeast(0))) {
                val bytes = UploadImagePreparer.prepare(context, u)
                if (bytes == null) { onError(null); continue }
                http.uploadImage(bytes).onSuccess { out += it }.onFailure { onError(it.message) }
            }
            if (out.isNotEmpty()) onUploaded(out)
            onUploading(false)
        }
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
