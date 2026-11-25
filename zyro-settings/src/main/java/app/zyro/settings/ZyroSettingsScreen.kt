package app.zyro.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Bg = Color(0xFF090711)
private val Panel = Color(0xFF171326)
private val Accent = Color(0xFFB56CFF)
private val MainText = Color(0xFFF5F0FF)
private val Muted = Color(0xFFAAA2BC)

@Composable
fun ZyroSettingsScreen(modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repo = remember(context) { ZyroSettingsRepository(context) }
    val state by repo.appearance.collectAsState(initial = ZyroAppearanceSettings())
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(
            modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("ZYRO", color = Accent, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("Внешний вид", color = MainText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Настрой интерфейс под себя", color = Muted, fontSize = 14.sp)
                }
                if (onClose != null) TextButton(onClick = onClose) {
                    Text("Закрыть", color = Muted)
                }
            }
            PanelCard {
                ToggleRow("Liquid Glass", "Полупрозрачный стиль поверхностей ZYRO",
                    state.liquidGlassEnabled) { scope.launch { repo.setLiquidGlassEnabled(it) } }
                ToggleRow("Размытие", "Blur, если его поддерживает хост",
                    state.blurEnabled, state.liquidGlassEnabled) {
                    scope.launch { repo.setBlurEnabled(it) }
                }
                ToggleRow("Прозрачность", "Полупрозрачные панели",
                    state.transparencyEnabled, state.liquidGlassEnabled) {
                    scope.launch { repo.setTransparencyEnabled(it) }
                }
            }
            PanelCard {
                Text("Интенсивность · ${(state.surfaceIntensity * 100).toInt()}%",
                    color = MainText, fontWeight = FontWeight.SemiBold)
                Slider(state.surfaceIntensity,
                    { scope.launch { repo.setSurfaceIntensity(it) } }, valueRange = 0f..1f)
                Text("Радиус углов · ${state.cornerRadius.toInt()} dp",
                    color = MainText, fontWeight = FontWeight.SemiBold)
                Slider(state.cornerRadius,
                    { scope.launch { repo.setCornerRadius(it) } }, valueRange = 8f..36f)
                Text("Сила анимаций · ${(state.animationStrength * 100).toInt()}%",
                    color = MainText, fontWeight = FontWeight.SemiBold)
                Slider(state.animationStrength,
                    { scope.launch { repo.setAnimationStrength(it) } },
                    valueRange = 0f..1f, enabled = !state.reduceMotion)
            }
            PanelCard {
                Text("Тема", color = MainText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeChoice("Система", state.themeMode == ZyroThemeMode.SYSTEM) {
                        scope.launch { repo.setThemeMode(ZyroThemeMode.SYSTEM) }
                    }
                    ThemeChoice("Тёмная", state.themeMode == ZyroThemeMode.DARK) {
                        scope.launch { repo.setThemeMode(ZyroThemeMode.DARK) }
                    }
                    ThemeChoice("Светлая", state.themeMode == ZyroThemeMode.LIGHT) {
                        scope.launch { repo.setThemeMode(ZyroThemeMode.LIGHT) }
                    }
                }
            }
            PanelCard {
                Text("Доступность", color = MainText, fontWeight = FontWeight.SemiBold)
                ToggleRow("Уменьшить движение", "Снизить интенсивность анимаций",
                    state.reduceMotion) { scope.launch { repo.setReduceMotion(it) } }
            }
            PanelCard {
                Text("Цвет акцента", color = MainText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("violet", "pink", "cyan").forEach { preset ->
                        ThemeChoice(when (preset) {
                            "pink" -> "Розовый"
                            "cyan" -> "Бирюзовый"
                            else -> "Фиолетовый"
                        }, state.accentPreset == preset) {
                            scope.launch { repo.setAccentPreset(preset) }
                        }
                    }
                }
            }
            Button(onClick = { scope.launch { repo.resetAppearance() } },
                modifier = Modifier.fillMaxWidth()) {
                Text("Сбросить настройки внешнего вида")
            }
            Text("Настройки сохраняются локально. Модуль пока не подключён к TikTok.",
                color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content)
    }
}

@Composable
private fun ToggleRow(
    title: String, description: String, checked: Boolean, enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MainText, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 12.sp)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeChoice(title: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(if (selected) "✓ $title" else title,
            color = if (selected) Accent else Muted)
    }
}
