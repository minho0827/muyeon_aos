package com.muyeon.app.ui.my.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.my.MyIcons
import com.muyeon.app.ui.my.MyMenuBuilder
import com.muyeon.app.ui.my.MyMenuItem
import com.muyeon.app.ui.my.MyUiState
import com.muyeon.app.ui.my.membershipSummary
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.member.ActiveType

/**
 * 회원 MY 본문 — 웹 MemberMyHome: 주황 머리 · 바로가기 카드 · 메뉴 묶음 · (운영자) 캐스팅/공연 자료 · [앱 설정][로그아웃].
 *
 * @param onMenu 메뉴 누름(앱 화면/웹 창 판단은 부르는 쪽)
 * @param onShortcutCalendar 가운데 바로가기(레슨 일정·활동일정·일정)
 */
@Composable
fun MemberMyHome(
    state: MyUiState,
    onHeader: () -> Unit,
    onMenu: (MyMenuItem) -> Unit,
    onShortcutCalendar: () -> Unit,
    onShortcutCasting: () -> Unit,
    onShortcutCommunity: () -> Unit,
    regionName: String?,
    onRegion: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val str: (Int) -> String = context::getString
    val type = state.activeType
    val groups = MyMenuBuilder.build(type, state.serverMenus, state.info, str)
    val summary = membershipSummary(state.info?.membership, str)
    val noti = state.info?.notiUnread ?: 0
    val quote = state.info?.quoteUnread ?: 0
    val typeLabel = stringResource(
        when (type) {
            ActiveType.TEACHER -> R.string.member_type_teacher
            ActiveType.ACADEMY -> R.string.member_type_academy
            ActiveType.DANCER -> R.string.member_type_dancer
            ActiveType.TEAM -> R.string.member_type_team
            else -> R.string.member_type_general
        },
    )
    val name = state.info?.name ?: stringResource(R.string.my_default_name)

    // 가운데 바로가기 — 무용수 '활동일정' · 공연팀 '일정'(캐스팅) · 그 밖 '레슨 일정'(웹 HomeShortcuts).
    val middle = when (type) {
        ActiveType.DANCER -> MyShortcut(stringResource(R.string.shortcut_activity_schedule), MyIcons.of("filled-note"), onShortcutCalendar)
        ActiveType.TEAM -> MyShortcut(stringResource(R.string.shortcut_schedule), MyIcons.of("filled-star"), onShortcutCasting)
        else -> MyShortcut(stringResource(R.string.shortcut_lesson_schedule), MyIcons.of("filled-note"), onShortcutCalendar)
    }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = com.muyeon.app.ui.main.tabBarBottomPadding()), horizontalAlignment = Alignment.CenterHorizontally) {
        MyProfileHeader(stringResource(R.string.my_header, name, withEuro(typeLabel)), onClick = onHeader)
        MyShortcutsCard(listOf(middle, MyShortcut(stringResource(R.string.tab_community), MyIcons.of("filled-community"), onShortcutCommunity)))
        Column(Modifier.offset(y = (-60).dp)) {
            MyRegionRow(regionName, onRegion)
            MyCommunitySection(onShortcutCommunity)
            groups.forEachIndexed { gi, group ->
                val rows = group.map { item ->
                    MyMenuRow(
                        key = item.path ?: item.bridge ?: item.title,
                        title = item.title,
                        iconRes = MyIcons.of(item.icon),
                        desc = MyMenuBuilder.descOf(item, summary, str),
                        showDot = (item.bridge == MyMenuBuilder.B_NOTIFICATIONS && noti > 0) ||
                            ((item.bridge == MyMenuBuilder.B_MY_QUOTES || item.bridge == MyMenuBuilder.B_LESSON_HUB) && quote > 0),
                    )
                }
                MyMenuList(rows, onClick = { r -> group.firstOrNull { (it.path ?: it.bridge ?: it.title) == r.key }?.let(onMenu) }, topPadding = gi == 0)
            }
            if (state.info?.castingMaterialAllowed == true) {
                val items = listOf(
                    MyMenuItem(stringResource(R.string.my_menu_casting_materials), "camera", "/my/castingMaterials", desc = stringResource(R.string.my_desc_casting_materials)),
                    MyMenuItem(stringResource(R.string.my_menu_performance_materials), "camera", "/my/performanceMaterials", desc = stringResource(R.string.my_desc_performance_materials)),
                )
                MyMenuList(items.map { MyMenuRow(it.path!!, it.title, MyIcons.of(it.icon), it.desc) }, onClick = { r -> items.first { it.path == r.key }.let(onMenu) }, topPadding = false)
            }
            MySubMenu(
                listOf(
                    MySubMenuItem("settings", stringResource(R.string.my_app_settings), R.drawable.ic_web_gear),
                    MySubMenuItem("logout", stringResource(R.string.my_logout), R.drawable.ic_web_logout),
                ),
                onClick = { if (it.key == "logout") onLogout() else onSettings() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(Dimensions.spacing3XL))
        }
    }
}

/** "강사로" / "일반회원으로" — 받침에 따라 로/으로(웹 withEuro). */
internal fun withEuro(name: String): String {
    val c = name.lastOrNull() ?: return name
    val code = c.code - 0xAC00
    if (code < 0 || code > 11171) return "$name(으)로"
    val jong = code % 28
    return name + if (jong == 0 || jong == 8) "로" else "으로"
}
