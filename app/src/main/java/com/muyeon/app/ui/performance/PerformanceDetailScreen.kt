package com.muyeon.app.ui.performance

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.ImageCarousel
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.PrimaryBottomButton
import com.muyeon.app.ui.performance.components.PerformanceDetailHead
import com.muyeon.app.ui.performance.components.PerformanceDetailImages
import com.muyeon.app.ui.performance.components.PerformanceInfoSection
import com.muyeon.app.ui.performance.components.PerformanceSourceSection
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import org.json.JSONArray
import org.koin.androidx.compose.koinViewModel

/** 공연 상세 — 흰 위 바 "공연 상세"(뒤로) · 사진 · 머리 · 정보 · [예매하러 가기](바깥 브라우저). */
@Composable
fun PerformanceDetailScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    viewModel: PerformanceDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is PerformanceListEvent.ScrapToast -> Toast.makeText(context, if (e.added) R.string.common_scrap_added else R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is PerformanceListEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                PerformanceListEvent.LoginRequired -> onLogin()
                PerformanceListEvent.ScrollToTop -> Unit
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        MainTopBar(title = stringResource(R.string.performance_detail_title), primary = false, onBack = onBack)
        val d = state.detail
        when {
            state.isLoading && d == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary) }
            d == null -> EmptyView(title = stringResource(R.string.performance_detail_not_found))
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    val images = d.images.ifEmpty { listOfNotNull(d.summary.posterUrl) }
                    ImageCarousel(
                        images = images,
                        fallbackRes = R.drawable.img_noimage_performance,
                        onOpen = { i -> ImageViewerActivity.start(context, JSONArray(images.map { com.muyeon.app.ui.format.ImageUrls.resolve(it) }).toString(), i, allowSave = false) },
                    )
                    PerformanceDetailHead(d, liked = state.liked, onToggleLike = viewModel::toggleLike)
                    if (d.detailType == "IMAGE" && d.detailImages.isNotEmpty()) PerformanceDetailImages(d.detailImages)
                    else {
                        PerformanceInfoSection(d)
                        Spacer(Modifier.height(Dimensions.spacingXXL))
                        PerformanceSourceSection(d, onOpenUrl = openUrl)
                    }
                    Spacer(Modifier.height(Dimensions.spacing3XL))
                }
                d.bookingUrl?.let { url -> PrimaryBottomButton(stringResource(R.string.performance_detail_booking), onClick = { openUrl(url) }) }
            }
        }
    }
}
