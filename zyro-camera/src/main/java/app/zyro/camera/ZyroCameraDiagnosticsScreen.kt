package app.zyro.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private val CameraBg = Color(0xFF090711)
private val CameraPanel = Color(0xFF171326)
private val CameraAccent = Color(0xFFB56CFF)
private val CameraText = Color(0xFFF5F0FF)
private val CameraMuted = Color(0xFFAAA2BC)

/**
 * Camera2 capability viewer. It reads metadata and does not start preview or recording.
 */
@Composable
fun ZyroCameraDiagnosticsScreen(
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }
    var report by remember { mutableStateOf<ZyroCameraReport?>(null) }
    var selectedCameraId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(report) {
        val ids = report?.cameras.orEmpty().map { it.cameraId }
        if (selectedCameraId !in ids) selectedCameraId = ids.firstOrNull()
    }

    LaunchedEffect(hasCameraPermission) {
        isScanning = true
        report = runCatching { ZyroCameraDiagnostics.scan(context) }
            .getOrElse {
                ZyroCameraReport(emptyList(), listOf("Ошибка диагностики: ${it.message ?: "неизвестная ошибка"}"))
            }
        isScanning = false
    }

    MaterialTheme {
        Column(
            modifier.fillMaxSize()
                .background(CameraBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ZYRO", color = CameraAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Диагностика камеры", color = CameraText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Возможности Camera2 на этом устройстве", color = CameraMuted, fontSize = 13.sp)
                }
                if (onClose != null) {
                    OutlinedButton(onClick = onClose) { Text("Закрыть") }
                }
            }

            CameraPanelCard {
                Text(
                    "Режим проверки",
                    color = CameraText,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "ZYRO читает системные характеристики. Камера не включается, фото и видео не записываются.",
                    color = CameraMuted,
                    fontSize = 13.sp
                )
                if (!hasCameraPermission) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Разрешить подробную диагностику")
                    }
                    Text(
                        if (permissionDenied) {
                            "Разрешение не выдано. Доступные базовые сведения всё равно будут показаны."
                        } else {
                            "Без разрешения Android может скрывать часть размеров и режимов FPS. Разрешение можно не выдавать."
                        },
                        color = CameraMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Text("Разрешение CAMERA предоставлено", color = CameraAccent, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isScanning = true
                            report = runCatching { ZyroCameraDiagnostics.scan(context) }
                                .getOrElse {
                                    ZyroCameraReport(emptyList(), listOf("Ошибка диагностики: ${it.message ?: "неизвестная ошибка"}"))
                                }
                            isScanning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Обновить данные")
                }
            }

            if (isScanning) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = CameraAccent)
                    Text("  Сканирование характеристик…", color = CameraMuted)
                }
            } else {
                val currentReport = report
                if (currentReport == null || currentReport.cameras.isEmpty()) {
                    CameraPanelCard {
                        Text("Камеры не обнаружены", color = CameraText, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Проверь системные разрешения камеры и повтори сканирование.",
                            color = CameraMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Text(
                        "Найдено камер: ${currentReport.cameras.size}",
                        color = CameraText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentReport.cameras.forEach { camera ->
                            FilterChip(
                                selected = selectedCameraId == camera.cameraId,
                                onClick = { selectedCameraId = camera.cameraId },
                                label = { Text("${camera.lensFacing} · ${camera.cameraId}") }
                            )
                        }
                    }
                    val activeCamera = currentReport.cameras.firstOrNull {
                        it.cameraId == selectedCameraId
                    } ?: currentReport.cameras.first()
                    CameraInfoCard(activeCamera)
                    ZyroVideoQualitySelector(camera = activeCamera)
                    ZyroCameraRecordingOptions(camera = activeCamera)
                }

                currentReport?.warnings?.forEach { warning ->
                    CameraPanelCard {
                        Text("Примечание", color = CameraAccent, fontWeight = FontWeight.SemiBold)
                        Text(warning, color = CameraMuted, fontSize = 12.sp)
                    }
                }
            }

            Text(
                "Диапазон FPS — это значение, объявленное драйвером Camera2. Оно не гарантирует такую частоту записи, обработки или загрузки в TikTok.",
                color = CameraMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CameraInfoCard(camera: ZyroCameraInfo) {
    CameraPanelCard {
        Text(camera.lensFacing, color = CameraText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Camera ID: ${camera.cameraId} · Уровень Camera2: ${camera.hardwareLevel}", color = CameraAccent, fontSize = 12.sp)
        InfoLine("Ориентация сенсора", camera.sensorOrientation?.let { "$it°" } ?: "Нет данных")
        InfoLine("Вспышка", camera.flashAvailable?.let { if (it) "Есть" else "Нет" } ?: "Нет данных")

        Text("Режимы автоэкспозиции / FPS", color = CameraText, fontWeight = FontWeight.SemiBold)
        Text(camera.aeFpsRanges.ifEmpty { listOf("Не предоставлены системой") }.joinToString(", "), color = CameraMuted, fontSize = 12.sp)

        Text("Размеры видеопотока", color = CameraText, fontWeight = FontWeight.SemiBold)
        if (camera.recordingSizes.isEmpty()) {
            Text("Данные недоступны", color = CameraMuted, fontSize = 12.sp)
        } else {
            camera.recordingSizes.take(8).forEach { size ->
                Text(
                    "${size.width} × ${size.height} · ${"%.1f".format(java.util.Locale.US, size.megapixels)} Мп",
                    color = CameraMuted,
                    fontSize = 12.sp
                )
            }
            if (camera.recordingSizes.size > 8) {
                Text("И ещё размеров: ${camera.recordingSizes.size - 8}", color = CameraMuted, fontSize = 11.sp)
            }
        }

        Text("Constrained high-speed режимы", color = CameraText, fontWeight = FontWeight.SemiBold)
        Text(
            camera.highSpeedModes.take(8).ifEmpty { listOf("Не заявлены") }.joinToString("\n"),
            color = CameraMuted,
            fontSize = 12.sp
        )

        InfoLine("Электронная стабилизация", supportedLabel(camera.videoStabilization))
        InfoLine("Оптическая стабилизация", supportedLabel(camera.opticalStabilization))
        InfoLine("HDR профили", camera.hdrProfiles.ifEmpty { listOf("Не указаны") }.joinToString(", "))

        if (camera.detailsMayBeLimited) {
            Text(
                "Часть сведений может быть скрыта Android без разрешения CAMERA.",
                color = CameraAccent,
                fontSize = 11.sp
            )
        }
    }
}

private fun supportedLabel(value: Boolean?): String = when (value) {
    true -> "Поддерживается"
    false -> "Не заявлена"
    null -> "Нет данных"
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CameraMuted, fontSize = 12.sp)
        Text(value, color = CameraText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CameraPanelCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CameraPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}
