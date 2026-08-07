package com.muyeon.app.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.muyeon.app.ui.ipconfig.IpConfigScreen
import com.muyeon.app.ui.ipconfig.IpConfigViewModel

class IpConnectActivity : ComponentActivity() {

    private val viewModel: IpConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IpConfigScreen(
                viewModel = viewModel
            )
        }
    }
}
