package com.example.util

import java.text.Normalizer
import java.util.Locale

object UsernameNormalizer {

    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val INVALID_CHARS_REGEX = Regex("[^a-z0-9_]")

    /**
     * Normalizes a username according to rules:
     * - trim spaces at start and end
     * - convert to lowercase
     * - remove diacritics / accents
     * - remove internal spaces
     * - allow only [a-z0-9_]
     *
     * Examples:
     * "Alex Quest" -> "alexquest"
     * "ÁlexQuest" -> "alexquest"
     * "ALEXQUEST" -> "alexquest"
     * "Alex_Quest" -> "alex_quest"
     */
    fun normalizeUsername(name: String): String {
        val trimmed = name.trim()
        val lowercase = trimmed.lowercase(Locale.ROOT)
        val normalizedNfd = Normalizer.normalize(lowercase, Normalizer.Form.NFD)
        val withoutDiacritics = DIACRITICS_REGEX.replace(normalizedNfd, "")
        return INVALID_CHARS_REGEX.replace(withoutDiacritics, "")
    }
}
