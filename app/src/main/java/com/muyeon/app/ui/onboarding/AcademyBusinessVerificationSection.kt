package com.muyeon.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

@Composable
fun AcademyBusinessVerificationSection(
    businessNumber: String,
    onBusinessNumberChange: (String) -> Unit,
    verifyLater: Boolean,
    onVerifyLaterChange: (Boolean) -> Unit,
    allowsVerifyLater: Boolean,
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("사업자등록번호", fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = MuyeonColors.textHead)
            Spacer(Modifier.weight(1f))
            Text("사업자번호가 없어요", fontFamily = customFontFamily, fontSize = 14.sp,
                color = MuyeonColors.textSub, textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { dialog = "number" })
        }
        OutlinedTextField(
            value = formatBusinessNumber(businessNumber),
            onValueChange = { onBusinessNumberChange(it.filter(Char::isDigit).take(10)) },
            enabled = !verifyLater, singleLine = true,
            placeholder = { Text("사업자등록번호 입력", fontFamily = customFontFamily) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("기업 인증", fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 15.sp, color = MuyeonColors.textHead)
        Column(
            Modifier.fillMaxWidth().background(MuyeonColors.groupedBg, RoundedCornerShape(12.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("사업자등록증명원을 첨부해 주세요", fontFamily = customFontFamily,
                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MuyeonColors.textHead)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DocumentSample("사업자등록증명원", true, Modifier.weight(1f))
                DocumentSample("사업자등록증", false, Modifier.weight(1f))
            }
            Text("ⓘ 사업자등록증명원이 뭐예요?", fontFamily = customFontFamily, fontSize = 14.sp,
                color = MuyeonColors.textSub, modifier = Modifier.clickable { dialog = "document" })
        }
        if (allowsVerifyLater) {
            Row(Modifier.clickable { onVerifyLaterChange(!verifyLater) }, verticalAlignment = Alignment.CenterVertically) {
                Icon(if (verifyLater) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    "다음에 인증", tint = if (verifyLater) MuyeonColors.primary else MuyeonColors.secondary)
                Spacer(Modifier.width(8.dp))
                Text("다음에 인증할게요", fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textHead)
            }
        }
    }

    if (dialog != null) {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (dialog == "document") "사업자등록증명원이 뭐예요?" else "사업자등록번호가 없어요",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.Close, "닫기", modifier = Modifier.clickable { dialog = null })
                }
            },
            text = {
                Text(
                    if (dialog == "document") "사업자등록증과 달리 위조 방지용 번호와 발급 일자가 표시된 서류예요. 정부24에서 발급한 최근 3개월 이내 사업자등록증명원을 제출해 주세요."
                    else "국내 학원·개인사업자는 사업자등록번호와 사업자등록증명원이 있어야 학원 원장 인증을 신청할 수 있어요. 번호가 없다면 ‘다음에 인증할게요’를 선택하고 일반회원으로 계속 이용해 주세요. 해외 사업자는 고객센터로 문의해 주세요.",
                    fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 22.sp,
                )
            },
            confirmButton = {
                if (dialog == "document") {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gov.kr")))
                    }) { Text("정부24에서 발급하기", fontFamily = customFontFamily) }
                } else {
                    TextButton(onClick = { dialog = null }) { Text("확인", fontFamily = customFontFamily) }
                }
            },
        )
    }
}

@Composable
private fun DocumentSample(title: String, accepted: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.height(120.dp).background(Color.White, RoundedCornerShape(10.dp))
        .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, fontFamily = customFontFamily, fontSize = 11.sp,
            color = if (accepted) MuyeonColors.primary else MuyeonColors.textSub)
        Icon(Icons.Filled.Description, null, tint = MuyeonColors.secondary, modifier = Modifier.size(36.dp))
        Text(if (accepted) "○" else "×", fontSize = 32.sp,
            color = if (accepted) MuyeonColors.primary else MuyeonColors.textSub)
    }
}

private fun formatBusinessNumber(raw: String): String {
    val value = raw.filter(Char::isDigit).take(10)
    return when {
        value.length <= 3 -> value
        value.length <= 5 -> "${value.take(3)}-${value.drop(3)}"
        else -> "${value.take(3)}-${value.drop(3).take(2)}-${value.drop(5)}"
    }
}
