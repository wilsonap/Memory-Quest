package com.example.data.model

data class LevelConfig(
    val levelNumber: Int,
    val cardCount: Int,
    val gridColumns: Int,
    val previewSeconds: Int,
    val initialLives: Int = 3
) {
    val pairCount: Int get() = cardCount / 2

    companion object {
        fun getConfigForLevel(level: Int): LevelConfig {
            val cardCount = when {
                level == 1 -> 4
                level == 2 -> 6
                level == 3 -> 8
                level == 4 -> 10
                level == 5 -> 12
                level == 6 -> 16
                level == 7 -> 20
                level == 8 -> 24
                level == 9 -> 32
                level == 10 -> 40
                level == 11 -> 48
                else -> 64
            }

            val columns = when {
                cardCount <= 10 -> 2
                cardCount <= 16 -> 4
                cardCount <= 24 -> 6
                cardCount <= 32 -> 6
                cardCount <= 40 -> 6
                cardCount <= 48 -> 6
                else -> 8
            }

            val previewTime = when {
                cardCount <= 6 -> 4
                cardCount <= 12 -> 5
                cardCount <= 20 -> 6
                cardCount <= 32 -> 8
                else -> 10
            }

            return LevelConfig(
                levelNumber = level,
                cardCount = cardCount,
                gridColumns = columns,
                previewSeconds = previewTime,
                initialLives = 3
            )
        }
    }
}
