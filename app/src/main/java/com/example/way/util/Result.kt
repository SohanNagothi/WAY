package com.example.way.util

/**
 * Generic wrapper for operations that can succeed or fail.
 * Used across repositories and ViewModels for consistent error handling.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

