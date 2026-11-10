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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.review.LessonQuotePrefill
import com.muyeon.app.ui.lesson.components.LessonDetailLikeButton
import com.muyeon.app.ui.lesson.components.LessonDetailRatingBadge
import com.muyeon.app.ui.lesson.components.LessonDetailReviews
import com.muyeon.app.ui.review.ReviewApi
import com.muyeon.app.ui.review.ReviewList
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.json.JSONArray
import java.util.Locale

/**
 * 레슨 콘텐츠 상세 — iOS `LessonContentDetailView.swift` 대응.
 *  대표 이미지 캐러셀 · 제목 · 강사 · 레슨 정보(웹 LessonDetail.js 와 같은 칸·순서·용어) · 장소 · 소개 · 상세 이미지 · 리뷰·평점 · 비슷한 콘텐츠.
 *  ★ 2026-11-08 리뷰·평점 = 웹처럼 개설 강사 후기 재활용(components/LessonDetailReviews, 소개·상세 이미지 다음). 강사 줄 이름 옆 ★평균(개수).
 *  ★ 2026-11-08 아래 버튼 = [lessonDetailCta](어디서 열었는지 [source] × 레슨 상태). 예약은 기존 예약 화면(LessonActivity booking).
 *   레슨 상품 가격은 '수업료'(웹·예약 시트 용어) — '예산'은 견적 요청에서 학생이 적는 희망 금액이라 쓰지 않는다.
 */
@Composable
fun LessonContentDetailScreen(
    api: LessonBrowseApi,
    lessonProductId: Int,
    onClose: () -> Unit,
    onSelectLesson: (Int) -> Unit,
    onSelectTeacher: (Int) -> Unit,
    /** 값이 바뀌면 다시 읽는다(강사 프로필에서 후기 작성 후 복귀 — REVIEWS). 조회수는 첫 진입만 센다. */
    reloadSignal: Int = 0,
    source: LessonDetailSource = LessonDetailSource.BROWSE,
    /** 로그인한 내 id(없으면 비회원) — 내 레슨이면 관리 버튼, 비회원이 예약·요청을 누르면 [onLogin]. */
    myUserId: Int? = null,
    onLogin: () -> Unit = {},
    /** 레슨 예약하기 → 기존 예약 화면(회차·인원·예약금). */
    onBook: (Int) -> Unit = {},
    /** 내가 개설한 레슨 · 관리. */
    onManage: () -> Unit = {},
    /** 같은 조건 견적 / 이 강사님께 레슨 요청 → 문진 위저드(프리필). */
    onRequestQuote: (LessonQuotePrefill) -> Unit = {},
    /** 리뷰·평점 칸(개설 강사 후기, GET /teachers/:id/reviews). 없으면 칸을 그리지 않는다. */
    reviewApi: ReviewApi? = null,
    /** 후기 → 기존 후기 상세. */
    onSelectReview: (Int) -> Unit = {},
    /** [후기 전체 보기] → 기존 강사 후기 목록(teacherId). */
    onViewAllReviews: (Int) -> Unit = {},
    /** [후기 작성하기] → 기존 후기 작성(teacherId, 강사 이름). */
    onWriteReview: (Int, String?) -> Unit = { _, _ -> },
) {
    var detail by remember { mutableStateOf<LessonContentDetail?>(null) }
    var similar by remember { mutableStateOf<List<BrowseFeedItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var slotCount by remember { mutableStateOf<Int?>(null) }
    var requesting by remember { mutableStateOf(false) }
    var reviews by remember { mutableStateOf<ReviewList?>(null) }
    // 후기 작성·수정 화면에서 돌아오면 후기만 다시 읽는다(첫 진입 resume 은 건너뛴다).
    var reviewsResume by remember { mutableIntStateOf(0) }
    var resumedOnce by remember { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (resumedOnce) reviewsResume++ else resumedOnce = true
        onPauseOrDispose { }
    }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val toggleScrap: ToggleScrapUseCase = koinInject()
    val getLikedIds: GetLikedIdsUseCase = koinInject()
    var liked by remember { mutableStateOf(false) }
    var likeBusy by remember { mutableStateOf(false) }
    // 계정이 바뀌면(로그아웃·다른 계정) 하트를 끄고 다시 읽는다.
    LaunchedEffect(lessonProductId, com.muyeon.app.auth.accountEpoch()) { liked = false; liked = lessonProductId.toLong() in getLikedIds(ScrapTarget.LESSON) }

    LaunchedEffect(lessonProductId, reloadSignal) {
        loading = detail == null
        // view=1 은 첫 진입에만 — 재조회 때마다 조회수가 늘면 통계가 부풀려진다.
        api.detail(lessonProductId, countView = detail == null).onSuccess { detail = it }
        // 예약할 회차가 있는지 — 웹·앱 예약 시트와 같은 공개 API(available-slots). 버튼을 고르는 데만 쓴다.
        slotCount = api.availableSlotCount(lessonProductId)
        loading = false
        similar = api.similar(lessonProductId)
    }
    val creatorId = detail?.creatorId?.takeIf { it > 0 }
    LaunchedEffect(creatorId, reloadSignal, reviewsResume) {
        val tid = creatorId ?: return@LaunchedEffect
        reviewApi?.list(tid)?.onSuccess { reviews = it }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = stringResource(R.string.lesson_detail_title), onBack = onClose)

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
                                placeholder = com.muyeon.app.ui.components.imageLoadingPainter(),
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
                    lessonPriceText(d)?.let {
                        Text(
                            it,
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

                LessonInfoSections(d, source)

                d.creatorId?.takeIf { it > 0 }?.let { cid ->
                    HorizontalDivider(color = MuyeonColors.border)
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelectTeacher(cid) }.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuoteAvatar(d.creatorImage, d.creatorName ?: "강사", 44.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    d.creatorName ?: "강사",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                                )
                                // 웹 강사 줄과 같다 — 후기가 있으면 이름 옆 ★평균(개수).
                                LessonDetailRatingBadge(reviews?.avg, reviews?.count)
                            }
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
                            stringResource(R.string.lesson_detail_section_intro),
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
                                    placeholder = com.muyeon.app.ui.components.imageLoadingPainter(),
                            )
                        }
                    }
                }

                // 리뷰·평점 — 웹 LessonDetail.js 처럼 레슨 본문(소개·상세 이미지) 다음. 후기가 없으면 빈 문구(웹과 같다).
                val rv = reviews
                if (creatorId != null && rv != null) {
                    LessonDetailReviews(
                        data = rv,
                        onSelectReview = onSelectReview,
                        onViewAll = { onViewAllReviews(creatorId) },
                        onWrite = { onWriteReview(creatorId, d.creatorName) },
                    )
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
                                            placeholder = com.muyeon.app.ui.components.imageLoadingPainter(),
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

        // ── 아래 버튼(어디서 열었는지 × 레슨 상태) ──
        if (d != null && !loading) {
            val cta = lessonDetailCta(source, d, myUserId, slotCount)
            fun request() {
                if (requesting) return
                if (myUserId == null) { onLogin(); return }
                // 무용수 유형은 레슨(견적) 요청 불가 — 프리필 조회 전에 안내(2026-09-26 정책, 리뷰 상세와 같다).
                if (!com.muyeon.app.webview.ActiveRole.allowLessonCustomer(ctx)) return
                requesting = true
                scope.launch {
                    api.quotePrefill(lessonProductId)?.let(onRequestQuote)
                    requesting = false
                }
            }
            when (cta) {
                LessonDetailCta.NONE -> CtaHint(stringResource(R.string.lesson_detail_not_bookable))
                LessonDetailCta.REQUEST -> CtaHint(stringResource(R.string.lesson_detail_request_hint))
                else -> Unit
            }
            // 찜 — 웹 LessonDetail(useLikes LESSON)·iOS 와 같다. 비회원이면 로그인.
            val heart: @Composable () -> Unit = {
                LessonDetailLikeButton(liked) {
                    if (likeBusy) return@LessonDetailLikeButton
                    if (myUserId == null) { onLogin(); return@LessonDetailLikeButton }
                    val was = liked
                    liked = !was; likeBusy = true
                    scope.launch {
                        when (val r = toggleScrap(ScrapTarget.LESSON, lessonProductId.toLong(), was)) {
                            is ToggleScrapResult.Done -> liked = r.liked
                            is ToggleScrapResult.Failed -> { liked = was; android.widget.Toast.makeText(ctx, r.message, android.widget.Toast.LENGTH_SHORT).show() }
                            ToggleScrapResult.LoginRequired -> { liked = was; onLogin() }
                        }
                        likeBusy = false
                    }
                }
            }
            when (cta) {
                LessonDetailCta.BOOK -> MuyeonBottomButton(
                    stringResource(R.string.lesson_detail_cta_book), "lessonDetail.book", leading = heart,
                ) { if (myUserId == null) onLogin() else onBook(lessonProductId) }
                LessonDetailCta.MANAGE -> MuyeonBottomButton(
                    stringResource(R.string.lesson_detail_cta_manage), "lessonDetail.manage", leading = heart, onClick = onManage,
                )
                LessonDetailCta.SAME_QUOTE -> MuyeonBottomButton(
                    stringResource(if (requesting) R.string.lesson_detail_cta_loading else R.string.lesson_detail_cta_same_quote),
                    "lessonDetail.sameQuote", loading = requesting, leading = heart, onClick = ::request,
                )
                LessonDetailCta.REQUEST -> MuyeonBottomButton(
                    stringResource(R.string.lesson_detail_cta_request),
                    "lessonDetail.request", loading = requesting, leading = heart, onClick = ::request,
                )
                LessonDetailCta.NONE -> Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun CtaHint(text: String) {
    HorizontalDivider(color = MuyeonColors.border)
    Text(
        text,
        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(start = 24.dp, end = 24.dp, top = 12.dp),
    )
}

/** 웹 LessonDetail.js 수업료 — 무료 / "N원 (1인당|예약 1건당 · 현장 결제)". 가격이 없으면 null(칸을 숨긴다). */
@Composable
private fun lessonPriceText(d: LessonContentDetail): String? {
    val p = d.price ?: return null
    if (p == 0) return stringResource(R.string.lesson_detail_price_free)
    val unit = stringResource(
        if (d.priceUnit == "PER_BOOKING") R.string.lesson_detail_price_per_booking else R.string.lesson_detail_price_per_person,
    )
    return stringResource(R.string.lesson_detail_price_value, String.format(Locale.KOREA, "%,d", p), unit)
}

/**
 * 웹 상세와 같은 '레슨 정보'·'장소' 칸(값이 있는 것만). 순서: 장르·난이도·유형·진행 일정(시간대)·수업 시간·정원·수업료 / 장소·지역.
 *  견적 추천에서 열면 견적 문진과 같은 조건(이용 목적·진행 방식·고객 연령대)을 덧붙인다 — 같은 조건으로 요청할 내용을 보여 주려고.
 */
@Composable
private fun LessonInfoSections(d: LessonContentDetail, source: LessonDetailSource) {
    val kind = when (d.lessonKind) {
        "REGULAR" -> stringResource(R.string.lesson_detail_kind_regular)
        "ONEDAY" -> stringResource(R.string.lesson_detail_kind_oneday)
        "EXPERIENCE" -> stringResource(R.string.lesson_detail_kind_experience)
        else -> null
    }
    val info = listOf(
        stringResource(R.string.lesson_detail_genre) to d.genre,
        stringResource(R.string.lesson_detail_level) to d.level,
        stringResource(R.string.lesson_detail_kind) to kind,
        stringResource(if (d.isDatedKind) R.string.lesson_detail_schedule_dated else R.string.lesson_detail_schedule) to d.scheduleText,
        stringResource(R.string.lesson_detail_duration) to d.duration?.takeIf { it > 0 }?.let { stringResource(R.string.lesson_detail_duration_value, it) },
        stringResource(R.string.lesson_detail_capacity) to d.maxParticipants?.takeIf { it > 0 }?.let { stringResource(R.string.lesson_detail_capacity_value, it) },
        stringResource(R.string.lesson_detail_price) to lessonPriceText(d),
    ) + if (source == LessonDetailSource.QUOTE_RECOMMEND) listOf(
        stringResource(R.string.lesson_detail_purposes) to d.purposes?.takeIf { it.isNotEmpty() }?.joinToString(", ") { id ->
            com.muyeon.app.ui.quote.QuoteCategory.find(d.genre)?.classOptions?.firstOrNull { it.id == id }?.label ?: id
        },
        stringResource(R.string.lesson_detail_formats) to d.formats?.takeIf { it.isNotEmpty() }?.joinToString(", ") { id ->
            QuotePrefsOptions.formats.firstOrNull { it.first == id }?.second ?: id
        },
        stringResource(R.string.lesson_detail_ages) to d.ageGroups?.takeIf { it.isNotEmpty() }?.joinToString(", ") { id ->
            LessonBrowseOptions.ages.firstOrNull { it.first == id }?.second ?: id
        },
    ) else emptyList()
    val place = listOf(
        stringResource(R.string.lesson_detail_place) to d.place,
        stringResource(R.string.lesson_detail_region) to d.region,
    )
    InfoBlock(stringResource(R.string.lesson_detail_section_info), info)
    InfoBlock(stringResource(R.string.lesson_detail_section_place), place)
}

@Composable
private fun InfoBlock(title: String, rows: List<Pair<String, String?>>) {
    val shown = rows.filter { !it.second.isNullOrBlank() }
    if (shown.isEmpty()) return
    HorizontalDivider(color = MuyeonColors.border)
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        shown.forEach { (k, v) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    k,
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textSub, modifier = Modifier.width(72.dp),
                )
                Text(
                    v.orEmpty(),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                )
            }
        }
    }
}
