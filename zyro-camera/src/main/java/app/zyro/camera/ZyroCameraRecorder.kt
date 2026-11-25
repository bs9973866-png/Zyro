package app.zyro.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.view.PreviewView
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val RecorderPanel = Color(0xFF171326)
private val RecorderAccent = Color(0xFFB56CFF)
private val RecorderText = Color(0xFFF5F0FF)
private val RecorderMuted = Color(0xFFAAA2BC)

/**
 * User-controlled CameraX preview and silent video recorder.
 *
 * Recording starts only after the user presses the record button. Files stay in
 * the host app's private files directory; the host can export them explicitly.
 * No microphone permission is requested and no background recording service is used.
 */
@Composable
fun ZyroCameraRecorder(
    modifier: Modifier = Modifier,
    quality: ZyroVideoQuality = ZyroVideoQuality.FULL_HD,
    onVideoSaved: (Uri) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val currentOnVideoSaved by rememberUpdatedState(onVideoSaved)
    val currentOnError by rememberUpdatedState(onError)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var selectedLens by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val activeRecording = remember { AtomicReference<Recording?>(null) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) cameraError = "Без разрешения CAMERA предпросмотр и запись недоступны."
    }

    LaunchedEffect(hasPermission, selectedLens, quality, lifecycleOwner, previewView) {
        if (!hasPermission) return@LaunchedEffect

        isCameraReady = false
        videoCapture = null
        cameraError = null

        try {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val provider = suspendCancellableCoroutine { continuation ->
                providerFuture.addListener(
                    {
                        try {
                            if (continuation.isActive) continuation.resume(providerFuture.get())
                        } catch (error: Throwable) {
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                    },
                    mainExecutor
                )
            }

            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val requestedQuality = quality.toCameraXQuality()
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        requestedQuality,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                )
                .build()
            val capture = VideoCapture.withOutput(recorder)
            val selector = CameraSelector.Builder()
                .requireLensFacing(selectedLens)
                .build()

            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            cameraProvider = provider
            videoCapture = capture
            isCameraReady = true
        } catch (error: Exception) {
            cameraError = error.message ?: "Не удалось запустить камеру."
            currentOnError(cameraError ?: "Ошибка камеры")
        }
    }

    DisposableEffect(cameraProvider) {
        onDispose {
            activeRecording.getAndSet(null)?.stop()
            cameraProvider?.unbindAll()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = RecorderPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Камера ZYRO", color = RecorderText, style = MaterialTheme.typography.titleLarge)
            if (!hasPermission) {
                Text(
                    "Камера включится только после твоего разрешения.",
                    color = RecorderMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Разрешить камеру")
                }
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedLens == CameraSelector.LENS_FACING_BACK,
                        enabled = !isRecording,
                        onClick = { selectedLens = CameraSelector.LENS_FACING_BACK },
                        label = { Text("Основная") }
                    )
                    FilterChip(
                        selected = selectedLens == CameraSelector.LENS_FACING_FRONT,
                        enabled = !isRecording,
                        onClick = { selectedLens = CameraSelector.LENS_FACING_FRONT },
                        label = { Text("Фронтальная") }
                    )
                }

                if (isRecording) {
                    Text("● ИДЁТ ЗАПИСЬ", color = Color(0xFFFF6B83))
                    OutlinedButton(
                        onClick = { activeRecording.getAndSet(null)?.stop() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Остановить запись")
                    }
                } else {
                    Button(
                        onClick = {
                            val capture = videoCapture
                            if (capture == null) {
                                currentOnError("Камера ещё не готова.")
                            } else {
                                runCatching {
                                    val directory = File(context.filesDir, "Movies/ZYRO")
                                    check(directory.exists() || directory.mkdirs()) {
                                        "Не удалось создать папку для видео."
                                    }
                                    val outputFile = File(
                                        directory,
                                        "zyro-${System.currentTimeMillis()}.mp4"
                                    )
                                    val outputOptions = FileOutputOptions.Builder(outputFile).build()
                                    val newRecording = capture.output
                                        .prepareRecording(context, outputOptions)
                                        .start(mainExecutor) { event ->
                                            when (event) {
                                                is VideoRecordEvent.Start -> isRecording = true
                                                is VideoRecordEvent.Finalize -> {
                                                    isRecording = false
                                                    activeRecording.set(null)
                                                    if (event.hasError()) {
                                                        outputFile.delete()
                                                        currentOnError(
                                                            "Запись не завершилась (код ${event.error})."
                                                        )
                                                    } else {
                                                        currentOnVideoSaved(event.outputResults.outputUri)
                                                    }
                                                }
                                            }
                                        }
                                    activeRecording.set(newRecording)
                                    isRecording = true
                                }.onFailure {
                                    cameraError = it.message ?: "Не удалось начать запись."
                                    currentOnError(cameraError ?: "Ошибка записи")
                                }
                            }
                        },
                        enabled = isCameraReady && videoCapture != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isCameraReady) "● Начать запись" else "Запуск камеры…")
                    }
                }

                Text(
                    "Пресет: ${quality.label}. Камера использует ближайшее поддерживаемое качество.",
                    color = RecorderMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Видео записывается без звука и сохраняется локально в приватной папке приложения. Для публикации хост-приложение должно отдельно экспортировать файл.",
                    color = RecorderMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            cameraError?.let {
                Text(it, color = Color(0xFFFF9AAE), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun ZyroVideoQuality.toCameraXQuality(): Quality = when (this) {
    ZyroVideoQuality.SD -> Quality.SD
    ZyroVideoQuality.HD -> Quality.HD
    ZyroVideoQuality.FULL_HD -> Quality.FHD
    ZyroVideoQuality.QHD -> Quality.QHD
    ZyroVideoQuality.UHD -> Quality.UHD
    ZyroVideoQuality.MAX -> Quality.HIGHEST
}
