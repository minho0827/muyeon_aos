package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import org.json.JSONArray
import java.util.Locale

/**
 * 레슨 콘텐츠 상세 — iOS `LessonContentDetailView.swift` 대응.
 *  대표 이미지 캐러셀 · 제목/장르/지역/가격 · 강사 · 소개 · 상세 이미지 · 비슷한 콘텐츠.
 */
@Composable
fun LessonContentDetailScreen(
    api: LessonBrowseApi,
    lessonProductId: Int,
    onClose: () -> Unit,
    onSelectLesson: (Int) -> Unit,
    onSelectTeacher: (Int) -> Unit,
) {
    var detail by remember { mutableStateOf<LessonContentDetail?>(null) }
    var similar by remember { mutableStateOf<List<BrowseFeedItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val ctx = LocalContext.current

    LaunchedEffect(lessonProductId) {
        loading = true
        // view=1 은 첫 진입에만 — 재조회 때마다 조회수가 늘면 통계가 부풀려진다.
        api.detail(lessonProductId, countView = true).onSuccess { detail = it }
        similar = api.similar(lessonProductId)
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "레슨 상세", onBack = onClose)

        val d = detail
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            d == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    "레슨을 불러오지 못했어요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            }
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val images = d.images ?: emptyList()
                if (images.isNotEmpty()) {
                    val pager = rememberPagerState { images.size }
                    Box(Modifier.fillMaxWidth().height(260.dp).background(Color(0xFFF2F2F7))) {
                        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                            AsyncImage(
                                QuoteUi.imageUrl(images[i]), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clickable {
                                    ImageViewerActivity.start(
                                        ctx, JSONArray(images).toString(), pager.currentPage, false,
                                    )
                                },
                            )
                        }
                        if (images.size > 1) {
                            Text(
                                "${pager.currentPage + 1} / ${images.size}",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                                lineHeight = 15.sp, color = Color.White,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        listOfNotNull(d.genre, d.region).joinToString(" · "),
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        lineHeight = 15.sp, color = MuyeonColors.primary,
                    )
                    Text(
                        d.title,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        lineHeight = 28.sp, color = MuyeonColors.textHead,
                    )
                    d.price?.takeIf { it > 0 }?.let {
                        Text(
                            "${String.format(Locale.KOREA, "%,d", it)}원",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            lineHeight = 22.sp, color = MuyeonColors.textHead,
                        )
                    }
                    d.viewCount?.takeIf { it > 0 }?.let {
                        Text(
                            "조회 ${it}회",
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                            color = MuyeonColors.chevron,
                        )
                    }
                }

                d.creatorId?.takeIf { it > 0 }?.let { cid ->
                    HorizontalDivider(color = MuyeonColors.border)
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelectTeacher(cid) }.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuoteAvatar(d.creatorImage, d.creatorName ?: "강사", 44.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                d.creatorName ?: "강사",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                lineHeight = 18.sp, color = MuyeonColors.textHead,
                            )
                            Text(
                                "프로필 보기",
                                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                                color = MuyeonColors.textSub,
                            )
                        }
                        Text("›", fontFamily = customFontFamily, fontSize = 16.sp, color = MuyeonColors.chevron)
                    }
                }

                d.description?.takeIf { it.isNotEmpty() }?.let {
                    HorizontalDivider(color = MuyeonColors.border)
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "레슨 소개",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            it,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            lineHeight = 24.sp, color = MuyeonColors.body,
                        )
                    }
                }

                d.detailImages?.takeIf { it.isNotEmpty() }?.let { detailImages ->
                    HorizontalDivider(color = MuyeonColors.border)
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "상세 이미지",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        detailImages.forEachIndexed { i, img ->
                            AsyncImage(
                                QuoteUi.imageUrl(img), null, contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF2F2F7))
                                    .clickable {
                                        ImageViewerActivity.start(
                                            ctx, JSONArray(detailImages).toString(), i, false,
                                        )
                                    },
                            )
                        }
                    }
                }

                if (similar.isNotEmpty()) {
                    HorizontalDivider(color = MuyeonColors.border)
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "비슷한 콘텐츠",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            similar.forEach { s ->
                                Column(
                                    Modifier.width(150.dp).clickable { onSelectLesson(s.itemId) },
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    AsyncImage(
                                        QuoteUi.imageUrl(s.thumbnail), null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF2F2F7)),
                                    )
                                    Text(
                                        s.cardTitle,
                                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp, lineHeight = 18.sp, color = MuyeonColors.textHead,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
