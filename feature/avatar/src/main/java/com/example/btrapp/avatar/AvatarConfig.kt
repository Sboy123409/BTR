package com.example.btrapp.avatar

import androidx.compose.ui.graphics.Color
import com.example.btrapp.core.ui.theme.PosterBlack
import com.example.btrapp.core.ui.theme.PosterCream
import com.example.btrapp.core.ui.theme.PosterRed
import kotlin.random.Random

enum class FaceShape(val label: String) { Round("Round"), Oval("Oval"), Square("Square") }

enum class HairStyle(val label: String) {
    None("Bald"), Short("Short"), Spiky("Spiky"), Bob("Bob"), Long("Long"), Bun("Bun"), Cap("Cap"),
}

enum class EyeStyle(val label: String) { Dots("Dots"), Round("Round"), Anime("Big"), Sharp("Sharp") }

enum class BrowStyle(val label: String) { Soft("Soft"), Bold("Bold"), Flat("Flat") }

enum class OutfitStyle(val label: String) { Tee("Tee"), Hoodie("Hoodie"), Collar("Collar"), Tank("Tank") }

enum class Accessory(val label: String) {
    None("None"), Glasses("Glasses"), Sunglasses("Shades"), Headphones("Headphones"), Bandana("Bandana"),
}

/** Colours are indexes into these palettes so saved avatars survive palette tweaks. */
object AvatarPalette {
    val skin = listOf(
        Color(0xFFFFE0BD), Color(0xFFFFCD94), Color(0xFFEAC086),
        Color(0xFFC68642), Color(0xFF8D5524), Color(0xFF5C3A21),
    )
    val hair = listOf(
        Color(0xFF1C1C1C), Color(0xFF4A2C1A), Color(0xFF8B5A2B), Color(0xFFE3B04B),
        PosterRed, Color(0xFF8E6CEF), Color(0xFF2E86DE), Color(0xFFB8B8B8),
    )
    val outfit = listOf(
        PosterRed, PosterBlack, PosterCream, Color(0xFF2E86DE),
        Color(0xFF27AE60), Color(0xFFF2C94C), Color(0xFF9B51E0), Color(0xFFFFFFFF),
    )
    val background = listOf(
        PosterCream, PosterRed, Color(0xFF87CEEB), Color(0xFFB8E994),
        Color(0xFFFFD3B6), Color(0xFFD6C1FF), Color(0xFF2D3436), Color(0xFFFFFFFF),
    )
}

data class AvatarConfig(
    val name: String = "",
    val skin: Int = 1,
    val face: FaceShape = FaceShape.Round,
    val hair: HairStyle = HairStyle.Spiky,
    val hairColor: Int = 0,
    val eyes: EyeStyle = EyeStyle.Round,
    val brows: BrowStyle = BrowStyle.Soft,
    val outfit: OutfitStyle = OutfitStyle.Hoodie,
    val outfitColor: Int = 0,
    val accessory: Accessory = Accessory.None,
    val background: Int = 0,
) {
    /** A random look that keeps the current name. */
    fun shuffled(random: Random = Random): AvatarConfig = copy(
        skin = random.nextInt(AvatarPalette.skin.size),
        face = FaceShape.entries.random(random),
        hair = HairStyle.entries.random(random),
        hairColor = random.nextInt(AvatarPalette.hair.size),
        eyes = EyeStyle.entries.random(random),
        brows = BrowStyle.entries.random(random),
        outfit = OutfitStyle.entries.random(random),
        outfitColor = random.nextInt(AvatarPalette.outfit.size),
        accessory = if (random.nextInt(3) == 0) Accessory.entries.random(random) else Accessory.None,
        background = random.nextInt(AvatarPalette.background.size),
    )
}

/** The avatar's expression. Ordered from least to most cheerful. */
enum class Mood {
    Sleepy, Neutral, Happy, Ecstatic;

    companion object {
        /** All done → ecstatic, some done → happy, nothing done late at night → sleepy. */
        fun from(done: Int, total: Int, hourOfDay: Int): Mood = when {
            total > 0 && done == total -> Ecstatic
            done > 0 -> Happy
            hourOfDay >= 21 || hourOfDay < 6 -> Sleepy
            else -> Neutral
        }
    }
}
