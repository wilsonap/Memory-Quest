package com.example.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.data.model.GameTheme
import com.example.data.model.ShopItem
import com.example.ui.components.BannerAdView
import com.example.ui.components.TopGameBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    player: PlayerEntity?,
    unlockedThemes: List<UnlockedThemeEntity>,
    onBuyTheme: (GameTheme) -> Unit,
    onSelectTheme: (String) -> Unit,
    onBuyFrame: (ShopItem) -> Unit,
    onBuyBooster: (String, Int) -> Unit,
    onBackClick: () -> Unit,
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val coins = player?.coins ?: 0
    val unlockedThemeIds = unlockedThemes.map { it.themeId }.toSet()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1B1038),
                        Color(0xFF0F0824)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopGameBar(
                coins = coins,
                title = stringResource(R.string.shop_title),
                onBackClick = onBackClick
            )

            // Shop Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E143B),
                contentColor = Color(0xFF4CC9F0)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.shop_tab_boosters), fontWeight = FontWeight.Bold) },
                    selectedContentColor = Color(0xFF4CC9F0),
                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.shop_tab_themes), fontWeight = FontWeight.Bold) },
                    selectedContentColor = Color(0xFFFFB703),
                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.shop_tab_frames), fontWeight = FontWeight.Bold) },
                    selectedContentColor = Color(0xFFE0AAFF),
                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        // BOOSTERS
                        items(ShopItem.SHOP_BOOSTERS) { booster ->
                            BoosterShopCard(
                                booster = booster,
                                coins = coins,
                                onBuy = { onBuyBooster(booster.id, booster.price) }
                            )
                        }
                    }

                    1 -> {
                        // THEMES
                        items(GameTheme.ALL_THEMES) { theme ->
                            val isUnlocked = theme.isDefaultUnlocked || unlockedThemeIds.contains(theme.id)
                            val isEquipped = player?.equippedThemeId == theme.id

                            ThemeShopCard(
                                theme = theme,
                                isUnlocked = isUnlocked,
                                isEquipped = isEquipped,
                                coins = coins,
                                onBuy = { onBuyTheme(theme) },
                                onEquip = { onSelectTheme(theme.id) }
                            )
                        }
                    }

                    2 -> {
                        // FRAMES
                        items(ShopItem.SHOP_FRAMES) { frame ->
                            val isEquipped = player?.equippedFrameId == frame.id

                            FrameShopCard(
                                frame = frame,
                                isEquipped = isEquipped,
                                coins = coins,
                                onBuy = { onBuyFrame(frame) }
                            )
                        }
                    }
                }
            }

            BannerAdView(isAdsRemoved = isAdsRemoved)
        }
    }
}

@Composable
private fun BoosterShopCard(
    booster: ShopItem,
    coins: Int,
    onBuy: () -> Unit
) {
    val canAfford = coins >= booster.price
    val iconVector = when (booster.iconName) {
        "Favorite" -> Icons.Default.Favorite
        "Lightbulb" -> Icons.Default.Lightbulb
        "Visibility" -> Icons.Default.Visibility
        "Timer" -> Icons.Default.Timer
        "AcUnit" -> Icons.Default.AcUnit
        else -> Icons.Default.AutoAwesome
    }

    Surface(
        color = Color(0xFF231842),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF4CC9F0).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF32235E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = stringResource(booster.titleRes),
                    tint = Color(0xFF4CC9F0),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(booster.titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(booster.descriptionRes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBuy,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB703),
                    disabledContainerColor = Color(0xFF383120)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${booster.price}💰",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) Color.Black else Color.Gray
                    )
                )
            }
        }
    }
}

@Composable
private fun ThemeShopCard(
    theme: GameTheme,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    coins: Int,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val canAfford = coins >= theme.priceCoins

    Surface(
        color = if (isEquipped) Color(0xFF2C194D) else Color(0xFF1E143B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                color = if (isEquipped) Color(0xFFFFB703) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.symbols.firstOrNull()?.first ?: "🎨",
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(theme.nameRes),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = theme.category,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4CC9F0))
                        )
                    }
                }

                if (isEquipped) {
                    Surface(
                        color = Color(0xFFFFB703),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.shop_equipped), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                        }
                    }
                } else if (isUnlocked) {
                    OutlinedButton(
                        onClick = onEquip,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.shop_equip), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onBuy,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7209B7),
                            disabledContainerColor = Color(0xFF2D263B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${theme.priceCoins} 💰",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preview symbols
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                theme.symbols.take(6).forEach { (sym, _) ->
                    Surface(
                        color = Color(0xFF2B2050),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = sym, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameShopCard(
    frame: ShopItem,
    isEquipped: Boolean,
    coins: Int,
    onBuy: () -> Unit
) {
    val canAfford = coins >= frame.price
    val frameBorder = when (frame.id) {
        "frame_golden" -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
        "frame_neon" -> Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFFF72585)))
        "frame_magic" -> Brush.linearGradient(listOf(Color(0xFF9D4EDD), Color(0xFFE0AAFF)))
        else -> Brush.linearGradient(listOf(Color(0xFF4CC9F0), Color(0xFF7209B7)))
    }

    Surface(
        color = Color(0xFF231842),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, frameBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(frame.titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = stringResource(frame.descriptionRes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isEquipped) {
                Text(
                    text = "EM USO ✨",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06D6A0)
                    )
                )
            } else if (frame.isOwned) {
                OutlinedButton(
                    onClick = onBuy,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Usar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7209B7),
                        disabledContainerColor = Color(0xFF2D263B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("${frame.price} 💰", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
