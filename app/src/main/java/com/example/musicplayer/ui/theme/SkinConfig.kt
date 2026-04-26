package com.example.musicplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PlayerBackground { STANDARD, GRADIENT, ARTWORK }

data class SkinConfig(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val shapes: Shapes,
    val cardElevation: Dp,
    val cardOutlined: Boolean,
    val playerBackground: PlayerBackground
)

val modernSkin = SkinConfig(
    id = "modern",
    name = "Modern",
    description = "Clean Material 3 design with balanced corners",
    emoji = "✦",
    shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp)
    ),
    cardElevation = 2.dp,
    cardOutlined = false,
    playerBackground = PlayerBackground.STANDARD
)

val minimalSkin = SkinConfig(
    id = "minimal",
    name = "Minimal",
    description = "Flat, outlined cards with sharp corners",
    emoji = "▭",
    shapes = Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(12.dp)
    ),
    cardElevation = 0.dp,
    cardOutlined = true,
    playerBackground = PlayerBackground.STANDARD
)

val roundedSkin = SkinConfig(
    id = "rounded",
    name = "Rounded",
    description = "Bubbly pill-shaped UI with soft edges",
    emoji = "◉",
    shapes = Shapes(
        extraSmall = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(18.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp)
    ),
    cardElevation = 3.dp,
    cardOutlined = false,
    playerBackground = PlayerBackground.STANDARD
)

val gradientSkin = SkinConfig(
    id = "gradient",
    name = "Gradient",
    description = "Vivid gradient player background",
    emoji = "◈",
    shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp)
    ),
    cardElevation = 4.dp,
    cardOutlined = false,
    playerBackground = PlayerBackground.GRADIENT
)

val artworkSkin = SkinConfig(
    id = "artwork",
    name = "Artwork",
    description = "Album art fills the player background",
    emoji = "◧",
    shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp)
    ),
    cardElevation = 6.dp,
    cardOutlined = false,
    playerBackground = PlayerBackground.ARTWORK
)

val allSkins = listOf(modernSkin, minimalSkin, roundedSkin, gradientSkin, artworkSkin)

fun skinById(id: String): SkinConfig = allSkins.find { it.id == id } ?: modernSkin
