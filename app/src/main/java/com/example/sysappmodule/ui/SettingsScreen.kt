package com.example.sysappmodule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.sysappmodule.BuildConfig
import com.example.sysappmodule.data.ThemeMode
import com.example.sysappmodule.ui.components.LocalItemCorners
import com.example.sysappmodule.ui.components.SettingItem
import com.example.sysappmodule.ui.components.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
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
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            SettingsGroup(title = "外观") {
                item {
                    ThemeSegmentedItem(
                        selected = themeMode,
                        onSelect = onThemeChange
                    )
                }
                item {
                    SettingItem(
                        title = "动态颜色",
                        description = "根据系统壁纸提取配色（Android 12+）",
                        icon = Icons.Default.Palette,
                        onClick = { onDynamicColorChange(!dynamicColor) },
                        trailingContent = {
                            Switch(
                                checked = dynamicColor,
                                onCheckedChange = onDynamicColorChange
                            )
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSegmentedItem(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val options = remember {
        listOf(
            ThemeOption(ThemeMode.LIGHT, Icons.Default.LightMode, "明亮"),
            ThemeOption(ThemeMode.DARK, Icons.Default.DarkMode, "暗黑"),
            ThemeOption(ThemeMode.SYSTEM, Icons.Outlined.BrightnessAuto, "跟随系统")
        )
    }

    val corners = LocalItemCorners.current
    val shape = RoundedCornerShape(
        topStart = corners.topStart,
        topEnd = corners.topEnd,
        bottomStart = corners.bottomStart,
        bottomEnd = corners.bottomEnd
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "主题",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "选择界面显示模式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = selected == option.mode,
                        onClick = { onSelect(option.mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        icon = {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(option.label)
                    }
                }
            }
        }
    }
}

private data class ThemeOption(
    val mode: ThemeMode,
    val icon: ImageVector,
    val label: String
)
