package com.example.util

import com.example.R

object UsernameValidator {

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errorResId: Int) : ValidationResult()
    }

    private val RESERVED_WORDS = setOf(
        "admin", "administrator", "support", "suporte",
        "firebase", "autocheckia", "memoryquest", "moderador"
    )

    private val START_CHAR_REGEX = Regex("^[a-zA-Z0-9].*")
    private val ALLOWED_CHARS_REGEX = Regex("^[a-zA-Z0-9_]+$")

    /**
     * Validates a raw display name according to the strict game rules:
     * - 3 to 20 visible characters
     * - Starts with letter or number
     * - Only letters, numbers, and underscore _
     * - No spaces, no emojis, no special characters
     * - Not digits only
     * - Max 2 consecutive underscores
     * - Not reserved words
     */
    fun validate(rawName: String): ValidationResult {
        val trimmed = rawName.trim()

        if (trimmed.length < 3 || trimmed.length > 20) {
            return ValidationResult.Invalid(R.string.val_err_length)
        }

        if (!START_CHAR_REGEX.matches(trimmed)) {
            return ValidationResult.Invalid(R.string.val_err_start_char)
        }

        if (!ALLOWED_CHARS_REGEX.matches(trimmed)) {
            return ValidationResult.Invalid(R.string.val_err_allowed_chars)
        }

        val normalized = UsernameNormalizer.normalizeUsername(trimmed)

        if (normalized.isEmpty() || normalized.length < 3) {
            return ValidationResult.Invalid(R.string.val_err_length)
        }

        if (normalized.all { it.isDigit() }) {
            return ValidationResult.Invalid(R.string.val_err_digits_only)
        }

        if (normalized.contains("___")) {
            return ValidationResult.Invalid(R.string.val_err_consecutive_underscores)
        }

        if (RESERVED_WORDS.any { reserved -> normalized == reserved || normalized.startsWith(reserved) }) {
            return ValidationResult.Invalid(R.string.val_err_reserved)
        }

        return ValidationResult.Valid
    }
}
