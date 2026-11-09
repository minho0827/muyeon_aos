package com.muyeon.app.ui.policy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muyeon.app.navigation.Screen
import com.muyeon.app.ui.common.applyEdgeToEdge
import com.muyeon.app.ui.common.setEdgeContent
import com.muyeon.app.utils.Constants

/**
 * ★ 2026-11-09 약관 전문을 따로 띄우는 작은 화면 — 탭 셸 밖(로그인·가입 약관·멤버십 구매·거래 동의)에서 쓴다.
 *   예전엔 외부 브라우저로 웹 /policy 를 열었다. 지금은 앱 약관 화면(PolicyScreen) 그대로, 뒤로 가면 보던 화면.
 *   탭 셸(MainActivity)로 보내면 그 위 화면(가입·동의 체크)이 닫혀서 따로 둔다.
 */
class PolicyActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_DOC = "doc"
        private const val EXTRA_VERSION = "version"

        /** 문서 키로(terms·privacy·refund …). */
        fun start(context: Context, doc: String?, version: String? = null) {
            val i = Intent(context, PolicyActivity::class.java)
                .putExtra(EXTRA_DOC, doc.orEmpty())
                .putExtra(EXTRA_VERSION, version.orEmpty())
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }

        /**
         * 서버가 주는 약관 주소("/policy?doc=…&embed=1" 또는 풀 주소)로. 약관 주소가 아니면 false.
         */
        fun startPath(context: Context, path: String): Boolean {
            val base = Constants.getBaseUrl(context).trimEnd('/')
            val p = if (path.startsWith(base)) path.removePrefix(base) else path
            val uri = runCatching { Uri.parse(if (p.startsWith("/")) p else "/$p") }.getOrNull() ?: return false
            if (uri.scheme != null || uri.path?.trimEnd('/') != "/policy") return false
            start(context, uri.getQueryParameter("doc"), uri.getQueryParameter("version"))
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdge()
        super.onCreate(savedInstanceState)
        val doc0 = intent.getStringExtra(EXTRA_DOC)?.ifBlank { null }
        val version0 = intent.getStringExtra(EXTRA_VERSION)?.ifBlank { null }
        setEdgeContent {
            val nav = rememberNavController()
            // 버전을 오가면 같은 화면을 새로 쌓는다(앱 셸 AppNavHost 의 약관과 같다). 첫 화면에서 뒤로 = 닫기.
            NavHost(nav, startDestination = Screen.Policy.route) {
                composable(
                    Screen.Policy.route,
                    arguments = listOf(
                        navArgument(Screen.Policy.ARG_DOC) { type = NavType.StringType; nullable = true; defaultValue = doc0 },
                        navArgument(Screen.Policy.ARG_VERSION) { type = NavType.StringType; nullable = true; defaultValue = version0 },
                    ),
                ) { e ->
                    val doc = e.arguments?.getString(Screen.Policy.ARG_DOC)?.takeIf { it.isNotBlank() } ?: Screen.Policy.DEFAULT_DOC
                    val version = e.arguments?.getString(Screen.Policy.ARG_VERSION)?.takeIf { it.isNotBlank() }
                    PolicyScreen(
                        version = version,
                        onBack = { if (!nav.popBackStack()) finish() },
                        onOpenVersion = { v -> nav.navigate(Screen.Policy.build(doc, v)) },
                        viewModel = org.koin.androidx.compose.koinViewModel(key = "policy:$doc:$version", parameters = { org.koin.core.parameter.parametersOf(doc, version) }),
                    )
                }
            }
        }
    }
}
