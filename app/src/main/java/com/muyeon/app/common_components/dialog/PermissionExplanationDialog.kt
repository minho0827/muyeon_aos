package com.muyeon.app.common_components.dialog

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muyeon.app.R
import com.muyeon.app.theme.AppColor
import com.muyeon.app.theme.AppTypography
import com.muyeon.app.common_components.button.ButtonState
import com.muyeon.app.common_components.button.ButtonType
import com.muyeon.app.common_components.button.CustomButton
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle

data class PermissionItem(
    val nameRes: Int,
    val isOptional: Boolean,
    val descriptionRes: Int,
    val iconRes: Int
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PermissionExplanationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val permissions = listOf(
        PermissionItem(
            nameRes = R.string.perm_camera_name,
            isOptional = true,
            descriptionRes = R.string.perm_camera_desc,
            iconRes = R.drawable.ic_camera
        ),
        PermissionItem(
            nameRes = R.string.perm_photo_name,
            isOptional = true,
            descriptionRes = R.string.perm_photo_desc,
            iconRes = R.drawable.ic_photo
        ),
        PermissionItem(
            nameRes = R.string.perm_notif_name,
            isOptional = true,
            descriptionRes = R.string.perm_notif_desc,
            iconRes = R.drawable.ic_notifications
        ),
        PermissionItem(
            nameRes = R.string.perm_location_name,
            isOptional = true,
            descriptionRes = R.string.perm_location_desc,
            iconRes = R.drawable.ic_location
        )
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp * 0.85f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = maxDialogHeight)
                .padding(horizontal =  16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.perm_title),
                        style = AppTypography.T19.toTextStyle(FontWeight.W700),
                        color = AppColor.Gray900,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.perm_subtitle),
                        style = AppTypography.T13.toTextStyle(FontWeight.W700),
                        color = AppColor.Gray900,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    permissions.forEach { permission ->
                        PermissionItemRow(permission)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppColor.Gray50,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.perm_optional_notice),
                            style = AppTypography.T15.toTextStyle(FontWeight.W400),
                            color = AppColor.Gray600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CustomButton(
                    type = ButtonType.Full,
                    state = ButtonState.Normal,
                    rightButtonText = stringResource(R.string.confirm_button),
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
private fun PermissionItemRow(permission: PermissionItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(id = permission.iconRes),
            contentDescription = stringResource(permission.nameRes),
            tint = Color.Unspecified,
            modifier = Modifier
                .size(22.dp)
                .padding(end = 6.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val title = stringResource(permission.nameRes)
                val optionalLabel = stringResource(R.string.option)
                Text(
                    text = buildAnnotatedString {
                        append(title)
                        withStyle(
                            style = SpanStyle(
                                color = AppColor.Gray600,
                                fontWeight = FontWeight.W400,
                                fontSize = 14.sp
                            )
                        ) {
                            append(" $optionalLabel")
                        }
                    },
                    style = AppTypography.T18.toTextStyle(FontWeight.W600),
                    color = AppColor.Gray900
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(permission.descriptionRes),
                style = AppTypography.T15.toTextStyle(FontWeight.W400),
                color = AppColor.Gray800
            )
        }
    }
}