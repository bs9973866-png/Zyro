package app.zyro.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import app.zyro.camera.ZyroCameraDiagnosticsScreen
import app.zyro.camera.ZyroCameraRecorder
import app.zyro.camera.ZyroVideoQuality
import app.zyro.settings.ZyroSettingsScreen

private enum class Destination(val title: String, val symbol: String) {
    HOME("Главная", "⌂"),
    SETTINGS("Вид", "◈"),
    DIAGNOSTICS("Камера", "◎"),
    RECORDER("Запись", "●")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZyroHostApp() }
    }
}

@Composable
private fun ZyroHostApp() {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    val selected = Destination.valueOf(destination)

    MaterialTheme {
        Scaffold(
            containerColor = Color(0xFF090711),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF171326)) {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = selected == item,
                            onClick = { destination = item.name },
                            icon = { Text(item.symbol, fontSize = 18.sp) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selected) {
                    Destination.HOME -> HomeScreen(
                        onOpen = { destination = it.name }
                    )
                    Destination.SETTINGS -> ZyroSettingsScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                    Destination.DIAGNOSTICS -> ZyroCameraDiagnosticsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onClose = { destination = Destination.HOME.name }
                    )
                    Destination.RECORDER -> RecorderScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpen: (Destination) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF090711))
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("ZYRO", color = Color(0xFFB56CFF), fontSize = 14.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("Твой Android.
Твои настройки.", color = Color(0xFFF5F0FF),
            fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        Text(
            "Тестовая оболочка ZYRO: настройки интерфейса, информация о камерах и ручная видеозапись.",
            color = Color(0xFFAAA2BC), fontSize = 14.sp
        )
        HomeCard(
            title = "Внешний вид",
            description = "Темы, Liquid Glass и параметры интерфейса",
            action = "Открыть настройки"
        ) { onOpen(Destination.SETTINGS) }
        HomeCard(
            title = "Диагностика камеры",
            description = "Размеры видео, доступные FPS, HDR и стабилизация",
            action = "Проверить устройство"
        ) { onOpen(Destination.DIAGNOSTICS) }
        HomeCard(
            title = "Камера и запись",
            description = "Предпросмотр и ручная запись видео без звука",
            action = "Открыть камеру"
        ) { onOpen(Destination.RECORDER) }
        Text(
            "Это самостоятельное demo/host-приложение. Оно не изменяет TikTok и не подключается к его аккаунтам.",
            color = Color(0xFFAAA2BC), fontSize = 12.sp
        )
    }
}

@Composable
private fun HomeCard(
    title: String,
    description: String,
    action: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171326))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(title, color = Color(0xFFF5F0FF),
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(description, color = Color(0xFFAAA2BC), fontSize = 13.sp)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(action)
            }
        }
    }
}

@Composable
private fun RecorderScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF090711))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentPadding()
    ) {
        ZyroCameraRecorder(
            quality = ZyroVideoQuality.FULL_HD,
            onVideoSaved = { uri ->
                Toast.makeText(context, "Видео сохранено: $uri", Toast.LENGTH_LONG).show()
            },
            onError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        )
    }
}

private fun contentPadding(): Arrangement.Vertical = Arrangement.spacedBy(8.dp)
