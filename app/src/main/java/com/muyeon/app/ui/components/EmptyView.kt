package com.muyeon.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 빈 목록 안내 — 앱 전체에서 이 부품 하나만 쓴다. 웹 components/ui/EmptyView.js(.list-none: 문서 그림 48 · 제목 18 medium · 설명 14 · 외곽선 둥근 버튼).
 *
 * 위치 규칙(2026-11-08 사용자 요청 — 화면마다 문구 높이가 달랐던 것):
 *   - **목록 영역 맨 위**(필터 칩·탭 줄 바로 아래)에 놓는다. 위 여백은 [Dimensions.emptyTop] 고정.
 *   - 높이가 다른 상자 안에서 세로 가운데 정렬(`Alignment.Center`·`weight`+`Center`·`wrapContentHeight(Center)`)하지 않는다.
 *   - 당겨서 새로고침 안이면 스크롤 목록의 `item { EmptyView(…) }` 로(맨 위).
 *   - 불러오는 중에는 그리지 않는다 — 부르는 쪽이 로딩이 끝난 뒤에만 넣는다(웹은 busy 게이트).
 */
@Composable
fun EmptyView(
    title: String,
    modifier: Modifier = Modifier,
    content: String? = null,
    buttonText: String? = null,
    onButton: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth()
            .padding(top = Dimensions.emptyTop, bottom = Dimensions.emptyBottom, start = Dimensions.gutter, end = Dimensions.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
    ) {
        Icon(painterResource(R.drawable.ic_web_nodata), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(Dimensions.emptyIcon))
        Text(title, style = AppTypography.Title18.copy(fontWeight = FontWeight.Medium), color = AppColors.TextHead, textAlign = TextAlign.Center)
        if (!content.isNullOrBlank()) Text(content, style = AppTypography.Body14, color = AppColors.TextSub, textAlign = TextAlign.Center)
        if (buttonText != null && onButton != null) {
            OutlinedButton(
                onClick = onButton, shape = CircleShape, border = BorderStroke(1.dp, AppColors.ChipBorder),
                modifier = Modifier.padding(top = Dimensions.spacingS).testTag("empty.action").semantics { contentDescription = "empty.action" },
            ) {
                Text(buttonText, style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
            }
        }
    }
}
