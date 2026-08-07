package com.muyeon.app.ui.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.muyeon.app.R
import com.muyeon.app.common_components.button.ButtonState
import com.muyeon.app.common_components.button.ButtonType
import com.muyeon.app.common_components.button.CustomButton
import com.muyeon.app.data.models.qr.ReservationData
import com.muyeon.app.theme.AppTypography
import com.muyeon.app.webview.ScanQRWebViewInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultQRScreen(
    result: String,
    navController: NavController,
    scanQRInterface: ScanQRWebViewInterface?,
    viewModel: ResultQRViewModel = viewModel(
        factory = ResultQRViewModelFactory(result, scanQRInterface)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    stringResource(R.string.title_scan_qr),
                    style = AppTypography.T12.toTextStyle(FontWeight.Bold)
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(end = 20.dp),
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.icon_navigation),
                            contentDescription = "Back Button"
                        )
                    },
                )
            })
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (uiState.parsingError != null) {
                Text("${stringResource(R.string.fail)}: ${uiState.parsingError}", color = MaterialTheme.colorScheme.error)
            } else {
                ReservationDataCard(
                    data = uiState.reservationData,
                    isLoading = uiState.isLoading,
                    onCallApiClick = viewModel::callApiAndSendResult
                )
            }
        }
    }

    uiState.dialogMessage?.let { message ->
        val resourceId: Int? = try {
            val id = message.toInt()
            if (id != 0) id else null
        } catch (e: NumberFormatException) {
            null
        }
        val localizedMessage = if (resourceId != null) {
            stringResource(id = resourceId)
        } else {
            message
        }
        ResultAlertDialog(
            message = localizedMessage,
            isSuccess = uiState.apiStatus == 200,
            onDismiss = viewModel::dismissDialog,
            onConfirm = viewModel::sendResultAndDismiss
        )
    }
}

@Composable
fun ReservationDataCard(
    data: ReservationData?,
    isLoading: Boolean,
    onCallApiClick: () -> Unit
) {
    val buttonState = if (isLoading) ButtonState.Loading else ButtonState.Normal

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        DataRow(label = "rsv_seq", value = data?.rsvSeq.toString())
        DataRow(label = "mbr_seq", value = data?.mbrSeq.toString())
        DataRow(label = "cf_fd_seq", value = data?.cfFdSeq.toString())
        DataRow(label = "status_cd", value = data?.rsvStatusCd)
        DataRow(label = "rsv_date", value = data?.rsvDate)
        DataRow(label = "rsv_cnt", value = data?.rsvCnt.toString())

        Spacer(modifier = Modifier.padding(20.dp))

        CustomButton(
            type = ButtonType.Full,
            state = buttonState,
            rightButtonText = stringResource(R.string.call_api),
            onClick = onCallApiClick,
        )
    }
}

@Composable
fun ResultAlertDialog(
    message: String,
    isSuccess: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val iconId = if (isSuccess) R.drawable.check else R.drawable.cross
    val valuePainter = painterResource(id = iconId)

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = valuePainter,
                    contentDescription = if (isSuccess) "Success" else "Failure",
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    message,
                    style = AppTypography.T12.toTextStyle(FontWeight.Bold),
                )
            }
        },
        confirmButton = {
            CustomButton(
                type = ButtonType.Full,
                state = ButtonState.Normal,
                rightButtonText = stringResource(R.string.ok),
                onClick = onConfirm,
            )
        }
    )
}

@Composable
fun DataRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = AppTypography.T12.toTextStyle(FontWeight.Medium)
        )
        if (value != null) {
            Text(
                value,
                style = AppTypography.T12.toTextStyle(FontWeight.Normal)
            )
        }
    }
}