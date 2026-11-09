package com.muyeon.app.ui.castingstage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.castingstage.CastingStageBody
import com.muyeon.app.ui.castingstage.components.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.rememberCastingStageBodyState
import com.muyeon.app.ui.common.MuyeonColors

/** 캐스팅·무대 첫 화면(iOS CastingStageHomeView) — 위 바(제목·＋·검색) + 본문(CastingStageBody). */
@Composable
fun CastingStageScreen(
    api: CastingStageApi,
    /** null = 하단 탭(뒤로 버튼 없음 · 탭바가 아래 여백을 맡는다). */
    onClose: (() -> Unit)?,
    onOpenCasting: (Int) -> Unit,
    onOpenNews: (StageNewsItem, StageCategory) -> Unit,
    onCastingList: () -> Unit,
    onNewsList: (Int, String) -> Unit,
    onIssues: () -> Unit,
    /** 공연팀으로 활동 중일 때만 넘긴다(웹 CastingStage 헤더 ＋와 같은 규칙). null 이면 숨김. */
    onCreate: (() -> Unit)? = null,
    /** 바뀌면 다시 불러온다 — 공고를 새로 올린 뒤. */
    reloadKey: Int = 0,
    /** 커뮤니티 탭 안 머리(위 바 + 탭). 주면 이 화면 위 바(제목·＋·검색) 대신 그린다 — ＋·검색은 머리가 맡는다. */
    header: (@Composable () -> Unit)? = null,
) {
    // 하단 탭으로 열리면 흰 바 위 상태바 아이콘을 검정으로(탭 셸은 주황 바 기준 흰 아이콘이 기본).
    if (onClose == null && header == null) {
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.DisposableEffect(Unit) {
            (view.context as? android.app.Activity)?.window?.let { w ->
                androidx.core.view.WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = true
            }
            onDispose { }
        }
    }
    val body = rememberCastingStageBodyState()
    Column(Modifier.fillMaxSize().background(Color.White).then(if (header == null) Modifier.statusBarsPadding() else Modifier).then(if (onClose != null) Modifier.navigationBarsPadding() else Modifier)) {
        if (header != null) header()
        else Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onClose != null) Box(Modifier.size(44.dp).clickable(onClick = onClose).semantics { contentDescription = "뒤로" }, Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead)
            } else Spacer(Modifier.width(16.dp))
            Text("캐스팅·무대", style = Bold, fontSize = 20.sp, color = MuyeonColors.textHead)
            Spacer(Modifier.weight(1f))
            if (onCreate != null) {
                Box(Modifier.size(44.dp).clickable(onClick = onCreate).semantics { contentDescription = "공연 공고 등록" }, Alignment.Center) {
                    Icon(Icons.Filled.Add, null, tint = MuyeonColors.textHead)
                }
            }
            Box(Modifier.size(44.dp).clickable { onCastingList() }.semantics { contentDescription = "캐스팅 공고 전체 보기" }, Alignment.Center) {
                Icon(Icons.Filled.Search, null, tint = MuyeonColors.textHead)
            }
        }
        CastingStageBody(
            api = api,
            state = body,
            sectionNav = true,
            bottomPadding = if (onClose == null) com.muyeon.app.ui.main.tabBarBottomPadding() else 80.dp,
            onOpenCasting = onOpenCasting,
            onOpenNews = onOpenNews,
            onCastingList = onCastingList,
            onNewsList = onNewsList,
            onIssues = onIssues,
            reloadKey = reloadKey,
        )
    }
}
