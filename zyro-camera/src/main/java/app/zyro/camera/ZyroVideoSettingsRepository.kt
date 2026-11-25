package app.zyro.camera

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zyroVideoDataStore by preferencesDataStore(name = "zyro_video_preferences")

data class ZyroSavedVideoSettings(
    val quality: ZyroVideoQuality,
    val fps: Int
)

/** Stores each camera's preferred video resolution preset and target frame rate locally. */
class ZyroVideoSettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    private fun qualityKey(cameraId: String) =
        stringPreferencesKey("camera.$cameraId.quality")

    private fun fpsKey(cameraId: String) =
        intPreferencesKey("camera.$cameraId.fps")

    fun settings(cameraId: String): Flow<ZyroSavedVideoSettings?> =
        appContext.zyroVideoDataStore.data.map { preferences ->
            val qualityName = preferences[qualityKey(cameraId)]
            val fps = preferences[fpsKey(cameraId)]
            val quality = qualityName?.let {
                runCatching { ZyroVideoQuality.valueOf(it) }.getOrNull()
            }
            if (quality == null || fps == null) null
            else ZyroSavedVideoSettings(quality, fps)
        }

    suspend fun save(cameraId: String, selection: ZyroVideoSelection) {
        appContext.zyroVideoDataStore.edit { preferences ->
            preferences[qualityKey(cameraId)] = selection.quality.name
            preferences[fpsKey(cameraId)] = selection.fps
        }
    }

    suspend fun clear(cameraId: String) {
        appContext.zyroVideoDataStore.edit { preferences ->
            preferences.remove(qualityKey(cameraId))
            preferences.remove(fpsKey(cameraId))
        }
    }
}
