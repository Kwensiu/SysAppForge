package com.example.sysappmodule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.sysappmodule.BuildConfig
import com.example.sysappmodule.data.ThemeMode
import com.example.sysappmodule.ui.components.SettingItem
import com.example.sysappmodule.ui.components.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsGroup(title = "外观") {
                item {
                    ThemeOptionItem(
                        mode = ThemeMode.LIGHT,
                        current = themeMode,
                        label = "明亮",
                        desc = "始终使用明亮主题",
                        icon = Icons.Default.LightMode,
                        onSelect = onThemeChange
                    )
                }
                item {
                    ThemeOptionItem(
                        mode = ThemeMode.DARK,
                        current = themeMode,
                        label = "暗黑",
                        desc = "始终使用暗黑主题",
                        icon = Icons.Default.DarkMode,
                        onSelect = onThemeChange
                    )
                }
                item {
                    ThemeOptionItem(
                        mode = ThemeMode.SYSTEM,
                        current = themeMode,
                        label = "跟随系统",
                        desc = "根据系统设置自动切换",
                        icon = Icons.Default.BrightnessAuto,
                        onSelect = onThemeChange
                    )
                }
            }

            SettingsGroup(title = "其他") {
                item {
                    SettingItem(
                        title = "更新",
                        description = "检查新版本（敬请期待）",
                        icon = Icons.Default.SystemUpdate
                    )
                }
                item {
                    SettingItem(
                        title = "关于",
                        description = "SysAppForge v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        icon = Icons.Default.Info
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionItem(
    mode: ThemeMode,
    current: ThemeMode,
    label: String,
    desc: String,
    icon: ImageVector,
    onSelect: (ThemeMode) -> Unit
) {
    val selected = mode == current
    SettingItem(
        title = label,
        description = desc,
        icon = icon,
        onClick = { onSelect(mode) },
        selected = selected,
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = { onSelect(mode) }
            )
        }
    )
}
