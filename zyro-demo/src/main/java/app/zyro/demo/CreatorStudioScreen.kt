package app.zyro.demo

import android.content.ContentValues
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class CreatorVideoDetails(
    val name: String,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?
)

@OptIn(UnstableApi::class)
@Composable
internal fun CreatorStudioScreen(initialUri: Uri? = null, initialSharedText: String? = null) {
    val context = LocalContext.current
    var uriText by rememberSaveable { mutableStateOf<String?>(initialUri?.toString()) }
    var sharedText by rememberSaveable { mutableStateOf(initialSharedText) }
    var playbackSpeed by rememberSaveable { mutableStateOf(1f) }
    var trimStartMs by rememberSaveable { mutableStateOf(0f) }
    var trimEndMs by rememberSaveable { mutableStateOf(0f) }
    var exporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportedUriText by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) sharedText = initialSharedText
    }
    LaunchedEffect(initialUri) {
        if (initialUri != null) uriText = initialUri.toString()
    }

    val uri = remember(uriText) { uriText?.let(Uri::parse) }
    var details by remember(uri) { mutableStateOf<CreatorVideoDetails?>(null) }
    var loading by remember(uri) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selected ->
        if (selected != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    selected,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            uriText = selected.toString()
            exportedUriText = null
            exportError = null
        }
    }

    LaunchedEffect(uri) {
        if (uri == null) {
            details = null
            trimStartMs = 0f
            trimEndMs = 0f
            return@LaunchedEffect
        }
        loading = true
        details = withContext(Dispatchers.IO) {
            runCatching {
                val name = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        val fileName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                        val fileSize = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                        (fileName ?: "Видео") to fileSize
                    } else "Видео" to null
                } ?: ("Видео" to null)

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    CreatorVideoDetails(
                        name = name.first,
                        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                        width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                        height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                        sizeBytes = name.second
                    )
                } finally {
                    retriever.release()
                }
            }.getOrNull()
        }
        trimStartMs = 0f
        trimEndMs = (details?.durationMs ?: 0L).toFloat()
        loading = false
    }

    val player = remember(uri) {
        uri?.let { selectedUri ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(selectedUri))
                playWhenReady = false
                prepare()
            }
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    fun startExport() {
        val source = uri ?: return
        val duration = details?.durationMs ?: return
        val startMs = trimStartMs.toLong().coerceIn(0L, duration - 1L)
        val endMs = trimEndMs.toLong().coerceIn(startMs + 1L, duration)
        if (Build.VERSION.SDK_INT < 29 || endMs - startMs < 1000L) return

        exporting = true
        exportError = null
        exportedUriText = null
        val outputFile = File(context.cacheDir, "zyro_trim_${System.currentTimeMillis()}.mp4")
        val clippedItem = MediaItem.Builder()
            .setUri(source)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()
        val editedItem = EditedMediaItem.Builder(clippedItem).build()
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Thread {
                        var publishedUri: Uri? = null
                        try {
                            val values = ContentValues().apply {
                                put(MediaStore.Video.Media.DISPLAY_NAME, "ZYRO_${System.currentTimeMillis()}.mp4")
                                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ZYRO")
                                put(MediaStore.Video.Media.IS_PENDING, 1)
                            }
                            val destination = context.contentResolver.insert(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                values
                            ) ?: error("Android не создал файл в медиатеке")
                            publishedUri = destination
                            context.contentResolver.openOutputStream(destination)?.use { output ->
                                FileInputStream(outputFile).use { input -> input.copyTo(output) }
                            } ?: error("Не удалось открыть файл для сохранения")
                            val ready = ContentValues().apply {
                                put(MediaStore.Video.Media.IS_PENDING, 0)
                            }
                            context.contentResolver.update(destination, ready, null, null)
                            Handler(Looper.getMainLooper()).post {
                                exportedUriText = destination.toString()
                                exporting = false
                            }
                        } catch (error: Exception) {
                            publishedUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                            Handler(Looper.getMainLooper()).post {
                                exportError = error.message ?: "Не удалось сохранить результат"
                                exporting = false
                            }
                        } finally {
                            outputFile.delete()
                        }
                    }.start()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    outputFile.delete()
                    exportError = exportException.message ?: "Ошибка обработки видео"
                    exporting = false
                }
            })
            .build()
        runCatching {
            transformer.start(editedItem, outputFile.absolutePath)
        }.onFailure {
            outputFile.delete()
            exportError = it.message ?: "Не удалось начать экспорт"
            exporting = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF090711))
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("ZYRO CREATOR", color = Color(0xFFB56CFF),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text("Медиастудия", color = Color(0xFFF5F0FF),
            fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "Импортируй видео, смотри предпросмотр, обрезай нужный фрагмент и сохраняй копию в галерею.",
            color = Color(0xFFAAA2BC), fontSize = 14.sp
        )

        sharedText?.let { textValue ->
            val sharedUri = runCatching { Uri.parse(textValue.trim()) }.getOrNull()
            val isWebLink = sharedUri?.scheme == "https" || sharedUri?.scheme == "http"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171326))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Полученная ссылка или текст", color = Color(0xFFF5F0FF), fontWeight = FontWeight.Bold)
                    Text(textValue, color = Color(0xFFAAA2BC), fontSize = 12.sp)
                    if (isWebLink && sharedUri != null) {
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, sharedUri))
                        }) { Text("Открыть ссылку") }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(arrayOf("video/*")) }) {
                Text(if (uri == null) "Выбрать видео" else "Заменить видео")
            }
            if (uri != null) OutlinedButton(onClick = {
                uriText = null
                exportedUriText = null
                exportError = null
            }) { Text("Сбросить") }
        }

        if (loading) Text("Читаю информацию о видео…", color = Color(0xFFAAA2BC))

        if (player != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171326))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    update = { it.player = player }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        FilterChip(
                            selected = playbackSpeed == speed,
                            onClick = {
                                playbackSpeed = speed
                                player.setPlaybackSpeed(speed)
                            },
                            label = { Text("${speed}×") }
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(190.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171326))
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                    Text("▣", color = Color(0xFFB56CFF), fontSize = 34.sp)
                    Text("Предпросмотр появится здесь", color = Color(0xFFF5F0FF), fontWeight = FontWeight.SemiBold)
                    Text("Поддерживаются локальные видеофайлы, доступные Android.", color = Color(0xFFAAA2BC), fontSize = 12.sp)
                }
            }
        }

        details?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171326))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Информация о файле", color = Color(0xFFF5F0FF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(info.name, color = Color(0xFFAAA2BC), fontSize = 13.sp)
                    Text("Разрешение: ${info.width ?: "—"} × ${info.height ?: "—"}", color = Color(0xFFF5F0FF))
                    Text("Длительность: ${info.durationMs?.let(::formatVideoDuration) ?: "—"}", color = Color(0xFFF5F0FF))
                    Text("Размер: ${info.sizeBytes?.let(::formatVideoSize) ?: "неизвестен"}", color = Color(0xFFF5F0FF))
                }
            }

            if (Build.VERSION.SDK_INT >= 29 && (info.durationMs ?: 0L) >= 1000L) {
                val duration = info.durationMs!!.toFloat()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171326))
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Обрезка видео", color = Color(0xFFF5F0FF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Начало: ${formatVideoDuration(trimStartMs.toLong())}", color = Color(0xFFAAA2BC))
                        Slider(
                            value = trimStartMs.coerceIn(0f, (duration - 1000f).coerceAtLeast(0f)),
                            onValueChange = { trimStartMs = it.coerceAtMost(trimEndMs - 1000f) },
                            valueRange = 0f..(duration - 1000f).coerceAtLeast(0f),
                            enabled = !exporting
                        )
                        Text("Конец: ${formatVideoDuration(trimEndMs.toLong())}", color = Color(0xFFAAA2BC))
                        Slider(
                            value = trimEndMs.coerceIn(1000f, duration),
                            onValueChange = { trimEndMs = it.coerceAtLeast(trimStartMs + 1000f) },
                            valueRange = 1000f..duration,
                            enabled = !exporting
                        )
                        Text(
                            "Выбранный фрагмент: ${formatVideoDuration((trimEndMs - trimStartMs).toLong())}",
                            color = Color(0xFFF5F0FF)
                        )
                        Button(
                            onClick = { startExport() },
                            enabled = !exporting && trimEndMs - trimStartMs >= 1000f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (exporting) "Обрабатываю видео…" else "Обрезать и сохранить копию")
                        }
                        if (exporting) Text("Экспорт выполняется на устройстве. Не закрывай приложение до завершения.", color = Color(0xFFAAA2BC), fontSize = 12.sp)
                        exportError?.let { Text(it, color = Color(0xFFFF8A8A), fontSize = 12.sp) }
                        exportedUriText?.let { saved ->
                            Text("Готово! Копия сохранена в Movies/ZYRO.", color = Color(0xFF8FE3B0))
                            OutlinedButton(
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(saved))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(send, "Поделиться обработанным видео"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Поделиться результатом") }
                        }
                    }
                }
            } else if (Build.VERSION.SDK_INT < 29) {
                Text("Экспорт обрезанного видео требует Android 10 или новее. Просмотр и отправка исходника доступны.", color = Color(0xFFAAA2BC), fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Поделиться исходным видео"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Поделиться исходным видео") }
        }

        Text(
            "ZYRO обрабатывает только выбранный тобой файл локально. Автоматической загрузки в сеть нет.",
            color = Color(0xFFAAA2BC), fontSize = 12.sp
        )
    }
}

private fun formatVideoDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun formatVideoSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
