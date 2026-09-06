package com.muyeon.app.ui.lesson

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import org.json.JSONObject

/** 우편번호 검색 결과 — iOS `PostcodeResult` 1:1. */
data class PostcodeResult(
    val roadAddress: String,
    val jibunAddress: String,
    val postalCode: String,
    val buildingName: String,
) {
    /** 도로명 우선, 없으면 지번 — iOS preferred. */
    val preferred: String get() = roadAddress.ifEmpty { jibunAddress }
}

/**
 * 다음(Daum) 우편번호 검색 시트 — iOS `MuyeonPostcodeSheet.swift` 1:1.
 *  WebView 에 postcode.v2.js 를 임베드한다. **키가 필요 없다.**
 *
 * ⚠️ 종전 AOS 위저드는 주소를 자유 텍스트로 받아 region/regionCode/lat/lng 가
 *   전부 비어 있었다 — 같은 레슨이 iOS 로 올리면 지역 필터·지도 핀이 붙고
 *   AOS 로 올리면 안 붙었다. 주소 표준화 경로를 iOS 와 같은 것으로 맞춘다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostcodeSheet(onComplete: (PostcodeResult) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(
                    "취소",
                    fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss),
                )
                Text(
                    "주소 검색",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            HorizontalDivider(color = MuyeonColors.border)
            PostcodeWebView(Modifier.weight(1f).fillMaxWidth().background(MuyeonColors.surface)) {
                onComplete(it)
                onDismiss()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PostcodeWebView(modifier: Modifier = Modifier, onComplete: (PostcodeResult) -> Unit) {
    val latest by rememberUpdatedState(onComplete)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // iOS 는 webkit.messageHandlers.postcode 를 쓴다. 안드로이드 대응은 JS 인터페이스.
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onComplete(json: String) {
                            val o = runCatching { JSONObject(json) }.getOrNull() ?: return
                            val r = PostcodeResult(
                                o.optString("roadAddress"), o.optString("jibunAddress"),
                                o.optString("postalCode"), o.optString("buildingName"),
                            )
                            // JS 스레드에서 온다 — Compose 상태 변경은 메인으로 넘긴다.
                            post { latest(r) }
                        }
                    },
                    "PostcodeBridge",
                )
                // baseUrl 을 daum 도메인으로 줘야 postcode.v2.js 가 정상 동작한다(iOS 와 동일).
                loadDataWithBaseURL(
                    "https://postcode.map.daum.net", POSTCODE_HTML, "text/html", "utf-8", null,
                )
            }
        },
    )
}

private const val POSTCODE_HTML = """
<!DOCTYPE html><html><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
<style>html,body{margin:0;padding:0;height:100%;}#postcode{width:100%;height:100vh;}</style>
</head><body>
<div id="postcode"></div>
<script src="https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
<script>
if (typeof daum !== 'undefined') {
  new daum.Postcode({
    oncomplete: function(data) {
      window.PostcodeBridge.onComplete(JSON.stringify({
        roadAddress: data.roadAddress || data.address || '',
        jibunAddress: data.jibunAddress || data.autoJibunAddress || '',
        postalCode: data.zonecode || '',
        buildingName: data.buildingName || ''
      }));
    }, width: '100%', height: '100%'
  }).embed(document.getElementById('postcode'));
}
</script>
</body></html>
"""
