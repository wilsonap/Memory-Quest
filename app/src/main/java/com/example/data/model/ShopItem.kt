package com.example.data.model

import androidx.annotation.StringRes
import com.example.R

enum class ShopCategory { BOOSTER, THEME, FRAME, EFFECT }

data class ShopItem(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val price: Int,
    val category: ShopCategory,
    val iconName: String,
    val isOwned: Boolean = false,
    val quantity: Int = 0
) {
    companion object {
        val SHOP_BOOSTERS = listOf(
            ShopItem(
                id = "booster_life",
                titleRes = R.string.booster_life_title,
                descriptionRes = R.string.booster_life_desc,
                price = 80,
                category = ShopCategory.BOOSTER,
                iconName = "Favorite"
            ),
            ShopItem(
                id = "booster_hint",
                titleRes = R.string.booster_hint_title,
                descriptionRes = R.string.booster_hint_desc,
                price = 120,
                category = ShopCategory.BOOSTER,
                iconName = "Lightbulb"
            ),
            ShopItem(
                id = "booster_reveal",
                titleRes = R.string.booster_reveal_title,
                descriptionRes = R.string.booster_reveal_desc,
                price = 150,
                category = ShopCategory.BOOSTER,
                iconName = "Visibility"
            ),
            ShopItem(
                id = "booster_time",
                titleRes = R.string.booster_time_title,
                descriptionRes = R.string.booster_time_desc,
                price = 100,
                category = ShopCategory.BOOSTER,
                iconName = "Timer"
            ),
            ShopItem(
                id = "booster_freeze",
                titleRes = R.string.booster_freeze_title,
                descriptionRes = R.string.booster_freeze_desc,
                price = 110,
                category = ShopCategory.BOOSTER,
                iconName = "AcUnit"
            )
        )

        val SHOP_FRAMES = listOf(
            ShopItem("frame_classic", R.string.frame_classic_title, R.string.frame_classic_desc, 0, ShopCategory.FRAME, "CropSquare", true),
            ShopItem("frame_golden", R.string.frame_golden_title, R.string.frame_golden_desc, 300, ShopCategory.FRAME, "Star"),
            ShopItem("frame_neon", R.string.frame_neon_title, R.string.frame_neon_desc, 450, ShopCategory.FRAME, "Bolt"),
            ShopItem("frame_magic", R.string.frame_magic_title, R.string.frame_magic_desc, 600, ShopCategory.FRAME, "AutoAwesome")
        )
    }
}
