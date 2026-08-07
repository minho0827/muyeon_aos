package com.muyeon.app.ui.ipconfig

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muyeon.app.R
import com.muyeon.app.common_components.button.ButtonState
import com.muyeon.app.common_components.button.ButtonType
import com.muyeon.app.common_components.button.CustomButton
import com.muyeon.app.data.models.ipconfig.IpHistoryItem
import com.muyeon.app.webview.WebViewActivity

@Composable
fun IpConfigScreen(
    viewModel: IpConfigViewModel = viewModel()
) {
    val context = LocalContext.current
    val ipAddress by viewModel.currentIp.collectAsState()
    val history by viewModel.history.collectAsState()
    val ipError by viewModel.ipError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(viewModel.connectEvent) {
        viewModel.connectEvent.collect { event ->
            if (event is ConnectEvent.Success) {
                context.startActivity(Intent(context, WebViewActivity::class.java))
            }
        }
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isLoading)
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CustomButton(
                    type = ButtonType.Full,
                    state = ButtonState.Normal,
                    rightButtonText = if (isLoading)  stringResource(R.string.connecting) else  stringResource(R.string.connect),
                    onClick = { if (!isLoading) viewModel.onConnect() },
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.enviroment_configuration),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                IpTextField(
                    ipAddress = ipAddress,
                    onIpChange = viewModel::onIpChange,
                    ipError = ipError
                )

                IpHistorySection(
                    history = history,
                    onIpSelected = viewModel::onIpChange
                )
            }

            if (isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun IpTextField(
    ipAddress: String,
    onIpChange: (String) -> Unit,
    ipError: String?
) {
    Column {
        OutlinedTextField(
            value = ipAddress,
            onValueChange = onIpChange,
            placeholder = { Text(stringResource(R.string.ip_address_placdholder)) },
            isError = ipError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ipError?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun IpHistorySection(
    history: List<IpHistoryItem>,
    onIpSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text =  stringResource(R.string.ip_address_history),
            style = MaterialTheme.typography.titleMedium
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
        ) {
            if (history.isEmpty()) {
                Text(
                    text =  stringResource(R.string.no_history),
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    items(history) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.ipAddress,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { onIpSelected(item.ipAddress) }) {
                                Text( stringResource(R.string.use))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary
        )
    }
}