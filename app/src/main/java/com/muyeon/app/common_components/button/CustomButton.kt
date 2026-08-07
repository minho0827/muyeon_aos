package com.muyeon.app.common_components.button

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.muyeon.app.common_components.circular_progress_indicator.CustomCircularProgressIndicator
import com.muyeon.app.theme.AppColor
import com.muyeon.app.theme.AppTypography

enum class ButtonType {
    Full,
    T11,
    T12,
    Vertical,
    WithIcon
}

@Suppress("unused")
enum class ButtonState {
    Normal,
    Pressed,
    Disabled,
    Loading
}

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    type: ButtonType,
    state: ButtonState,
    leftButtonText: String? = null,
    rightButtonText: String,
    onClick: () -> Unit,
    onClickBack: () -> Unit? = {}
) {
    val isClickable = state != ButtonState.Disabled && state != ButtonState.Loading
    val defaultLeftText = "레이블"

    when (type) {
        ButtonType.Full -> {
            Button(
                onClick = { if (isClickable) onClick() },
                modifier = modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state == ButtonState.Disabled) AppColor.Gray200 else AppColor.Orange500
                ),
            ) {
                if (state == ButtonState.Loading) {
                    CustomCircularProgressIndicator()
                } else {
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = rightButtonText,
                        style = AppTypography.T13.toTextStyle(FontWeight.Bold),
                        color = AppColor.RealWhite
                    )
                }
            }
        }

        ButtonType.T11 -> {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                OutlinedButton(
                    onClick = { onClickBack() },
                    modifier = modifier
                        .weight(1f),
                    border = BorderStroke(1.dp, color = AppColor.Gray500),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.RealWhite
                    )
                ) {
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = leftButtonText ?: defaultLeftText,
                        style = AppTypography.T13.toTextStyle(FontWeight.SemiBold),
                        color = AppColor.RealBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { if (isClickable) onClick() },
                    modifier = modifier.weight(1f),
                    border = BorderStroke(1.dp, color = AppColor.Orange500),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.Orange500
                    )
                ) {
                    if (state == ButtonState.Loading) {
                        CustomCircularProgressIndicator()
                    } else {
                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = rightButtonText,
                            style = AppTypography.T13.toTextStyle(FontWeight.Bold),
                            color = AppColor.RealWhite
                        )
                    }
                }
            }
        }

        ButtonType.T12 -> {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                OutlinedButton(
                    onClick = { onClickBack() },
                    modifier = modifier
                        .weight(1f),
                    border = BorderStroke(1.dp, color = AppColor.Gray500),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.RealWhite
                    )
                ) {
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = leftButtonText ?: defaultLeftText,
                        style = AppTypography.T13.toTextStyle(FontWeight.SemiBold),
                        color = AppColor.RealBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { if (isClickable) onClick() },
                    modifier = modifier.weight(2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.Orange500
                    )
                ) {
                    if (state == ButtonState.Loading) {
                        CustomCircularProgressIndicator()
                    } else {
                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = rightButtonText,
                            style = AppTypography.T13.toTextStyle(FontWeight.Bold),
                            color = AppColor.RealWhite
                        )
                    }
                }
            }
        }

        ButtonType.WithIcon -> {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                OutlinedButton(
                    onClick = { onClickBack() },
                    modifier = modifier
                        .weight(0.5f),
                    border = BorderStroke(1.dp, color = AppColor.Gray500),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.RealWhite
                    ),
                    contentPadding = PaddingValues()

                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Images",
                        modifier = Modifier.size(26.67.dp),
                        tint = AppColor.Gray900
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { if (isClickable) onClick() },
                    modifier = modifier.weight(2.5f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.Orange500
                    )
                ) {
                    if (state == ButtonState.Loading) {
                        CustomCircularProgressIndicator()
                    } else {
                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = rightButtonText,
                            style = AppTypography.T13.toTextStyle(FontWeight.Bold),
                            color = AppColor.RealWhite
                        )
                    }
                }
            }
        }

        ButtonType.Vertical -> {
            Column (
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                OutlinedButton(
                    onClick = { if (isClickable) onClick() },
                    modifier = modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.Orange500
                    )
                ) {
                    if (state == ButtonState.Loading) {
                        CustomCircularProgressIndicator()
                    } else {
                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = rightButtonText,
                            style = AppTypography.T12.toTextStyle(FontWeight.Bold),
                            color = AppColor.RealWhite
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { if (isClickable) onClick() },
                    modifier = modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state == ButtonState.Disabled) AppColor.Gray200
                        else AppColor.RealWhite
                    )
                ) {
                    if (state == ButtonState.Loading) {
                        CustomCircularProgressIndicator()
                    } else {
                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = leftButtonText ?: defaultLeftText,
                            style = AppTypography.T12.toTextStyle(FontWeight.Bold),
                            color = AppColor.RealBlack
                        )
                    }
                }
            }
        }
    }
}