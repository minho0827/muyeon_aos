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
import com.muyeon.app.webview.NativeWebRoute
import com.muyeon.app.webview.WebCallbacks
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
        // '같은 조건으로 견적 요청' 프리필 — class/age/format 옵션 id 와 지역(iOS prefill).
        private const val EXTRA_PREFILL = "prefill"
        private const val EXTRA_REGION = "region"
        private const val EXTRA_REGION_CODE = "regionCode"
        // 인트로 최초 1회 노출 기록(기기 기준) — iOS UserDefaults "muyeon.quoteIntroSeen" 대응.
        private const val QUOTE_PREFS = "muyeon.quote"
        private const val KEY_INTRO_SEEN = "muyeon.quoteIntroSeen"

        fun start(
            context: Context,
            categoryId: String?,
            targetTeacherId: String?,
            /** 문진 프리필 — {"class":[...],"age":[...],"format":[...]} JSON. */
            prefillJson: String? = null,
            region: String? = null,
            regionCode: String? = null,
        ) {
            val i = Intent(context, QuoteWizardActivity::class.java)
                .putExtra(EXTRA_CATEGORY, categoryId ?: "")
                .putExtra(EXTRA_TARGET_TEACHER, targetTeacherId ?: "")
                .putExtra(EXTRA_PREFILL, prefillJson ?: "")
                .putExtra(EXTRA_REGION, region ?: "")
                .putExtra(EXTRA_REGION_CODE, regionCode ?: "")
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val targetTeacherId = intent.getStringExtra(EXTRA_TARGET_TEACHER).orEmpty()
        val seed = seedAnswersOf(
            intent.getStringExtra(EXTRA_PREFILL).orEmpty(),
            intent.getStringExtra(EXTRA_REGION).orEmpty(),
            intent.getStringExtra(EXTRA_REGION_CODE).orEmpty(),
        )

        setContent {
            // 최초 1회 인트로("강사를 찾고 계신가요?") — 종류 선택으로 바로 떨어지는 갑작스러움 해소.
            //  강사 지정(1:1) 요청은 프로필 맥락이 있어 스킵(iOS presentQuoteWizard 규칙 동일).
            val prefs = remember { getSharedPreferences(QUOTE_PREFS, MODE_PRIVATE) }
            var showIntro by remember {
                mutableStateOf(targetTeacherId.isEmpty() && !prefs.getBoolean(KEY_INTRO_SEEN, false))
            }
            // 카테고리 미지정(웹 "새 견적 요청하기")이면 종류 선택부터 — iOS presentQuoteCategory 대응.
            var selected by remember { mutableStateOf(QuoteCategory.find(categoryId)) }
            val cat = selected

            when {
                showIntro -> QuoteIntroScreen(
                    onStart = { prefs.edit().putBoolean(KEY_INTRO_SEEN, true).apply(); showIntro = false },
                    onClose = { finish() },
                )
                cat == null -> QuoteCategoryScreen(onSelect = { selected = it }, onClose = { finish() })
                else -> WizardFlow(cat, targetTeacherId, seed)
            }
        }
    }

    /** 종류가 정해진 뒤의 문진 → 로딩 → 완료 흐름. */
    @Composable
    private fun WizardFlow(
        category: QuoteCategory,
        targetTeacherId: String,
        seedAnswers: Map<String, QuoteAnswer>,
    ) {
            val vm = remember(category.id) { QuoteWizardViewModel(category, seedAnswers) }
            var showExit by remember { mutableStateOf(false) }
            // 단계: wizard → loading(매칭 로딩) → done(완료 안내). iOS 흐름 동일.
            var phase by remember { mutableStateOf("wizard") }
            var submitOk by remember { mutableStateOf<Boolean?>(null) }

            // 이탈시트 추천 콘텐츠 — 시트가 뜨기 전에 미리 로드(iOS 프리페치와 동일: 통째로 한번에 노출).
            var exitLessons by remember { mutableStateOf<List<LessonContentItem>>(emptyList()) }
            val genre = category.title.replace(" 레슨", "")
            val regionCode = vm.answers.values.firstOrNull { !it.regionCode.isNullOrEmpty() }?.regionCode
            LaunchedEffect(genre, regionCode) {
                exitLessons = LessonContentRepo.loadForExit(
                    token = TokenManager.getAccessToken(this@QuoteWizardActivity),
                    genre = genre,
                    regionCode = regionCode,
                )
            }

            when (phase) {
              "loading" -> {
                QuoteSubmitLoadingScreen(
                    categoryTitle = category.title,
                    isDirect = targetTeacherId.isNotEmpty(),
                    onDone = {
                        // 로딩(≈2.8s)과 제출을 병렬 진행 — 실패면 안내 후 위저드 복귀(iOS 는 완료로 마감).
                        if (submitOk == false) {
                            Toast.makeText(this@QuoteWizardActivity, "요청에 실패했어요. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                            phase = "wizard"
                        } else {
                            // 웹이 받은견적 목록으로 라우팅한다(iOS notifyWebQuoteSubmitted).
                            WebCallbacks.quoteSubmitted(this@QuoteWizardActivity)
                            phase = "done"
                        }
                    },
                )
              }
              "done" -> {
                QuoteSubmittedScreen(
                    onViewQuotes = { openWebAndFinish("/myQuotes") },
                    onGoHome = { openWebAndFinish("/home") },
                    onClose = { finish() },
                )
              }
              else -> {
            Box(Modifier.fillMaxSize()) {
                QuoteWizardScreen(
                    vm = vm,
                    token = TokenManager.getAccessToken(this@QuoteWizardActivity),
                    onClose = {
                        // 문진 진행 중이면 이탈 방지 시트, 첫 질문이면 바로 닫기(iOS 동일).
                        if (vm.currentIndex > 0) showExit = true else finish()
                    },
                    onComplete = { answers ->
                        phase = "loading"
                        submit(answers, category.id, targetTeacherId) { ok -> submitOk = ok }
                    },
                )
                if (showExit) {
                    QuoteExitSheet(
                        lessons = exitLessons,
                        onStop = { finish() },
                        onContinue = { showExit = false },
                        // 둘러보기·콘텐츠 상세 네이티브(iOS presentLessonBrowse / presentLessonContentDetail).
                        onSeeAll = {
                            com.muyeon.app.ui.lesson.LessonBrowseActivity.startBrowse(this@QuoteWizardActivity)
                        },
                        onSelectLesson = { id ->
                            com.muyeon.app.ui.lesson.LessonBrowseActivity.startDetail(this@QuoteWizardActivity, id)
                        },
                    )
                }
            }
              }
            }
    }

    /** 완료 화면 CTA — 웹 화면으로 이동하고 위저드 종료. */
    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)

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
private fun QuoteExitSheet(
    lessons: List<LessonContentItem> = emptyList(),
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onSeeAll: () -> Unit = {},
    onSelectLesson: (Int) -> Unit = {},
) {
    val dimSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val sheetSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66000000))   // iOS: 검정 0.4 (color000000 opacity 0.4)
            // 딤 탭 → 계속 작성(iOS onTapGesture 동일). ripple 없음.
            .clickable(interactionSource = dimSource, indication = null) { onContinue() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(QuoteColors.white)
                .clickable(interactionSource = sheetSource, indication = null) { /* 시트 탭 소비 */ }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuotePrimaryButton("그만하기", Modifier.weight(1f), filled = false, onClick = onStop)
                QuotePrimaryButton("계속 작성하기", Modifier.weight(1f), filled = true, onClick = onContinue)
            }
            if (lessons.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                androidx.compose.material3.Divider(color = QuoteColors.cEAEAEA)
                Spacer(Modifier.height(20.dp))
                ExitRecommendSection(lessons = lessons, onSeeAll = onSeeAll, onSelectLesson = onSelectLesson)
            }
        }
    }
}

/**
 * 서버 quote-prefill 응답 → 문진 시드 답변.
 *  키(class/age/format)와 옵션 id 체계가 문진과 같아 그대로 얹는다(iOS 와 동일한 전제).
 *  지역은 별도 문항이라 region/regionCode 를 그 문항에 넣는다.
 */
private fun seedAnswersOf(prefillJson: String, region: String, regionCode: String): Map<String, QuoteAnswer> {
    val out = mutableMapOf<String, QuoteAnswer>()
    if (prefillJson.isNotEmpty()) {
        runCatching {
            val o = org.json.JSONObject(prefillJson)
            o.keys().forEach { k ->
                val arr = o.optJSONArray(k) ?: return@forEach
                var ids = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }
                // age 는 단일 선택 문항인데 서버는 배열로 준다 — 첫 값만 얹는다(복수면 UI 가 깨진다).
                if (k == "age") ids = ids.take(1)
                if (ids.isNotEmpty()) out[k] = QuoteAnswer(questionId = k, selectedOptionIds = ids)
            }
        }
    }
    if (region.isNotEmpty()) {
        out["region"] = QuoteAnswer(questionId = "region", region = region, regionCode = regionCode.ifEmpty { null })
    }
    return out
}
