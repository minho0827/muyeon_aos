package com.muyeon.app.ui.chat

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * 카톡식 말풍선 — 상단 모서리에 꼬리(tail). 수신=좌상단, 발신(내 것)=우상단.
 *  iOS `ChatBubbleShape.swift` 1:1 (radius 16, tail 6).
 *
 * ⚠️ 꼬리 공간만큼 본체를 안쪽으로 인셋한다 — 배경으로 그려도 꼬리가 잘리지 않게.
 *   꼬리 뿌리를 둥근 모서리 중심까지 물려야 본체와 꼬리 사이 초승달 틈(흰 선)이 안 생긴다.
 */
class ChatBubbleShape(
    private val mine: Boolean,
    private val radius: androidx.compose.ui.unit.Dp = 16.dp,
    private val tail: androidx.compose.ui.unit.Dp = 6.dp,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        val t = with(density) { tail.toPx() }
        val path = Path()

        if (mine) {
            // 본체는 오른쪽을 t 만큼 비우고, 그 공간에 꼬리.
            val body = Rect(0f, 0f, size.width - t, size.height)
            path.addRoundRect(RoundRect(body, androidx.compose.ui.geometry.CornerRadius(r, r)))
            val tp = Path().apply {
                moveTo(body.right - r, 0f)
                lineTo(size.width, 0f)
                // 끝점을 본체 안(-3)으로 겹치게 해 본체와 꼬리 사이 미세한 틈을 없앤다.
                quadraticTo(body.right + 2f, 2f, body.right - 3f, t + 4f)
                // 안쪽 밑변을 둥근모서리 중심까지 물림 — 호와 꼬리 사이 초승달 틈 제거.
                lineTo(body.right - r, r)
                close()
            }
            path.addPath(tp)
        } else {
            val body = Rect(t, 0f, size.width, size.height)
            path.addRoundRect(RoundRect(body, androidx.compose.ui.geometry.CornerRadius(r, r)))
            val tp = Path().apply {
                // 발신 꼬리와 같은 방향(winding)으로 되감는다 — 반대로 그리면 겹침이 구멍이 되어 흰 선이 비친다.
                moveTo(body.left + r, 0f)
                lineTo(body.left + r, r)
                lineTo(body.left + 3f, t + 4f)
                quadraticTo(body.left - 2f, 2f, 0f, 0f)
                close()
            }
            path.addPath(tp)
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is ChatBubbleShape && other.mine == mine && other.radius == radius && other.tail == tail

    override fun hashCode(): Int = (if (mine) 1 else 0) * 31 + radius.hashCode() * 31 + tail.hashCode()
}

/** 꼬리 쪽 여백 — 꼬리가 차지하는 6dp 만큼 본문 패딩을 더해 글자가 붙지 않게 한다. */
internal val BUBBLE_TAIL = 6.dp
