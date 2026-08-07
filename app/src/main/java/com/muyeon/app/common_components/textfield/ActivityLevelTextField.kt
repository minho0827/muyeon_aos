package com.muyeon.app.common_components.textfield

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Suppress("unused")
@Composable
fun ActivityLevelTextField(
    value: String,
    title: String? = null,
    onClicked: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.primary_border),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClicked() }
        ) {
            OutlinedTextField(
                value = value,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.primary_title)
                ),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_down),
                        contentDescription = "arrow_down",
                        modifier = modifier
                    )
                }
            )
        }
    }
}