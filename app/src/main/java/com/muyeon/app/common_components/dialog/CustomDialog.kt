package com.muyeon.app.common_components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.muyeon.app.common_components.button.ButtonState
import com.muyeon.app.common_components.button.ButtonType
import com.muyeon.app.common_components.button.CustomButton
import com.muyeon.app.theme.AppColor
import com.muyeon.app.theme.AppTypography

@Suppress("unused")
enum class ContentAlignment {
    Left,
    Middle
}

@Composable
fun CustomDialog(
    title: String,
    content: String? = null,
    leftButtonText: String? = "레이블",
    rightButtonText: String,
    buttonCount: Int,
    alignment: ContentAlignment,
    onDismiss: () -> Unit,
    onLeftButtonClick: () -> Unit = {},
    onRightButtonClick: () -> Unit,
    showPopup: Boolean
) {
    if (showPopup) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AppColor.RealWhite
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = when (alignment) {
                        ContentAlignment.Middle -> Alignment.CenterHorizontally
                        else -> Alignment.Start
                    }
                ) {
                    val textAlign =
                        if (alignment == ContentAlignment.Middle) TextAlign.Center else TextAlign.Start

                    Text(
                        text = title,
                        style = AppTypography.T12.toTextStyle(FontWeight.Bold),
                        color = AppColor.Gray900,
                        textAlign = textAlign,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    if (content != null)
                        Text(
                            text = content,
                            style = AppTypography.T14.toTextStyle(FontWeight.Medium),
                            color = AppColor.Gray800,
                            textAlign = textAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    if (buttonCount == 2) {
                        CustomButton(
                            type = ButtonType.T11,
                            state = ButtonState.Normal,
                            leftButtonText = leftButtonText,
                            rightButtonText = rightButtonText,
                            onClick = onRightButtonClick,
                            onClickBack = onLeftButtonClick
                        )
                    } else {
                        CustomButton(
                            type = ButtonType.Full,
                            state = ButtonState.Normal,
                            rightButtonText = rightButtonText,
                            onClick = onRightButtonClick,
                        )
                    }
                }
            }
        }
    }
}