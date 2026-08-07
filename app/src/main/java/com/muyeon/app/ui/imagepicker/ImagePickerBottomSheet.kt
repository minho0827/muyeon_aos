package com.muyeon.app.ui.imagepicker

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.muyeon.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImagePickerBottomSheet(
    images: List<Uri>,
    onSelect: (Uri) -> Unit,
    onCancel: () -> Unit,
    onAddImages: () -> Unit,
    requestPermissions: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.9f
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F4F4))
                ) {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            TextButton(onClick = onCancel) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.select_photo),
                                color = Color.Black,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            TextButton(onClick = onAddImages) {
                                Text(
                                    text = stringResource(R.string.add_more_photos),
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color(0xFFF4F4F4)
                        )
                    )

                    if (images.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.photos_licensed, images.size),
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF4F4F4))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                if (images.isEmpty()) {
                    EmptyState(onRequest = requestPermissions)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(images) { uri ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedUri = uri
                                        showConfirmDialog = true
                                    }
                            ) {
                                GlideImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            if (showConfirmDialog && selectedUri != null) {

                CustomDialog(
                    title = stringResource(R.string.selected_photo),
                    content = stringResource(R.string.confirm_send_photo),
                    leftButtonText = stringResource(R.string.cancel),
                    rightButtonText = stringResource(R.string.confirm_button),
                    buttonCount = 2,
                    alignment = ContentAlignment.Middle,
                    showPopup = showConfirmDialog,
                    onDismiss = {
                        showConfirmDialog = false
                        selectedUri = null },
                    onRightButtonClick ={
                        selectedUri?.let(onSelect)
                        showConfirmDialog = false
                        selectedUri = null },
                    onLeftButtonClick = {
                        showConfirmDialog = false
                        selectedUri = null
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter  = painterResource(R.drawable.icon_no_image),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_photos),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD0D0D0),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.please_add_photo),
            fontSize = 14.sp,
            color = Color(0xFFD0D0D0),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF007AFF)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF),
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.add_photo_button))
        }
    }
}
