package com.muyeon.app.ui.gate.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import org.json.JSONObject

/**
 * 웹 Maintenance.js 의 [개발용] 에러 정보 상자 — 웹 창이 점검 화면으로 오기 직전 남긴 sessionStorage lastError(status·message·url·time).
 *  값이 없으면 그리지 않는다. 읽을 수 없으면 '에러 데이터를 읽을 수 없어요.'(웹과 같다). [전체 JSON 보기]는 접었다 편다.
 */
@Composable
fun MaintenanceErrorBox(raw: String) {
    val o = runCatching { JSONObject(raw) }.getOrNull()
    var open by rememberSaveable { mutableStateOf(false) }
    val mono = AppTypography.Body14.copy(fontFamily = FontFamily.Monospace)
    Column(
        Modifier.fillMaxWidth().padding(top = Dimensions.spacingXL)
            .border(1.dp, AppColors.Danger.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .background(AppColors.Danger.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
            .padding(Dimensions.spacingL),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
    ) {
        Text(stringResource(R.string.gate_maintenance_dev_title), style = AppTypography.Section16, color = AppColors.Danger)
        if (o == null) {
            Text("Message: " + stringResource(R.string.gate_maintenance_dev_unreadable), style = mono, color = AppColors.Danger)
        } else {
            listOf("Status" to "status", "Message" to "message", "URL" to "url", "Time" to "time").forEach { (label, key) ->
                Text("$label: ${o.opt(key)?.takeIf { it != JSONObject.NULL } ?: ""}", style = mono, color = AppColors.Danger)
            }
        }
        Text(stringResource(R.string.gate_maintenance_dev_json), style = AppTypography.Body14, color = AppColors.TextSub,
            modifier = Modifier.padding(top = Dimensions.spacingS).clickable { open = !open })
        if (open) {
            Text(o?.toString(2) ?: raw, style = mono, color = AppColors.Danger,
                modifier = Modifier.fillMaxWidth().background(AppColors.Surface).border(1.dp, AppColors.Divider).padding(Dimensions.spacingS))
        }
    }
}
