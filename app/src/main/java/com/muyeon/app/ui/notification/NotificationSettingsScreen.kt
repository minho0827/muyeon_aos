package com.muyeon.app.ui.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * 알림 설정 — iOS `NotificationSettingsView.swift` 이식.
 *  1) 전체 푸시 알림(마스터)  2) 종류별 알림(채팅·레슨·견적·공고·공간·활동)
 *  시스템·공지는 끌 수 없다(locked).
 *
 * ⚠️ 여기서 끄는 것은 **푸시뿐**이다. 알림함에는 그대로 쌓인다(서버도 같은 규칙).
 *   끄면 기록까지 사라지면 "알림이 안 왔다"를 확인해 줄 방법이 없어진다.
 *
 * 카테고리 라벨·설명은 서버가 내려준다 — 앱이 문구를 들고 있으면 카테고리가 늘 때마다
 *  스토어 심사를 다시 받아야 한다.
 */
@Composable
fun NotificationSettingsScreen(api: NotificationApi, onClose: () -> Unit) {
    var prefs by remember { mutableStateOf<NotificationPrefs?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.prefs()
            .onSuccess { prefs = it; error = null }
            .onFailure { error = "설정을 불러오지 못했어요." }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    // 낙관적 반영 — 토글은 손끝에 바로 붙어야 한다. 실패하면 되돌리고 알린다.
    fun setMaster(on: Boolean) {
        val before = prefs ?: return
        prefs = before.copy(pushEnabled = on)
        scope.launch {
            api.updatePrefs(pushEnabled = on)
                .onSuccess { prefs = it }
                .onFailure { prefs = before; error = "설정을 저장하지 못했어요." }
        }
    }

    fun setCategory(key: String, on: Boolean) {
        val before = prefs ?: return
        prefs = before.copy(
            categories = before.categories.map { if (it.key == key) it.copy(enabled = on) else it },
        )
        scope.launch {
            api.updatePrefs(category = key to on)
                .onSuccess { prefs = it }
                .onFailure { prefs = before; error = "설정을 저장하지 못했어요." }
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(title = "알림 설정", onBack = onClose)
        HorizontalDivider(color = MuyeonColors.border)

        val current = prefs
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            current == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    error ?: "설정을 불러오지 못했어요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            }
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                MasterCard(current.pushEnabled, ::setMaster)
                CategorySection(current, ::setCategory)
                Text(
                    "끈 알림도 앱 안 '알림'에는 그대로 쌓여요. " +
                        "휴대폰 설정에서 무용연 알림을 꺼 두면 여기 설정과 무관하게 푸시가 오지 않아요.",
                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 16.sp,
                    color = MuyeonColors.textSub, modifier = Modifier.padding(bottom = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun MasterCard(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MuyeonColors.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "전체 푸시 알림",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp, lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            Text(
                "끄면 아래 설정과 무관하게 푸시가 오지 않아요.",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
        )
    }
}

@Composable
private fun CategorySection(prefs: NotificationPrefs, onChange: (String, Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "종류별 알림",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
            modifier = Modifier.padding(start = 4.dp),
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.surface),
        ) {
            prefs.categories.forEachIndexed { index, category ->
                CategoryRow(category, disabled = !prefs.pushEnabled, onChange = onChange)
                if (index < prefs.categories.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MuyeonColors.border)
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: NotificationCategoryPref,
    disabled: Boolean,
    onChange: (String, Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                category.label,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            Text(
                category.desc,
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub,
            )
        }
        if (category.locked) {
            // 끌 수 없는 것에 꺼진 스위치를 두면 "고장난 스위치"로 읽힌다 — 이유를 글로 말한다.
            Text(
                "항상 받음",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub,
            )
        } else {
            Switch(
                checked = category.enabled && !disabled,
                enabled = !disabled,
                onCheckedChange = { onChange(category.key, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
            )
        }
    }
}

class NotificationSettingsActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val i = Intent(context, NotificationSettingsActivity::class.java)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { NotificationApi(TokenManager.getAccessToken(this)) }
            NotificationSettingsScreen(api = api, onClose = { finish() })
        }
    }
}
