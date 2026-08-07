package com.muyeon.app.common_components.textfield

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Suppress("unused")
@Composable
fun IntegerTextField(
    value: String,
    title: String? = null,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    messageError: String,
    focusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium

            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = if (isFocused || value.isNotEmpty()) value else "0",
            onValueChange = { newValue ->
                val newText = if (newValue.all { it.isDigit() }) newValue else value
                onValueChange(newText)
            },
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!focusState.isFocused && value.isEmpty()) {
                        onValueChange("0")
                    }
                }
                .border(1.dp, colorResource(R.color.primary_border), RoundedCornerShape(12.dp))

        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isError) {
            Text(
                text = messageError,
                color = colorResource(R.color.primary_error),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

        }
    }
}