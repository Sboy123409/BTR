package com.example.btrapp.avatar

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The saved avatar, one preference key per field. One instance per process via [get]. */
class AvatarStore private constructor(private val prefs: SharedPreferences) {

    private val _config = MutableStateFlow(read())
    val config: StateFlow<AvatarConfig> = _config.asStateFlow()

    fun save(config: AvatarConfig) {
        prefs.edit {
            putString(KEY_NAME, config.name)
            putInt(KEY_SKIN, config.skin)
            putString(KEY_FACE, config.face.name)
            putString(KEY_HAIR, config.hair.name)
            putInt(KEY_HAIR_COLOR, config.hairColor)
            putString(KEY_EYES, config.eyes.name)
            putString(KEY_BROWS, config.brows.name)
            putString(KEY_OUTFIT, config.outfit.name)
            putInt(KEY_OUTFIT_COLOR, config.outfitColor)
            putString(KEY_ACCESSORY, config.accessory.name)
            putInt(KEY_BACKGROUND, config.background)
        }
        _config.value = config
    }

    private fun read(): AvatarConfig {
        val d = AvatarConfig()
        return AvatarConfig(
            name = prefs.getString(KEY_NAME, d.name).orEmpty(),
            skin = prefs.getInt(KEY_SKIN, d.skin).coerceIn(AvatarPalette.skin.indices),
            face = enumOr(prefs.getString(KEY_FACE, null), d.face),
            hair = enumOr(prefs.getString(KEY_HAIR, null), d.hair),
            hairColor = prefs.getInt(KEY_HAIR_COLOR, d.hairColor).coerceIn(AvatarPalette.hair.indices),
            eyes = enumOr(prefs.getString(KEY_EYES, null), d.eyes),
            brows = enumOr(prefs.getString(KEY_BROWS, null), d.brows),
            outfit = enumOr(prefs.getString(KEY_OUTFIT, null), d.outfit),
            outfitColor = prefs.getInt(KEY_OUTFIT_COLOR, d.outfitColor).coerceIn(AvatarPalette.outfit.indices),
            accessory = enumOr(prefs.getString(KEY_ACCESSORY, null), d.accessory),
            background = prefs.getInt(KEY_BACKGROUND, d.background).coerceIn(AvatarPalette.background.indices),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    companion object {
        private const val PREFS = "avatar"
        private const val KEY_NAME = "name"
        private const val KEY_SKIN = "skin"
        private const val KEY_FACE = "face"
        private const val KEY_HAIR = "hair"
        private const val KEY_HAIR_COLOR = "hair_color"
        private const val KEY_EYES = "eyes"
        private const val KEY_BROWS = "brows"
        private const val KEY_OUTFIT = "outfit"
        private const val KEY_OUTFIT_COLOR = "outfit_color"
        private const val KEY_ACCESSORY = "accessory"
        private const val KEY_BACKGROUND = "background"

        @Volatile
        private var instance: AvatarStore? = null

        fun get(context: Context): AvatarStore =
            instance ?: synchronized(this) {
                instance ?: AvatarStore(
                    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
    }
}
