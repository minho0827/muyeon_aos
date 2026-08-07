package com.muyeon.app.ui.location

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.muyeon.app.data.repository.LocationRepositoryImpl

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LocationScreen() {
    val context = LocalContext.current
    val activity = context.findActivity()

    val locationProvider = LocationRepositoryImpl(context, activity)

    val viewModel: LocationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return LocationViewModel(locationProvider) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )

    val state = viewModel.state.value

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                viewModel.onPermissionGranted()
            } else {
                viewModel.state.value =
                    state.copy(
                        latitude = null,
                        longitude = null,
                        status = "Reject", message = "Location access denied.")
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        viewModel.onPermissionGranted()
                    }

                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            },
            modifier = Modifier
                .height(50.dp)
                .padding(horizontal = 20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E88E5),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Get Location", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Text(text = "Lat: ${state.latitude ?: "Unknown"}", modifier = Modifier.padding(top = 8.dp))

        Text(text = "Long: ${state.longitude ?: "Unknown"}", modifier = Modifier.padding(top = 8.dp))

        Text(text = "Status: ${state.status}", modifier = Modifier.padding(top = 8.dp))
    }
}
