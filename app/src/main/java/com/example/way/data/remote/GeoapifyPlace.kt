package com.example.way.data.remote

/**
 * Represents a single place suggestion from Geoapify autocomplete.
 */
data class GeoapifyPlace(
    val placeId: String,
    val name: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double
)

