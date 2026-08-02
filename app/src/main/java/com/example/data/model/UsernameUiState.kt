package com.example.data.model

import com.example.util.UsernameValidator

data class UsernameUiState(
    val rawInput: String = "",
    val normalizedName: String = "",
    val validationResult: UsernameValidator.ValidationResult = UsernameValidator.ValidationResult.Valid,
    val isCheckingAvailability: Boolean = false,
    val isAvailable: Boolean? = null,
    val suggestions: List<String> = emptyList(),
    val isCheckingSuggestions: Boolean = false,
    val isOffline: Boolean = false
)
