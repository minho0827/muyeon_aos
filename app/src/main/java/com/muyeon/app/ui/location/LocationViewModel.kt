package com.muyeon.app.ui.location

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.domain.models.location.LocationResult
import com.muyeon.app.domain.repositories.LocationRepository
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationProvider: LocationRepository
) : ViewModel() {

    var state = mutableStateOf(LocationResult())
        private set

    fun onPermissionGranted() {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, status = "Requesting...", message = null)

            locationProvider.requestLocation { result ->
                state.value = result
            }
        }
    }
}
