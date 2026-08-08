package com.muyeon.app.ui.academy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringList
import com.muyeon.app.ui.quote.stringOrNull
import com.muyeon.app.ui.resume.ResumeOptions
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 학원(원장) 공개 프로필 — iOS `Academy/AcademyProfileView.swift` 1:1.
 *  `GET /api/academies/:id` 열람 전용(CTA 없음). 채용공고 학원명 탭(openAcademyProfile)으로 진입.
 */
data class AcademyJob(val id: Int, val title: String?, val genre: String?, val region: String?, val fields: List<String>?) {
    companion object {
        fun from(o: JSONObject) = AcademyJob(
            o.optInt("id"), o.stringOrNull("title"), o.stringOrNull("genre"),
            o.stringOrNull("region"), o.stringList("fields"),
        )
    }
}

data class AcademyProfile(
    val id: Int,
    val academyName: String?,
    val academyIntro: String?,
    val wantGenres: List<String>?,
    val wantFields: List<String>?,
    val region: String?,
    val image: String?,
    val openJobCount: Int?,
    val openJobs: List<AcademyJob>?,
) {
    val displayName: String get() = academyName?.trim()?.takeIf { it.isNotEmpty() } ?: "학원"

    companion object {
        fun from(o: JSONObject) = AcademyProfile(
            o.optInt("id"), o.stringOrNull("academyName"), o.stringOrNull("academyIntro"),
            o.stringList("wantGenres"), o.stringList("wantFields"), o.stringOrNull("region"),
            o.stringOrNull("image"), o.intOrNull("openJobCount"),
            o.optJSONArray("openJobs")?.map(AcademyJob::from),
        )
    }
}

private suspend fun fetchAcademy(token: String?, id: Int): AcademyProfile? = withContext(Dispatchers.IO) {
    runCatching {
        val req = Request.Builder().url("${BuildConfig.API_BASE_URL}/api/academies/$id")
            .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
            .build()
        OkHttpClient().newCall(req).execute().use { res ->
            if (!res.isSuccessful) null else AcademyProfile.from(JSONObject(res.body?.string().orEmpty()))
        }
    }.getOrNull()
}

/** `/` 로 시작하는 상대경로만 baseURL 접두 — 절대 URL 이중접두 방지(CLAUDE.md 절대 규칙). */
private fun absolutize(p: String) = if (p.startsWith("/")) BuildConfig.API_BASE_URL + p else p

@Composable
fun AcademyProfileScreen(token: String?, academyId: Int, onClose: () -> Unit, onOpenJob: (Int) -> Unit) {
    var profile by remember { mutableStateOf<AcademyProfile?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(academyId) {
        profile = fetchAcademy(token, academyId)
        loading = false
    }

    Box(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            profile == null -> Column(
                Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Apartment, null, Modifier.size(40.dp), tint = cAFAFAF)
                Spacer(Modifier.height(8.dp))
                Text(
                    "학원 정보를 불러올 수 없습니다.",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textSub,
                )
            }
            else -> AcademyBody(profile!!, onOpenJob)
        }

        // 히어로 위에 떠 있는 뒤로가기(iOS overlay(alignment: .topLeading))
        Box(
            Modifier.padding(start = 16.dp, top = 8.dp).size(34.dp)
                .shadow(2.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                .clickable { onClose() },
            Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", Modifier.size(18.dp), tint = MuyeonColors.body)
        }
    }
}

@Composable
private fun AcademyBody(p: AcademyProfile, onOpenJob: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 히어로 — 로고 있으면 240dp 이미지, 없으면 200dp 플레이스홀더
        Box(Modifier.fillMaxWidth(), Alignment.BottomStart) {
            val img = p.image?.takeIf { it.isNotEmpty() }
            if (img != null) {
                AsyncImage(
                    model = absolutize(img), contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(240.dp).background(cF7F7F7),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxWidth().height(200.dp).background(cF7F7F7), Alignment.Center) {
                    Icon(Icons.Default.Apartment, null, Modifier.size(60.dp), tint = cAFAFAF)
                }
            }
            p.region?.takeIf { it.isNotEmpty() }?.let { r ->
                Text(
                    r,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    lineHeight = 14.sp, color = Color.White,
                    modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(
                Modifier.padding(top = 18.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    p.displayName,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    lineHeight = 26.sp, color = MuyeonColors.textHead,
                )
                p.openJobCount?.takeIf { it > 0 }?.let { cnt ->
                    Text(
                        "모집중 $cnt",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        lineHeight = 14.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            p.wantGenres?.takeIf { it.isNotEmpty() }?.let { g ->
                AcademySection("찾는 강사 장르") { AcademyChips(g) }
            }
            p.wantFields?.takeIf { it.isNotEmpty() }?.let { f ->
                AcademySection("찾는 분야") { AcademyChips(f.map { ResumeOptions.fieldLabel(it) }) }
            }
            p.academyIntro?.takeIf { it.isNotEmpty() }?.let { intro ->
                AcademySection("학원 소개") {
                    Text(
                        intro,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        lineHeight = 21.sp, color = MuyeonColors.body, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            p.openJobs?.takeIf { it.isNotEmpty() }?.let { jobs ->
                AcademySection("모집중 공고") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        jobs.forEach { JobCard(it, onOpenJob) }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun JobCard(j: AcademyJob, onOpenJob: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
            .clickable { onOpenJob(j.id) }.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                j.title ?: "공고",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    j.genre,
                    j.fields?.take(2)?.joinToString(", ") { ResumeOptions.fieldLabel(it) },
                    j.region,
                ).filter { it.isNotEmpty() }.joinToString(" · "),
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = cAFAFAF)
    }
}

@Composable
private fun AcademySection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}

/** iOS AcademyFlexWrap — 3개씩 줄바꿈. */
@Composable
private fun AcademyChips(labels: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    Text(
                        label,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

private val cAFAFAF = Color(0xFFAFAFAF)
private val cF7F7F7 = Color(0xFFF7F7F7)

/** 웹 `openAcademyProfile` 브릿지 진입점. */
class AcademyProfileActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ID = "academyId"

        fun start(context: Context, academyId: Int) {
            val i = Intent(context, AcademyProfileActivity::class.java).putExtra(EXTRA_ID, academyId)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(EXTRA_ID, 0)
        setContent {
            val token = remember { TokenManager.getAccessToken(this) }
            AcademyProfileScreen(
                token, id,
                onClose = { finish() },
                // 공고 상세는 아직 웹 — 네이티브 승격 전까지 웹 경로 폴백.
                onOpenJob = { jobId ->
                    com.muyeon.app.webview.NativeWebRoute.openWebAndFinish(this, "/job/$jobId")
                },
            )
        }
    }
}
