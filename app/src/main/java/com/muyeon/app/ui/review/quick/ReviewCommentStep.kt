package com.muyeon.app.ui.review.quick

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.review.ReviewApi
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-14 빠른 후기 2단계 — 한 줄 후기 + 사진(둘 다 선택) → [후기 보내기](당근 '따뜻한 거래 경험을 알려주세요!', iOS ReviewCommentStep 1:1).
 *  [onDetail] = 자세히 쓰기(기존 긴 후기 폼).
 */
@Composable
fun ReviewCommentStep(c: QuickReviewCoordinator, api: ReviewApi, onBack: () -> Unit, onSubmit: () -> Unit, onDetail: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        c.uploading = true
        scope.launch {
            uris.forEach { uri ->
                if (c.images.size >= 5) return@forEach
                val bytes = com.muyeon.app.ui.common.UploadImagePreparer.prepare(context, uri)
                bytes?.let { api.uploadImage(it).getOrNull()?.let { url -> c.images = c.images + url } }
            }
            c.uploading = false
        }
    }
    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(
                "따뜻한 레슨 경험을 알려주세요!", color = MuyeonColors.textHead, fontFamily = customFontFamily,
                fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "남겨 주신 후기는 ${c.teacherName}님 프로필에 공개돼요.", color = MuyeonColors.textSub,
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp),
            )
            val shape = RoundedCornerShape(10.dp)
            Column(Modifier.fillMaxWidth().padding(top = 20.dp).border(1.dp, MuyeonColors.border, shape).padding(16.dp)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
                    if (c.content.isEmpty()) {
                        Text(
                            "레슨 중 기억에 남았던 순간이나 고마웠던 마음을 짧게 남겨 보세요. (선택)",
                            color = MuyeonColors.hint, fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 22.sp,
                        )
                    }
                    BasicTextField(
                        value = c.content,
                        onValueChange = { c.content = it.take(1000) },
                        textStyle = TextStyle(fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 22.sp, color = MuyeonColors.textHead),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag("quickReview.content"),
                    )
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(shape).border(1.dp, MuyeonColors.border, shape)
                            .clickable(enabled = !c.uploading && c.images.size < 5) {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }.testTag("quickReview.photo"),
                        Alignment.Center,
                    ) {
                        if (c.uploading) CircularProgressIndicator(Modifier.size(20.dp), color = MuyeonColors.primary, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.CameraAlt, "사진 추가", tint = MuyeonColors.secondary)
                    }
                    c.images.forEach { url ->
                        Box(Modifier.size(56.dp).clip(shape)) {
                            RemoteImage(url, Modifier.size(56.dp))
                            Box(
                                Modifier.align(Alignment.TopEnd).size(20.dp).clip(RoundedCornerShape(50))
                                    .background(Color(0x99000000)).clickable { c.images = c.images - url },
                                Alignment.Center,
                            ) { Icon(Icons.Filled.Close, "사진 빼기", tint = Color.White, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
            }
            c.error?.let {
                Text(it, color = MuyeonColors.danger, fontFamily = customFontFamily, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Text(
                "자세히 쓰기", color = MuyeonColors.textSub, fontFamily = customFontFamily, fontSize = 14.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(top = 18.dp).clickable(onClick = onDetail).testTag("quickReview.detail"),
            )
        }
        QuickReviewBottomButton(if (c.submitting) "보내는 중…" else "후기 보내기", !c.submitting && !c.uploading, onSubmit, "quickReview.submit")
    }
}
