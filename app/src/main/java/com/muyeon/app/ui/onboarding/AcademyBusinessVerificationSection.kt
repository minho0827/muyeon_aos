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
    /** 건너뛰기 없는 흐름(MY 회원유형 관리 등)의 '다음에 인증할게요' — 제출 없이 인증 화면을 닫는다. */
    onLater: (() -> Unit)? = null,
    isChecking: Boolean = false,
    isVerified: Boolean = false,
    statusMessage: String? = null,
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    // 가입(건너뛰기 허용) = 체크박스와 같은 동작, 그 외 = 제출 없이 닫기, 둘 다 없으면 '확인'만.
    val laterAction: (() -> Unit)? = if (allowsVerifyLater) ({ onVerifyLaterChange(true) }) else onLater
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("사업자등록번호", fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = MuyeonColors.textHead)
            Spacer(Modifier.weight(1f))
            Text("사업자등록번호가 없어요", fontFamily = customFontFamily, fontSize = 14.sp,
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
        if (!verifyLater && (isChecking || statusMessage != null)) {
            Text(
                if (isChecking) "사업자등록번호 확인 중…" else statusMessage.orEmpty(),
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 17.sp,
                color = if (isVerified) MuyeonColors.primary else MuyeonColors.textSub,
            )
        }
        Text("학원 인증", fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 15.sp, color = MuyeonColors.textHead)
        Column(
            Modifier.fillMaxWidth().background(MuyeonColors.groupedBg, RoundedCornerShape(12.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("사업자등록증명원이 있으면 첨부해 주세요 (선택)", fontFamily = customFontFamily,
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
        Text(
            "인증 전에는 공고를 비공개로 작성할 수 있지만, 공고 공개·이력서 및 지원자 상세 열람·연락·학원 레슨 운영은 제한돼요.",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
            color = MuyeonColors.textSub,
        )
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
                    else noNumberMessage(allowsVerifyLater),
                    fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 22.sp,
                )
            },
            confirmButton = {
                if (dialog == "document") {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gov.kr")))
                    }) { Text("정부24에서 발급하기", fontFamily = customFontFamily) }
                } else if (laterAction != null) {
                    // ★ 2026-10-19 안내문이 '다음에 인증할게요'를 고르라는데 건너뛰기 없는 흐름엔 그 버튼이 없었다(iOS 동일).
                    Button(onClick = { dialog = null; laterAction() }) { Text("다음에 인증할게요", fontFamily = customFontFamily) }
                } else {
                    TextButton(onClick = { dialog = null }) { Text("확인", fontFamily = customFontFamily) }
                }
            },
        )
    }
}

// 건너뛰면 서버는 원장 인증행만 'NONE'(MY 카드: 인증 필요)으로 만들고 활동유형은 그대로 둔다(iOS 와 같은 문구).
private fun noNumberMessage(allowsVerifyLater: Boolean): String {
    val head = "학원 원장 인증에는 사업자등록번호가 필요해요."
    return if (allowsVerifyLater) {
        "$head\n\n번호가 없다면 ‘다음에 인증할게요’로 먼저 가입해 두세요. 학원 원장 유형은 ‘인증 필요’ 상태로 등록되고, " +
            "그동안 공고를 비공개로 미리 작성해 둘 수 있어요. 번호가 준비되면 MY > 회원유형 관리에서 인증하면 바로 이용할 수 있어요."
    } else {
        "$head\n\n번호가 준비되면 MY > 회원유형 관리에서 언제든 인증할 수 있어요. " +
            "인증 전에도 공고를 비공개로 미리 작성해 둘 수 있고, 지금 쓰던 회원유형은 그대로 이용할 수 있어요."
    }
}

@Composable
fun TeamBusinessNumberHelp() {
    var isOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(MuyeonColors.groupedBg, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("사업자등록번호가 없는 공연팀도 인증할 수 있어요.", fontFamily = customFontFamily,
            fontSize = 14.sp, color = MuyeonColors.textSub, modifier = Modifier.weight(1f))
        Text("안내 보기", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, color = MuyeonColors.primary, modifier = Modifier.clickable { isOpen = true })
    }
    if (isOpen) {
        AlertDialog(
            onDismissRequest = { isOpen = false },
            title = { Text("사업자등록번호가 없어요", fontFamily = customFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text("해외 공연팀·기획자는 해당 국가에서 발급한 사업자 또는 단체 증빙을 첨부해 주세요. 국내 비영리 단체는 고유번호증을, 프로젝트 공연팀은 단체명과 활동 내역을 확인할 수 있는 공연 증빙을 제출할 수 있어요.",
                    fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 22.sp)
            },
            confirmButton = { TextButton(onClick = { isOpen = false }) { Text("확인", fontFamily = customFontFamily) } },
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
