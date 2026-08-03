package com.example.avatar.model

import androidx.compose.ui.graphics.Color

data class AvatarPreset(
    val id: String,
    val name: String,
    val category: String,
    val emoji: String,
    val bgColors: List<Color>
) {
    companion object {
        val ALL_PRESETS = listOf(
            AvatarPreset(
                id = "avatar_01",
                name = "Masculino",
                category = "Pessoas",
                emoji = "🧔",
                bgColors = listOf(Color(0xFF4A148C), Color(0xFF1A237E))
            ),
            AvatarPreset(
                id = "avatar_02",
                name = "Feminino",
                category = "Pessoas",
                emoji = "👩‍🦰",
                bgColors = listOf(Color(0xFF880E4F), Color(0xFF4A148C))
            ),
            AvatarPreset(
                id = "avatar_03",
                name = "Neutro",
                category = "Pessoas",
                emoji = "🧑",
                bgColors = listOf(Color(0xFF004D40), Color(0xFF006064))
            ),
            AvatarPreset(
                id = "avatar_04",
                name = "Robô",
                category = "Sci-Fi",
                emoji = "🤖",
                bgColors = listOf(Color(0xFF263238), Color(0xFF37474F))
            ),
            AvatarPreset(
                id = "avatar_05",
                name = "Astronauta",
                category = "Sci-Fi",
                emoji = "🚀",
                bgColors = listOf(Color(0xFF0D47A1), Color(0xFF1B5E20))
            ),
            AvatarPreset(
                id = "avatar_06",
                name = "Mago",
                category = "Fantasia",
                emoji = "🧙",
                bgColors = listOf(Color(0xFF311B92), Color(0xFF4A148C))
            ),
            AvatarPreset(
                id = "avatar_07",
                name = "Explorador",
                category = "Aventura",
                emoji = "🤠",
                bgColors = listOf(Color(0xFFE65100), Color(0xFFBF360C))
            ),
            AvatarPreset(
                id = "avatar_08",
                name = "Gamer",
                category = "Estilo",
                emoji = "🎮",
                bgColors = listOf(Color(0xFF7B1FA2), Color(0xFFC2185B))
            ),
            AvatarPreset(
                id = "avatar_09",
                name = "Animal",
                category = "Natureza",
                emoji = "🦊",
                bgColors = listOf(Color(0xFFFF6D00), Color(0xFFDD2C00))
            ),
            AvatarPreset(
                id = "avatar_10",
                name = "Alien",
                category = "Sci-Fi",
                emoji = "👽",
                bgColors = listOf(Color(0xFF1B5E20), Color(0xFF004D40))
            ),
            AvatarPreset(
                id = "avatar_11",
                name = "Ninja",
                category = "Ação",
                emoji = "🥷",
                bgColors = listOf(Color(0xFF212121), Color(0xFF424242))
            ),
            AvatarPreset(
                id = "avatar_12",
                name = "Divertido",
                category = "Especial",
                emoji = "🎭",
                bgColors = listOf(Color(0xFFFFD600), Color(0xFFFF6D00))
            )
        )

        fun getById(id: String): AvatarPreset {
            return ALL_PRESETS.find { it.id.equals(id, ignoreCase = true) } ?: ALL_PRESETS.first()
        }
    }
}
