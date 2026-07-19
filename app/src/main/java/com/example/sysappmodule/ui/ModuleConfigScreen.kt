package com.example.sysappmodule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sysappmodule.R
import com.example.sysappmodule.data.InstallMode
import com.example.sysappmodule.data.ModuleConfig
import com.example.sysappmodule.data.SelectedApp
import com.example.sysappmodule.util.toBitmap
import com.example.sysappmodule.vm.MainViewModel

@Composable
fun ModuleConfigScreen(
    config: ModuleConfig,
    isGenerating: Boolean,
    onConfigChange: ((ModuleConfig) -> ModuleConfig) -> Unit,
    onModeChange: (String, InstallMode) -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "模块元数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                MetadataField(
                    label = stringResource(R.string.label_module_id),
                    value = config.id,
                    onValueChange = { v -> onConfigChange { it.copy(id = v) } }
                )
                MetadataField(
                    label = stringResource(R.string.label_module_name),
                    value = config.name,
                    onValueChange = { v -> onConfigChange { it.copy(name = v) } }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataField(
                        label = stringResource(R.string.label_module_version),
                        value = config.version,
                        onValueChange = { v -> onConfigChange { it.copy(version = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    MetadataField(
                        label = "versionCode",
                        value = config.versionCode.toString(),
                        onValueChange = { v ->
                            val code = v.toLongOrNull() ?: 1L
                            onConfigChange { it.copy(versionCode = code) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                MetadataField(
                    label = stringResource(R.string.label_module_author),
                    value = config.author,
                    onValueChange = { v -> onConfigChange { it.copy(author = v) } }
                )
                MetadataField(
                    label = stringResource(R.string.label_module_description),
                    value = config.description,
                    onValueChange = { v -> onConfigChange { it.copy(description = v) } },
                    singleLine = false,
                    minLines = 2
                )
            }
        }

        Text(
            text = "已选应用 (${config.selectedApps.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
            fontWeight = FontWeight.SemiBold
        )

        if (config.selectedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_selection),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(config.selectedApps, key = { it.app.packageName }) { sel ->
                    SelectedAppItem(
                        selected = sel,
                        onModeChange = { mode -> onModeChange(sel.app.packageName, mode) }
                    )
                }
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = !isGenerating && config.selectedApps.isNotEmpty(),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.msg_generating))
            } else {
                Text(
                    text = stringResource(R.string.action_generate),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun MetadataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SelectedAppItem(
    selected: SelectedApp,
    onModeChange: (InstallMode) -> Unit
) {
    val ctx = LocalContext.current
    val iconPainter: Painter? = remember(selected.app.packageName) {
        try {
            ctx.packageManager.getApplicationIcon(selected.app.packageName)?.toBitmap()?.asImageBitmap()
                ?.let { BitmapPainter(it) }
        } catch (_: Throwable) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconPainter != null) {
                    androidx.compose.foundation.Image(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (selected.app.label.firstOrNull() ?: '?').toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selected.app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.label_install_mode),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            InstallModeOption(
                mode = InstallMode.PRIV_APP,
                current = selected.mode,
                label = stringResource(R.string.mode_priv_app),
                desc = stringResource(R.string.desc_priv_app),
                onSelect = onModeChange
            )
            InstallModeOption(
                mode = InstallMode.SYSTEM_APP,
                current = selected.mode,
                label = stringResource(R.string.mode_system_app),
                desc = stringResource(R.string.desc_system_app),
                onSelect = onModeChange
            )
            InstallModeOption(
                mode = InstallMode.INSTALL_EXISTING,
                current = selected.mode,
                label = stringResource(R.string.mode_install_existing),
                desc = stringResource(R.string.desc_install_existing),
                onSelect = onModeChange
            )
        }
    }
}

@Composable
private fun InstallModeOption(
    mode: InstallMode,
    current: InstallMode,
    label: String,
    desc: String,
    onSelect: (InstallMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = mode == current,
            onClick = { onSelect(mode) }
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
