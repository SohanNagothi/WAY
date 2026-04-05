package com.example.way.data.remote

import android.util.Log
import com.example.way.BuildConfig
import com.google.gson.Gson
import com.google.gson.Strictness
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * Lightweight Geoapify geocoding service using OkHttp + Gson.
 * Free tier: 3,000 requests/day — no credit card needed.
 *
 * API docs: https://apidocs.geoapify.com/docs/geocoding/address-autocomplete/
 */
object GeoapifyService {

    private const val TAG = "Geoapify"
    private const val BASE_URL = "https://api.geoapify.com/v1/geocode/autocomplete"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    val isConfigured: Boolean
        get() = BuildConfig.GEOAPIFY_API_KEY.isNotBlank()

    /**
     * Search for place suggestions matching [query].
     * If [nearLat]/[nearLng] are provided, results are biased to nearby places.
     */
    suspend fun autocomplete(
        query: String,
        nearLat: Double? = null,
        nearLng: Double? = null
    ): List<GeoapifyPlace> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()

        try {
            val biasPart = if (nearLat != null && nearLng != null) {
                "&bias=proximity:$nearLng,$nearLat"
            } else {
                ""
            }

            val url = "$BASE_URL?text=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                    "&format=json" +
                    "&limit=5" +
                    biasPart +
                    "&apiKey=${BuildConfig.GEOAPIFY_API_KEY}"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP ${response.code}: ${response.message}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val reader = JsonReader(StringReader(body)).apply {
                // Accept slightly malformed payloads instead of failing hard at line 1/path $.
                setStrictness(Strictness.LENIENT)
            }
            val type = TypeToken.get(GeoapifyResponse::class.java)
            val result = gson.fromJson<GeoapifyResponse>(reader, type) ?: return@withContext emptyList()

            result.results?.map { feature ->
                GeoapifyPlace(
                    placeId = feature.placeId ?: "",
                    name = feature.name ?: feature.formatted ?: "",
                    fullAddress = feature.formatted ?: "",
                    latitude = feature.lat ?: 0.0,
                    longitude = feature.lon ?: 0.0
                )
            } ?: emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Autocomplete error", e)
            emptyList()
        }
    }

    // ── JSON response models ──

    private data class GeoapifyResponse(
        @SerializedName("results") val results: List<GeoapifyFeature>?
    )

    private data class GeoapifyFeature(
        @SerializedName("place_id") val placeId: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("formatted") val formatted: String?,
        @SerializedName("lat") val lat: Double?,
        @SerializedName("lon") val lon: Double?
    )
}
