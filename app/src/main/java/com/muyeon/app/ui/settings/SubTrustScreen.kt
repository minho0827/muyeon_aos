package com.muyeon.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ReadField
import com.muyeon.app.ui.components.ReadSectionTitle
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.SubTrust
import com.muyeon.domain.usecase.account.GetSubTrustUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class SubTrustUiState(val data: SubTrust? = null, val isLoading: Boolean = true, val errorMessage: String? = null)

/** 대타 신뢰도 — 웹 pages/muyeon/my/SubTrust.js(GET /me/sub-trust). */
class SubTrustViewModel(private val get: GetSubTrustUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SubTrustUiState())
    val state: StateFlow<SubTrustUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = get()) {
                is ApiResult.Success -> _state.update { it.copy(data = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }
}

/** 대타 신뢰도 화면 — 최근 90일 점수·확정 취소·이용 정지 · 점수 기준 · 내역. */
@Composable
fun SubTrustScreen(onBack: () -> Unit, viewModel: SubTrustViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.settings_sub_trust), primary = false, onBack = onBack)
        val d = s.data
        when {
            s.isLoading -> Unit
            d == null -> EmptyView(stringResource(R.string.subtrust_failed), content = s.errorMessage,
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
            ) {
                val restricted = d.restrictedUntil?.takeIf { (KstDates.parse(it)?.toEpochMilli() ?: 0) > System.currentTimeMillis() }
                ReadSectionTitle(stringResource(R.string.subtrust_sec_90d))
                ReadField(stringResource(R.string.subtrust_points), stringResource(R.string.subtrust_points_value, d.points90d))
                ReadField(stringResource(R.string.subtrust_cancels), stringResource(R.string.subtrust_cancels_value, d.cancels90d))
                ReadField(stringResource(R.string.subtrust_dispatch),
                    restricted?.let { stringResource(R.string.subtrust_restricted, KstDates.dotTime(it)) } ?: stringResource(R.string.subtrust_normal))
                Text(stringResource(R.string.subtrust_visible_note), style = AppTypography.Sub13, color = AppColors.TextSub)
                ReadSectionTitle(stringResource(R.string.subtrust_sec_rules))
                stringArrayResource(R.array.subtrust_rules).forEach { Text(it, style = AppTypography.Sub13, color = AppColors.TextSub) }
                ReadSectionTitle(stringResource(R.string.subtrust_sec_history))
                if (d.history.isEmpty()) Text(stringResource(R.string.subtrust_history_empty), style = AppTypography.Body14, color = AppColors.TextSub)
                d.history.forEach { h ->
                    val side = when (h.side) { "TEACHER" -> stringResource(R.string.subtrust_side_teacher); "POSTER" -> stringResource(R.string.subtrust_side_poster); else -> "" }
                    val reason = stringResource(if (h.noShow) R.string.subtrust_noshow else R.string.subtrust_cancel)
                    ReadField(
                        "${KstDates.dot(h.createdAt)} · ${h.postingTitle ?: stringResource(R.string.subtrust_posting)}",
                        listOfNotNull(
                            "$side $reason".trim(),
                            if (h.revoked) stringResource(R.string.subtrust_revoked, h.points) else stringResource(R.string.subtrust_plus, h.points),
                            if (!h.revoked && h.expiresAt != null) stringResource(R.string.subtrust_expires, KstDates.dot(h.expiresAt)) else null,
                        ).joinToString(" · "),
                    )
                }
            }
        }
    }
}
