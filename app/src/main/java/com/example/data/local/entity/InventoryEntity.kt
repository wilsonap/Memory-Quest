package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val itemId: String,
    val itemType: String, // "BOOSTER", "THEME", "FRAME", "EFFECT"
    val quantity: Int = 1,
    val isEquipped: Boolean = false
)
