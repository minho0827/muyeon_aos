package com.muyeon.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.domain.usecase.account.LessonGenresUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class LessonGenresUiState(
    val genres: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val saving: Boolean = false,
    /** 결과 팝업 — 닫으면 화면도 닫는다(웹과 같다). */
    val result: Int? = null,
)

/** 레슨 전공 설정 — 웹 pages/muyeon/my/LessonGenreSettings.js. */
class LessonGenresViewModel(private val genresUseCase: LessonGenresUseCase) : ViewModel() {
    private val _state = MutableStateFlow(LessonGenresUiState())
    val state: StateFlow<LessonGenresUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = genresUseCase.load()) {
                is ApiResult.Success -> _state.update { it.copy(genres = r.data.toSet(), isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun toggle(g: String) = _state.update { it.copy(genres = if (g in it.genres) it.genres - g else it.genres + g) }

    fun save() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val ok = genresUseCase.save(LessonGenres.ALL.filter { it in _state.value.genres }) is ApiResult.Success
            _state.update { it.copy(saving = false, result = if (ok) R.string.lesson_genres_saved else R.string.lesson_genres_failed) }
        }
    }
}

/** 레슨 전공 설정 화면 — 고른 전공에 맞는 레슨 견적요청을 받는다. 저장·실패 팝업을 닫으면 화면을 닫는다(웹과 같다). */
@Composable
fun LessonGenresScreen(onClose: () -> Unit, viewModel: LessonGenresViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.lesson_genres_title), primary = false, onBack = onClose)
        when {
            s.isLoading -> Unit
            s.errorMessage != null -> EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> {
                Column(Modifier.weight(1f).padding(Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL)) {
                    Text(stringResource(R.string.lesson_genres_desc), style = AppTypography.Body14, color = AppColors.TextBody)
                    FormField(stringResource(R.string.lesson_genres_label), hint = stringResource(R.string.lesson_genres_multi)) {
                        FormChipWrap(LessonGenres.ALL.map { it to it }, { it in s.genres }, viewModel::toggle)
                    }
                }
                MuyeonBottomButton(stringResource(R.string.common_save), "lessonGenres.save", loading = s.saving, onClick = viewModel::save)
            }
        }
    }
    s.result?.let { r ->
        CustomDialog(
            title = stringResource(r), content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
            buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = onClose, onRightButtonClick = onClose, showPopup = true,
        )
    }
}
