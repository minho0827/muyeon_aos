package com.muyeon.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuotePrimaryButton
import com.muyeon.app.ui.quote.QuoteRegionPicker

/**
 * 온보딩 3종 — iOS `Onboarding/SignupTerms` · `NotificationConsent` · `AddressSetup` 1:1.
 *
 * ⚠️ 아래 상수는 iOS 값을 그대로 옮긴 것. **한쪽만 바꾸지 말 것**(양쪽 동시 수정).
 */
private object OnbMetric {
    const val gutter = 24          // 헤더/본문 좌우
    const val listGutter = 20      // 약관 항목·CTA 좌우
    const val titleSize = 24
    const val subtitleSize = 14
    const val checkAllSize = 24
    const val checkItemSize = 22
    const val corner = 12
    const val ctaVertical = 16
}

private val cAFAFAF = Color(0xFFAFAFAF)
private val cF4F4F4 = Color(0xFFF4F4F4)
private val c8E8E8E = Color(0xFF8E8E8E)

// ─────────────────────────────────────────────────────────── 약관 동의

/** 백엔드 REQUIRED_TERMS(terms/privacy/location/over14) + marketing 과 1:1. */
private data class TermItem(val key: String, val title: String, val required: Boolean, val doc: String?)

private val TERM_ITEMS = listOf(
    TermItem("terms", "이용약관 동의", true, "terms"),
    TermItem("privacy", "개인정보 수집·이용 동의", true, "privacy"),
    TermItem("location", "위치기반서비스 이용약관 동의", true, "location"),
    TermItem("over14", "만 14세 이상입니다", true, null),
    TermItem("marketing", "마케팅 정보 수신 동의 (선택)", false, null),
)

/**
 * 약관 동의(풀스크린, 강제) — 웹 `openSignupTerms` 진입.
 *  동의 → `window.__onSignupTermsAgreed(agreementsJson)` / 뒤로 → `window.__onSignupTermsDeclined()`
 */
@Composable
fun SignupTermsScreen(
    onAgree: (Map<String, Boolean>) -> Unit,
    onDecline: () -> Unit,
    onOpenPolicy: (String) -> Unit,
) {
    val checked = remember { mutableStateMapOf<String, Boolean>() }
    val allChecked = TERM_ITEMS.all { checked[it.key] == true }
    val allRequired = TERM_ITEMS.filter { it.required }.all { checked[it.key] == true }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Box(Modifier.size(44.dp).clickable { onDecline() }, Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", Modifier.size(22.dp), tint = MuyeonColors.textHead)
            }
        }

        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = OnbMetric.gutter.dp, vertical = 0.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "무용연 이용을 위해\n약관에 동의해 주세요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
                fontSize = OnbMetric.titleSize.sp, lineHeight = 33.sp,   // iOS 24 + lineSpacing 4 ≈ 33
                color = MuyeonColors.textHead,
            )
            Text(
                "필수 항목에 모두 동의하셔야 서비스를 이용할 수 있어요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium,
                fontSize = OnbMetric.subtitleSize.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
            )
        }

        // 전체 동의
        Row(
            Modifier.padding(horizontal = OnbMetric.listGutter.dp).fillMaxWidth()
                .clip(RoundedCornerShape(OnbMetric.corner.dp)).background(cF4F4F4)
                .clickable {
                    val next = !allChecked
                    TERM_ITEMS.forEach { checked[it.key] = next }
                }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckIcon(allChecked, OnbMetric.checkAllSize)
            Text(
                "약관 전체동의",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead,
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TERM_ITEMS.forEach { it ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = OnbMetric.listGutter.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.weight(1f).clickable { checked[it.key] = checked[it.key] != true },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckIcon(checked[it.key] == true, OnbMetric.checkItemSize)
                        Text(
                            "(${if (it.required) "필수" else "선택"}) ${it.title}",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.body,
                        )
                    }
                    it.doc?.let { doc ->
                        Box(Modifier.size(32.dp).clickable { onOpenPolicy(doc) }, Alignment.Center) {
                            Icon(Icons.Default.ChevronRight, "약관 보기", Modifier.size(16.dp), tint = cAFAFAF)
                        }
                    }
                }
            }
        }

        // 동의하고 계속 — iOS 는 활성 시 검정(#101116), 비활성 회색(#AFAFAF)
        Box(
            Modifier.fillMaxWidth()
                .padding(horizontal = OnbMetric.listGutter.dp).padding(top = 8.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(OnbMetric.corner.dp))
                .background(if (allRequired) MuyeonColors.textHead else cAFAFAF)
                .clickable(enabled = allRequired) { onAgree(TERM_ITEMS.associate { it.key to (checked[it.key] == true) }) }
                .padding(vertical = OnbMetric.ctaVertical.dp),
            Alignment.Center,
        ) {
            Text(
                "동의하고 계속",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White,
            )
        }
    }
}

@Composable
private fun CheckIcon(on: Boolean, size: Int) {
    Icon(
        if (on) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(size.dp),
        tint = if (on) MuyeonColors.textHead else cAFAFAF,
    )
}

// ─────────────────────────────────────────────────────── 알림 허용 바텀시트

/**
 * 알림 허용 시트 — iOS `NotificationConsentView`.
 *  Android 13+ 는 POST_NOTIFICATIONS 런타임 권한, 그 이하는 항상 granted.
 */
@Composable
fun NotificationConsentSheet(onAllow: (Boolean) -> Unit, onLater: () -> Unit) {
    var toggleOn by remember { mutableStateOf(true) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onAllow(granted)
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), Alignment.BottomCenter) {
        AnimatedVisibility(shown, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MuyeonColors.surface)
                    .padding(horizontal = OnbMetric.gutter.dp).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))
                Text(
                    "휴대폰 알림을 켜고",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    lineHeight = 26.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = MuyeonColors.primary)) { append("중요한 정보") }
                        withStyle(SpanStyle(color = MuyeonColors.textHead)) { append("를 받아보세요!") }
                    },
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    lineHeight = 26.sp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "강사·고객 메시지, 추천 서비스, 이벤트 등",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(OnbMetric.corner.dp)).background(cF4F4F4)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "무용연 앱 알림",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = toggleOn, onCheckedChange = { toggleOn = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                    )
                }

                Spacer(Modifier.height(40.dp))
                QuotePrimaryButton("알림 허용하기", Modifier.fillMaxWidth()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onAllow(true)   // 13 미만은 매니페스트 권한만으로 발송 가능
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().clickable { onLater() }.padding(vertical = 14.dp),
                    Alignment.Center,
                ) {
                    Text(
                        "나중에 받기",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                        lineHeight = 18.sp, color = c8E8E8E,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────── 주소 설정

private const val PREF_ONBOARDING = "muyeon.onboarding"
private const val KEY_ADDRESS = "muyeon.onboarding.address"
private const val KEY_ADDRESS_CODE = "muyeon.onboarding.addressCode"

/** iOS UserDefaults 키와 동일 문자열 유지 — 웹/네이티브 양쪽에서 같은 의미로 읽힌다. */
object OnboardingAddressStore {
    fun read(ctx: Context): Pair<String, String> {
        val p = ctx.getSharedPreferences(PREF_ONBOARDING, Context.MODE_PRIVATE)
        return (p.getString(KEY_ADDRESS, "") ?: "") to (p.getString(KEY_ADDRESS_CODE, "") ?: "")
    }

    fun persist(ctx: Context, region: String, code: String) {
        ctx.getSharedPreferences(PREF_ONBOARDING, Context.MODE_PRIVATE).edit()
            .putString(KEY_ADDRESS, region).putString(KEY_ADDRESS_CODE, code).apply()
    }
}

/** 관심 지역 설정 — iOS `AddressSetupView`. 지역 3단계 모달은 견적 위저드 것을 재사용. */
@Composable
fun AddressSetupScreen(
    token: String?,
    initialRegion: String,
    initialCode: String,
    onComplete: (String, String) -> Unit,
    onSkip: () -> Unit,
) {
    var region by remember { mutableStateOf(initialRegion) }
    var code by remember { mutableStateOf(initialCode) }
    var showPicker by remember { mutableStateOf(false) }
    val hasSelection = region.isNotEmpty()

    if (showPicker) {
        QuoteRegionPicker(
            token = token,
            onSelect = { names, codes -> region = names; code = codes; showPicker = false },
            onClose = { showPicker = false },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        Spacer(Modifier.height(24.dp))
        Text(
            "어느 지역에서\n레슨을 찾으세요?",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = OnbMetric.titleSize.sp,
            lineHeight = 33.sp, color = MuyeonColors.textHead,
            modifier = Modifier.padding(horizontal = OnbMetric.gutter.dp),
        )
        Text(
            "가까운 강사·학원을 먼저 보여드려요.",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textSub,
            modifier = Modifier.padding(horizontal = OnbMetric.gutter.dp).padding(top = 10.dp),
        )

        Row(
            Modifier.padding(horizontal = OnbMetric.gutter.dp).padding(top = 28.dp).fillMaxWidth()
                .clip(RoundedCornerShape(OnbMetric.corner.dp))
                .border(
                    1.dp,
                    if (hasSelection) MuyeonColors.primary else MuyeonColors.border,
                    RoundedCornerShape(OnbMetric.corner.dp),
                )
                .clickable { showPicker = true }.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = MuyeonColors.primary)
            Text(
                if (hasSelection) region else "지역을 선택해 주세요",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                lineHeight = 19.sp, modifier = Modifier.weight(1f),
                color = if (hasSelection) MuyeonColors.textHead else MuyeonColors.textSub,
            )
            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = cAFAFAF)
        }

        Spacer(Modifier.weight(1f))

        Column(
            Modifier.padding(horizontal = OnbMetric.gutter.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QuotePrimaryButton("이 지역으로 시작하기", Modifier.fillMaxWidth(), enabled = hasSelection) {
                onComplete(region, code)
            }
            Box(Modifier.clickable { onSkip() }.padding(vertical = 6.dp)) {
                Text(
                    "나중에 설정할게요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textSub,
                )
            }
        }
    }
}
