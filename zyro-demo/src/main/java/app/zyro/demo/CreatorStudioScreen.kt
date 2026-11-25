package app.zyro.demo

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class CreatorVideoDetails(
    val name: String,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?
)

@Composable
internal fun CreatorStudioScreen(initialUri: Uri? = null, initialSharedText: String? = null) {
    val context = LocalContext.current
    var uriText by rememberSaveable { mutableStateOf<String?>(initialUri?.toString()) }
    LaunchedEffect(initialUri) {
        if (initialUri != null) uriText = initialUri.toString()
    }
    var playbackSpeed by rememberSaveable { mutableStateOf(1f) }
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
        }
    }

    LaunchedEffect(uri) {
        if (uri == null) {
            details = null
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
                        val fileSize = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                            cursor.getLong(sizeIndex)
                        } else null
                        (fileName ?: "Видео") to fileSize
                    } else "Видео" to null
                } ?: ("Видео" to null)

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    CreatorVideoDetails(
                        name = name.first,
                        durationMs = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull(),
                        width = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                        )?.toIntOrNull(),
                        height = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                        )?.toIntOrNull(),
                        sizeBytes = name.second
                    )
                } finally {
                    retriever.release()
                }
            }.getOrNull()
        }
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
            "Выбирай видео из памяти телефона, смотри предпросмотр и отправляй его в установленные приложения. Можно также поделиться ссылкой из TikTok.",
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
            if (uri != null) {
                OutlinedButton(onClick = { uriText = null }) { Text("Сбросить") }
            }
        }

        if (loading) {
            Text("Читаю информацию о видео…", color = Color(0xFFAAA2BC))
        }

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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        androidx.compose.material3.FilterChip(
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
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("▣", color = Color(0xFFB56CFF), fontSize = 34.sp)
                    Text("Предпросмотр появится здесь",
                        color = Color(0xFFF5F0FF), fontWeight = FontWeight.SemiBold)
                    Text("Поддерживаются локальные видеофайлы, доступные Android.",
                        color = Color(0xFFAAA2BC), fontSize = 12.sp)
                }
            }
        }

        details?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171326))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Информация о файле", color = Color(0xFFF5F0FF),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(info.name, color = Color(0xFFAAA2BC), fontSize = 13.sp)
                    Text(
                        "Разрешение: ${info.width ?: "—"} × ${info.height ?: "—"}",
                        color = Color(0xFFF5F0FF)
                    )
                    Text("Длительность: ${info.durationMs?.let(::formatVideoDuration) ?: "—"}",
                        color = Color(0xFFF5F0FF))
                    Text("Размер: ${info.sizeBytes?.let(::formatVideoSize) ?: "неизвестен"}",
                        color = Color(0xFFF5F0FF))
                }
            }
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Поделиться видео"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Поделиться видео")
            }
        }

        Text(
            "ZYRO работает с файлом, который ты сам выбрал. Видео не загружается автоматически и не отправляется в сеть.",
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
