package app.zyro.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zyroDataStore by preferencesDataStore(name = "zyro_settings")
enum class ZyroThemeMode { SYSTEM, DARK, LIGHT }

data class ZyroAppearanceSettings(
    val themeMode: ZyroThemeMode = ZyroThemeMode.DARK,
    val liquidGlassEnabled: Boolean = true,
    val blurEnabled: Boolean = true,
    val transparencyEnabled: Boolean = true,
    val surfaceIntensity: Float = 0.72f,
    val cornerRadius: Float = 24f,
    val animationStrength: Float = 0.65f,
    val reduceMotion: Boolean = false,
    val accentPreset: String = "violet"
)

class ZyroSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private object Keys {
        val theme = stringPreferencesKey("appearance.theme")
        val glass = booleanPreferencesKey("appearance.liquid_glass")
        val blur = booleanPreferencesKey("appearance.blur")
        val transparency = booleanPreferencesKey("appearance.transparency")
        val surface = floatPreferencesKey("appearance.surface_intensity")
        val radius = floatPreferencesKey("appearance.corner_radius")
        val animation = floatPreferencesKey("appearance.animation_strength")
        val reduceMotion = booleanPreferencesKey("accessibility.reduce_motion")
        val accent = stringPreferencesKey("appearance.accent")
    }

    val appearance: Flow<ZyroAppearanceSettings> = appContext.zyroDataStore.data.map { prefs ->
        ZyroAppearanceSettings(
            themeMode = runCatching {
                ZyroThemeMode.valueOf(prefs[Keys.theme] ?: ZyroThemeMode.DARK.name)
            }.getOrDefault(ZyroThemeMode.DARK),
            liquidGlassEnabled = prefs[Keys.glass] ?: true,
            blurEnabled = prefs[Keys.blur] ?: true,
            transparencyEnabled = prefs[Keys.transparency] ?: true,
            surfaceIntensity = (prefs[Keys.surface] ?: 0.72f).coerceIn(0f, 1f),
            cornerRadius = (prefs[Keys.radius] ?: 24f).coerceIn(8f, 36f),
            animationStrength = (prefs[Keys.animation] ?: 0.65f).coerceIn(0f, 1f),
            reduceMotion = prefs[Keys.reduceMotion] ?: false,
            accentPreset = prefs[Keys.accent] ?: "violet"
        )
    }

    suspend fun setThemeMode(value: ZyroThemeMode) = update { this[Keys.theme] = value.name }
    suspend fun setLiquidGlassEnabled(value: Boolean) = update { this[Keys.glass] = value }
    suspend fun setBlurEnabled(value: Boolean) = update { this[Keys.blur] = value }
    suspend fun setTransparencyEnabled(value: Boolean) = update { this[Keys.transparency] = value }
    suspend fun setSurfaceIntensity(value: Float) = update { this[Keys.surface] = value.coerceIn(0f, 1f) }
    suspend fun setCornerRadius(value: Float) = update { this[Keys.radius] = value.coerceIn(8f, 36f) }
    suspend fun setAnimationStrength(value: Float) = update { this[Keys.animation] = value.coerceIn(0f, 1f) }
    suspend fun setReduceMotion(value: Boolean) = update { this[Keys.reduceMotion] = value }
    suspend fun setAccentPreset(value: String) = update { this[Keys.accent] = value }

    suspend fun resetAppearance() {
        appContext.zyroDataStore.edit { p ->
            p.remove(Keys.theme); p.remove(Keys.glass); p.remove(Keys.blur)
            p.remove(Keys.transparency); p.remove(Keys.surface); p.remove(Keys.radius)
            p.remove(Keys.animation); p.remove(Keys.reduceMotion); p.remove(Keys.accent)
        }
    }

    private suspend fun update(block: MutablePreferences.() -> Unit) {
        appContext.zyroDataStore.edit(block)
    }
}
