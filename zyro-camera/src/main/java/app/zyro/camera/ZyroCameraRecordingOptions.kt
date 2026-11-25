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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Options that a compatible host recorder may apply to its Camera2 capture session. */
data class ZyroAdvancedVideoOptions(
    val electronicStabilization: Boolean,
    val opticalStabilization: Boolean,
    val dynamicRangeProfile: String
)

private val AdvancedPanel = Color(0xFF171326)
private val AdvancedText = Color(0xFFF5F0FF)
private val AdvancedMuted = Color(0xFFAAA2BC)

/**
 * Shows only stabilization and dynamic-range modes advertised by this camera.
 * The host must still validate and apply the choices when configuring Camera2.
 */
@Composable
fun ZyroCameraRecordingOptions(
    camera: ZyroCameraInfo,
    modifier: Modifier = Modifier,
    onOptionsChanged: (ZyroAdvancedVideoOptions) -> Unit = {}
) {
    val hdrOptions = remember(camera.cameraId, camera.hdrProfiles) {
        camera.hdrProfiles.filterNot { it.equals("SDR", ignoreCase = true) }
    }
    var electronic by remember(camera.cameraId) {
        mutableStateOf(camera.videoStabilization == true)
    }
    var optical by remember(camera.cameraId) {
        mutableStateOf(camera.opticalStabilization == true)
    }
    var dynamicRange by remember(camera.cameraId, hdrOptions) {
        mutableStateOf(hdrOptions.firstOrNull() ?: "SDR")
    }

    LaunchedEffect(camera.cameraId, electronic, optical, dynamicRange) {
        onOptionsChanged(
            ZyroAdvancedVideoOptions(
                electronicStabilization = electronic,
                opticalStabilization = optical,
                dynamicRangeProfile = dynamicRange
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AdvancedPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Дополнительные параметры", color = AdvancedText, style = MaterialTheme.typography.titleMedium)
            Text("Стабилизация", color = AdvancedText, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = electronic,
                    enabled = camera.videoStabilization == true,
                    onClick = { electronic = !electronic },
                    label = { Text("EIS") }
                )
                FilterChip(
                    selected = optical,
                    enabled = camera.opticalStabilization == true,
                    onClick = { optical = !optical },
                    label = { Text("OIS") }
                )
            }
            if (camera.videoStabilization != true && camera.opticalStabilization != true) {
                Text("Эта камера не объявила поддержку переключаемой стабилизации.", color = AdvancedMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("Динамический диапазон", color = AdvancedText, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = dynamicRange == "SDR",
                    onClick = { dynamicRange = "SDR" },
                    label = { Text("SDR") }
                )
                hdrOptions.forEach { profile ->
                    FilterChip(
                        selected = dynamicRange == profile,
                        onClick = { dynamicRange = profile },
                        label = { Text(profile) }
                    )
                }
            }
            if (hdrOptions.isEmpty()) {
                Text("HDR-профили не заявлены. Доступен только SDR.", color = AdvancedMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Параметры передаются хост-приложению как настройки. Для реального эффекта рекордер должен применить их к совместимой сессии Camera2.",
                color = AdvancedMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
