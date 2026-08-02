package com.example.data.model

data class GameCard(
    val id: Int,
    val pairId: Int,
    val symbol: String,
    val name: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false,
    val isHighlighted: Boolean = false
)
