package app.zyro.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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

private data class SettingsPalette(
    val background: Color,
    val panel: Color,
    val accent: Color,
    val primaryText: Color,
    val secondaryText: Color
)

private fun settingsPalette(dark: Boolean, accentPreset: String): SettingsPalette {
    val accent = when (accentPreset) {
        "pink" -> if (dark) Color(0xFFFF5CAC) else Color(0xFFD63384)
        "cyan" -> if (dark) Color(0xFF54DCE5) else Color(0xFF008C95)
        else -> if (dark) Color(0xFFB56CFF) else Color(0xFF7540C8)
    }
    return if (dark) {
        SettingsPalette(
            background = Color(0xFF090711),
            panel = Color(0xFF171326),
            accent = accent,
            primaryText = Color(0xFFF5F0FF),
            secondaryText = Color(0xFFAAA2BC)
        )
    } else {
        SettingsPalette(
            background = Color(0xFFF7F4FB),
            panel = Color(0xFFFFFFFF),
            accent = accent,
            primaryText = Color(0xFF201A2A),
            secondaryText = Color(0xFF625B6F)
        )
    }
}

@Composable
fun ZyroSettingsScreen(modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repo = remember(context) { ZyroSettingsRepository(context) }
    val state by repo.appearance.collectAsState(initial = ZyroAppearanceSettings())
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val useDark = when (state.themeMode) {
        ZyroThemeMode.SYSTEM -> systemDark
        ZyroThemeMode.DARK -> true
        ZyroThemeMode.LIGHT -> false
    }
    val palette = remember(useDark, state.accentPreset) {
        settingsPalette(useDark, state.accentPreset)
    }
    val colors = if (useDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color(0xFF160D22),
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.panel,
            onSurface = palette.primaryText,
            secondary = palette.accent
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.panel,
            onSurface = palette.primaryText,
            secondary = palette.accent
        )
    }

    MaterialTheme(colorScheme = colors) {
        Column(
            modifier.fillMaxSize()
                .background(palette.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ZYRO", color = palette.accent, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("Внешний вид", color = palette.primaryText,
                        fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Настрой интерфейс под себя",
                        color = palette.secondaryText, fontSize = 14.sp)
                }
                if (onClose != null) TextButton(onClick = onClose) {
                    Text("Закрыть", color = palette.secondaryText)
                }
            }

            PanelCard(palette) {
                ToggleRow(
                    palette,
                    "Liquid Glass",
                    "Полупрозрачный стиль поверхностей ZYRO",
                    state.liquidGlassEnabled
                ) { scope.launch { repo.setLiquidGlassEnabled(it) } }
                ToggleRow(
                    palette,
                    "Размытие",
                    "Blur, если его поддерживает хост",
                    state.blurEnabled,
                    state.liquidGlassEnabled
                ) { scope.launch { repo.setBlurEnabled(it) } }
                ToggleRow(
                    palette,
                    "Прозрачность",
                    "Полупрозрачные панели",
                    state.transparencyEnabled,
                    state.liquidGlassEnabled
                ) { scope.launch { repo.setTransparencyEnabled(it) } }
            }

            PanelCard(palette) {
                Text("Интенсивность · ${(state.surfaceIntensity * 100).toInt()}%",
                    color = palette.primaryText, fontWeight = FontWeight.SemiBold)
                Slider(state.surfaceIntensity,
                    { scope.launch { repo.setSurfaceIntensity(it) } }, valueRange = 0f..1f)
                Text("Радиус углов · ${state.cornerRadius.toInt()} dp",
                    color = palette.primaryText, fontWeight = FontWeight.SemiBold)
                Slider(state.cornerRadius,
                    { scope.launch { repo.setCornerRadius(it) } }, valueRange = 8f..36f)
                Text("Сила анимаций · ${(state.animationStrength * 100).toInt()}%",
                    color = palette.primaryText, fontWeight = FontWeight.SemiBold)
                Slider(state.animationStrength,
                    { scope.launch { repo.setAnimationStrength(it) } },
                    valueRange = 0f..1f, enabled = !state.reduceMotion)
            }

            PanelCard(palette) {
                Text("Тема", color = palette.primaryText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeChoice(palette, "Система", state.themeMode == ZyroThemeMode.SYSTEM) {
                        scope.launch { repo.setThemeMode(ZyroThemeMode.SYSTEM) }
                    }
                    ThemeChoice(palette, "Тёмная", state.themeMode == ZyroThemeMode.DARK) {
                        scope.launch { repo.setThemeMode(ZyroThemeMode.DARK) }
                    }
                    ThemeChoice(palette, "Светлая", state.themeMode == ZyroThemeMode.LIGHT) {
                        scope.launch { repo.setThemeMode(ZyroThemeMode.LIGHT) }
                    }
                }
            }

            PanelCard(palette) {
                Text("Доступность", color = palette.primaryText, fontWeight = FontWeight.SemiBold)
                ToggleRow(
                    palette,
                    "Уменьшить движение",
                    "Снизить интенсивность анимаций",
                    state.reduceMotion
                ) { scope.launch { repo.setReduceMotion(it) } }
            }

            PanelCard(palette) {
                Text("Цвет акцента", color = palette.primaryText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("violet", "pink", "cyan").forEach { preset ->
                        ThemeChoice(
                            palette,
                            when (preset) {
                                "pink" -> "Розовый"
                                "cyan" -> "Бирюзовый"
                                else -> "Фиолетовый"
                            },
                            state.accentPreset == preset
                        ) { scope.launch { repo.setAccentPreset(preset) } }
                    }
                }
            }

            Button(onClick = { scope.launch { repo.resetAppearance() } },
                modifier = Modifier.fillMaxWidth()) {
                Text("Сбросить настройки внешнего вида")
            }
            Text(
                "Настройки сохраняются локально. Модуль пока не подключён к TikTok.",
                color = palette.secondaryText,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PanelCard(
    palette: SettingsPalette,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ToggleRow(
    palette: SettingsPalette,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.primaryText, fontWeight = FontWeight.SemiBold)
            Text(description, color = palette.secondaryText, fontSize = 12.sp)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeChoice(
    palette: SettingsPalette,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            if (selected) "✓ $title" else title,
            color = if (selected) palette.accent else palette.secondaryText
        )
    }
}
