package com.example.util

import kotlin.random.Random

object UsernameSuggestionGenerator {

    private val PREFIXES = listOf("Player", "Quest", "Match", "Memo", "Master", "Star")
    private val SUFFIXES = listOf("Quest", "Match", "Memo", "Pair", "Pro", "Hero")

    /**
     * Generates 4 to 6 candidate username suggestions based on a base name.
     */
    fun generateSuggestions(
        baseDisplayName: String,
        validator: UsernameValidator = UsernameValidator,
        count: Int = 5
    ): List<String> {
        val normalizedBase = UsernameNormalizer.normalizeUsername(baseDisplayName)
        val cleanBase = if (normalizedBase.isEmpty()) "Player" else baseDisplayName.trim().replace(" ", "")

        val candidates = mutableSetOf<String>()
        val currentYear = 2026

        // 1. Base + random numbers
        candidates.add("${cleanBase}${Random.nextInt(10, 99)}")
        candidates.add("${cleanBase}_${Random.nextInt(10, 99)}")
        candidates.add("${cleanBase}${Random.nextInt(100, 999)}")

        // 2. Base + year
        candidates.add("${cleanBase}${currentYear}")

        // 3. Suffix / Prefix combinations
        for (suffix in SUFFIXES) {
            candidates.add("${cleanBase}${suffix}")
        }
        for (prefix in PREFIXES) {
            candidates.add("${prefix}${cleanBase}")
        }

        // Filter candidates to ensure strict rule compliance
        return candidates
            .map { it.take(20) }
            .filter { candidate ->
                validator.validate(candidate) is UsernameValidator.ValidationResult.Valid
            }
            .distinct()
            .take(count)
    }
}
