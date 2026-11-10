package com.muyeon.app.ui.credential

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.UploadImagePreparer
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ReadSectionTitle
import com.muyeon.app.ui.credential.components.CredentialCard
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.credential.CredentialDocument
import com.muyeon.domain.usecase.credential.CredentialsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

/** 경력·자격 확인 — 웹 /my/credentials. 본인인증 안 했으면 [개인정보 수정]으로(앱 화면). */
@Composable
fun CredentialsScreen(onBack: () -> Unit, onProfileEdit: () -> Unit, viewModel: CredentialsViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 사진·PDF 여러 장(웹 accept="image/*,application/pdf" multiple).
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addFiles(uris.map { PickedDoc(it.toString(), context.contentResolver.getType(it) == PDF) })
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.credential_title), primary = false, onBack = onBack)
        val d = s.data
        when {
            s.isLoading -> Unit
            d == null -> EmptyView(stringResource(R.string.credential_load_failed), content = s.errorMessage,
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
            ) {
                Text(stringResource(R.string.credential_intro), style = AppTypography.Body14, color = AppColors.TextBody)
                ReadSectionTitle(stringResource(R.string.credential_sec_identity))
                if (d.identityVerified) Text(stringResource(R.string.credential_identity_done), style = AppTypography.Body14, color = AppColors.Positive)
                else Text(stringResource(R.string.credential_identity_none), style = AppTypography.Body14, color = AppColors.Primary,
                    modifier = Modifier.clickable(onClick = onProfileEdit).padding(vertical = Dimensions.spacingXXS))
                ReadSectionTitle(stringResource(R.string.credential_sec_items))
                if (d.unsortedPending) Text(stringResource(R.string.credential_unsorted), style = AppTypography.Sub13, color = AppColors.TextSub)
                d.items.forEach { it ->
                    CredentialCard(
                        item = it, editing = s.editing == it.kind, files = s.files, busy = s.busy,
                        canAdd = s.files.size < CredentialsUseCase.MAX_FILES,
                        onStart = { viewModel.startEdit(it.kind) },
                        onAdd = { pick.launch(arrayOf("image/*", PDF)) },
                        onRemove = viewModel::removeFile,
                        onCancel = viewModel::cancelEdit,
                        onWithdraw = { viewModel.withdraw(it) },
                        onSubmit = {
                            viewModel.setBusy(true)
                            scope.launch {
                                val docs = s.files.mapIndexed { i, f -> readDoc(context, Uri.parse(f.uri), f.isPdf, i) }
                                if (docs.any { d2 -> d2 == null }) viewModel.readFailed()
                                else { viewModel.setBusy(false); viewModel.submit(docs.filterNotNull()) }
                            }
                        },
                    )
                }
            }
        }
    }
    s.message?.let { m ->
        CustomDialog(
            title = stringResource(R.string.credential_notice_title), content = m, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
            buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = viewModel::dismissMessage, onRightButtonClick = viewModel::dismissMessage, showPopup = true,
        )
    }
}

private const val PDF = "application/pdf"
private const val LIMIT = 10L * 1024 * 1024

/** 사진은 줄여서(UploadImagePreparer), PDF 는 10MB 이하 원본 그대로. 못 읽으면 null. */
private suspend fun readDoc(context: Context, uri: Uri, isPdf: Boolean, index: Int): CredentialDocument? =
    if (!isPdf) UploadImagePreparer.prepare(context, uri)?.let { CredentialDocument(it, "document_$index.jpg", "image/jpeg") }
    else withContext(Dispatchers.IO) {
        runCatching {
            val size = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c -> if (c.moveToFirst()) c.getLong(0) else -1L } ?: -1L
            if (size > LIMIT) return@runCatching null
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.takeIf { it.size <= LIMIT }
                ?.let { CredentialDocument(it, "document_$index.pdf", PDF) }
        }.getOrNull()
    }
