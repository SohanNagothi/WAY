package com.example.way.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.way.data.model.FrequentLocation
import com.example.way.data.remote.GeoapifyService
import com.example.way.data.repository.LocationRepository
import com.example.way.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FrequentLocationsViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _locations = MutableLiveData<List<FrequentLocation>>()
    val locations: LiveData<List<FrequentLocation>> = _locations

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    init {
        viewModelScope.launch {
            locationRepository.getLocations().collectLatest {
                _locations.value = it
            }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun addLocation(name: String, address: String, gps: Pair<Double, Double>? = null) {
        viewModelScope.launch {
            val fallback = gps ?: (0.0 to 0.0)
            val resolved = resolveLatLng(name, address, fallback)
            val result = locationRepository.addLocation(
                FrequentLocation(
                    name = name,
                    address = address,
                    latitude = resolved.first,
                    longitude = resolved.second
                )
            )
            _statusMessage.value = when (result) {
                is Result.Success -> "Location added"
                is Result.Error -> result.message
                Result.Loading -> null
            }
        }
    }

    fun updateLocation(location: FrequentLocation, gps: Pair<Double, Double>? = null) {
        viewModelScope.launch {
            val fallback = gps ?: (location.latitude to location.longitude)
            val resolved = resolveLatLng(location.name, location.address, fallback)
            val result = locationRepository.updateLocation(
                location.copy(
                    latitude = resolved.first,
                    longitude = resolved.second
                )
            )
            _statusMessage.value = when (result) {
                is Result.Success -> "Location updated"
                is Result.Error -> result.message
                Result.Loading -> null
            }
        }
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            val result = locationRepository.deleteLocation(locationId)
            _statusMessage.value = when (result) {
                is Result.Success -> "Location deleted"
                is Result.Error -> result.message
                Result.Loading -> null
            }
        }
    }

    private suspend fun resolveLatLng(
        name: String,
        address: String,
        fallback: Pair<Double, Double>
    ): Pair<Double, Double> {
        if (!GeoapifyService.isConfigured) return fallback
        val query = listOf(name, address).filter { it.isNotBlank() }.joinToString(", ")
        if (query.isBlank()) return fallback

        val place = GeoapifyService.autocomplete(query).firstOrNull()
        return if (place != null) {
            place.latitude to place.longitude
        } else {
            fallback
        }
    }
}
