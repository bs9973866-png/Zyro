package app.zyro.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Numeric Camera2 AE range, retained for capability-based option filtering. */
data class ZyroFpsRange(val lower: Int, val upper: Int) {
    fun supports(fps: Int): Boolean = lower <= fps && upper >= fps
}

enum class ZyroVideoQuality(val label: String, val targetLongEdge: Int) {
    SD("480p", 854),
    HD("720p", 1280),
    FULL_HD("1080p", 1920),
    QHD("1440p", 2560),
    UHD("4K", 3840),
    MAX("Максимум", Int.MAX_VALUE)
}

data class ZyroVideoSelection(
    val quality: ZyroVideoQuality,
    val size: ZyroVideoSize,
    val fps: Int
)

/**
 * Returns only resolutions and frame rates reported by Camera2.
 * This is a configuration suggestion; the host recorder must still validate
 * the complete stream combination when it creates its capture session.
 */
object ZyroVideoOptions {
    fun availableQualities(camera: ZyroCameraInfo): List<ZyroVideoQuality> {
        val sizes = camera.recordingSizes
        if (sizes.isEmpty()) return emptyList()
        return ZyroVideoQuality.entries.filter { quality ->
            quality == ZyroVideoQuality.MAX ||
                sizes.any { maxOf(it.width, it.height) >= quality.targetLongEdge }
        }
    }

    fun sizeFor(camera: ZyroCameraInfo, quality: ZyroVideoQuality): ZyroVideoSize? {
        val sizes = camera.recordingSizes
        if (sizes.isEmpty()) return null
        val target = if (quality == ZyroVideoQuality.MAX) {
            sizes.maxOf { maxOf(it.width, it.height) }
        } else quality.targetLongEdge

        return sizes.minWithOrNull(
            compareBy<ZyroVideoSize> {
                kotlin.math.abs(maxOf(it.width, it.height) - target)
            }.thenByDescending { it.width.toLong() * it.height }
        )
    }

    fun availableFps(camera: ZyroCameraInfo): List<Int> {
        val candidates = listOf(24, 25, 30, 60, 90, 120)
        return candidates.filter { fps -> camera.fpsRanges.any { it.supports(fps) } }
    }
}

private val OptionPanel = Color(0xFF171326)
private val OptionAccent = Color(0xFFB56CFF)
private val OptionText = Color(0xFFF5F0FF)
private val OptionMuted = Color(0xFFAAA2BC)

/**
 * Capability-aware quality selector. The host receives the selected size/FPS
 * and is responsible for applying it to its own supported recording pipeline.
 */
@Composable
fun ZyroVideoQualitySelector(
    camera: ZyroCameraInfo,
    modifier: Modifier = Modifier,
    onSelectionChanged: (ZyroVideoSelection) -> Unit = {}
) {
    val qualities = remember(camera.cameraId, camera.recordingSizes) {
        ZyroVideoOptions.availableQualities(camera)
    }
    val fpsOptions = remember(camera.cameraId, camera.fpsRanges) {
        ZyroVideoOptions.availableFps(camera)
    }
    var selectedQuality by remember(camera.cameraId) {
        mutableStateOf(qualities.firstOrNull { it == ZyroVideoQuality.FULL_HD }
            ?: qualities.lastOrNull())
    }
    var selectedFps by remember(camera.cameraId) {
        mutableStateOf(if (30 in fpsOptions) 30 else fpsOptions.firstOrNull())
    }
    val selectedSize = selectedQuality?.let { ZyroVideoOptions.sizeFor(camera, it) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OptionPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Качество видео", color = OptionText, fontWeight = FontWeight.Bold)
            Text(
                "Доступны только режимы, которые объявила камера этого устройства.",
                color = OptionMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text("Разрешение", color = OptionText, fontWeight = FontWeight.SemiBold)
            if (qualities.isEmpty()) {
                Text("Нет данных о поддерживаемых разрешениях", color = OptionMuted)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    qualities.forEach { quality ->
                        FilterChip(
                            selected = selectedQuality == quality,
                            onClick = {
                                selectedQuality = quality
                                val size = ZyroVideoOptions.sizeFor(camera, quality)
                                val fps = selectedFps
                                if (size != null && fps != null) {
                                    onSelectionChanged(ZyroVideoSelection(quality, size, fps))
                                }
                            },
                            label = { Text(quality.label) }
                        )
                    }
                }
                selectedSize?.let {
                    Text(
                        "Выбрано: ${it.width} × ${it.height}",
                        color = OptionAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text("Частота кадров", color = OptionText, fontWeight = FontWeight.SemiBold)
            if (fpsOptions.isEmpty()) {
                Text("Нет подтверждённых режимов FPS", color = OptionMuted)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fpsOptions.forEach { fps ->
                        FilterChip(
                            selected = selectedFps == fps,
                            onClick = {
                                selectedFps = fps
                                val quality = selectedQuality
                                val size = quality?.let { ZyroVideoOptions.sizeFor(camera, it) }
                                if (quality != null && size != null) {
                                    onSelectionChanged(ZyroVideoSelection(quality, size, fps))
                                }
                            },
                            label = { Text("${fps} FPS") }
                        )
                    }
                }
            }
            Text(
                "Это выбор целевых параметров для совместимого рекордера, а не гарантия фактической частоты кадров. Реальная комбинация потоков, нагрев и обработка приложения могут ограничить результат.",
                color = OptionMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
