package com.muyeon.app.ui.quote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import androidx.lifecycle.lifecycleScope
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 견적 문진 풀스크린 — 웹 `openQuoteWizard` 브릿지로 진입.
 *  iOS `presentQuoteWizardDirect` 대응(카테고리/지정강사/프리필 인자 동일).
 */
class QuoteWizardActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_CATEGORY = "categoryId"
        private const val EXTRA_TARGET_TEACHER = "targetTeacherId"

        fun start(context: Context, categoryId: String?, targetTeacherId: String?) {
            val i = Intent(context, QuoteWizardActivity::class.java)
                .putExtra(EXTRA_CATEGORY, categoryId ?: "")
                .putExtra(EXTRA_TARGET_TEACHER, targetTeacherId ?: "")
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val targetTeacherId = intent.getStringExtra(EXTRA_TARGET_TEACHER).orEmpty()
        val category = QuoteCategory.find(categoryId) ?: QuoteCategory.all[0]

        setContent {
            val vm = remember { QuoteWizardViewModel(category) }
            var showExit by remember { mutableStateOf(false) }
            var submitting by remember { mutableStateOf(false) }

            Box(Modifier.fillMaxSize()) {
                QuoteWizardScreen(
                    vm = vm,
                    onClose = {
                        // 문진 진행 중이면 이탈 방지 시트, 첫 질문이면 바로 닫기(iOS 동일).
                        if (vm.currentIndex > 0) showExit = true else finish()
                    },
                    onComplete = { answers ->
                        if (!submitting) {
                            submitting = true
                            submit(answers, category.id, targetTeacherId) { ok ->
                                submitting = false
                                Toast.makeText(
                                    this@QuoteWizardActivity,
                                    if (ok) "견적 요청이 전달되었어요!" else "요청에 실패했어요. 잠시 후 다시 시도해 주세요.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                if (ok) finish()
                            }
                        }
                    },
                )
                if (showExit) {
                    QuoteExitSheet(
                        onStop = { finish() },
                        onContinue = { showExit = false },
                    )
                }
            }
        }
    }

    /** POST /api/quotes — iOS QuoteService.createQuote 와 동일 페이로드. */
    private fun submit(
        answers: Map<String, QuoteAnswer>,
        categoryId: String,
        targetTeacherId: String,
        onDone: (Boolean) -> Unit,
    ) {
        val token = TokenManager.getAccessToken(this)
        if (token.isNullOrEmpty()) { onDone(false); return }

        val arr = JSONArray()
        var region: String? = null
        var regionCode: String? = null
        answers.values.forEach { a ->
            val o = JSONObject().put("questionId", a.questionId)
            if (a.selectedOptionIds.isNotEmpty()) o.put("optionIds", JSONArray(a.selectedOptionIds))
            a.text?.takeIf { it.isNotEmpty() }?.let { o.put("text", it) }
            a.region?.takeIf { it.isNotEmpty() }?.let { o.put("region", it); region = it }
            a.regionCode?.takeIf { it.isNotEmpty() }?.let { o.put("regionCode", it); regionCode = it }
            a.dateText?.takeIf { it.isNotEmpty() }?.let { o.put("date", it) }
            arr.put(o)
        }
        val body = JSONObject()
            .put("categoryId", categoryId)
            .put("answers", arr)
            .apply {
                region?.let { put("region", it) }
                regionCode?.let { put("regionCode", it) }
                targetTeacherId.toIntOrNull()?.let { put("targetTeacherId", it) }
            }

        val req = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/api/quotes")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { OkHttpClient().newCall(req).execute().use { it.isSuccessful } }.getOrDefault(false)
            }
            onDone(ok)
        }
    }
}

/**
 * 이탈 방지 바텀시트 — iOS `QuoteWizardExitSheet` 1:1(딜레이/애니메이션 없이 즉시 표시).
 *  딤 40% + 흰 시트(상단 라운드 24) / 제목 20sp bold / 부제 15sp regular 가운데 / 버튼 48 높이.
 */
@Composable
private fun QuoteExitSheet(onStop: () -> Unit, onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66000000))   // iOS: 검정 0.4
            .clickable { onContinue() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(QuoteColors.white)
                .clickable(enabled = false) {}
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                "거의 다 왔어요!",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                color = QuoteColors.c101116,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "지금 한 번만 작성하면\n평균 4개 이상의 견적을 받을 수 있어요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp,
                color = QuoteColors.c6D6E71, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuotePrimaryButton("그만하기", Modifier.weight(1f), filled = false, onClick = onStop)
                QuotePrimaryButton("계속 작성하기", Modifier.weight(1f), filled = true, onClick = onContinue)
            }
        }
    }
}
