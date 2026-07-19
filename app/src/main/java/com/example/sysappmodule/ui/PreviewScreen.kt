package com.example.sysappmodule.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sysappmodule.data.AppInfo
import com.example.sysappmodule.data.InstallMode
import com.example.sysappmodule.data.ModuleTemplate
import com.example.sysappmodule.util.toBitmap
import com.example.sysappmodule.vm.TemplateDetailViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    template: ModuleTemplate?,
    allApps: List<AppInfo>,
    isGenerating: Boolean,
    onBack: () -> Unit,
    onGenerate: () -> Unit
) {
    // 等待最后一个 ModuleGenerated 事件用于弹窗（通过其他途径传入更优雅，此处用 ViewModel 事件可省略）
    // 简化：此处只做 UI；事件由外部 ViewModel 在 collect 处传递，这里通过 isGenerating 显示状态
    var generatedInfo by remember { mutableStateOf<GeneratedInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预览确认") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (template != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("返回修改") }
                        Button(
                            onClick = onGenerate,
                            enabled = !isGenerating && template.selectedApps.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("生成中…")
                            } else {
                                Text("生成模块")
                            }
                        }
                    }
                }
            }
        }
    ) { inner ->
        if (template == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(inner),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val selectedApps = template.selectedApps.mapNotNull { sel ->
            val info = allApps.firstOrNull { it.packageName == sel.packageName } ?: return@mapNotNull null
            Triple(info, sel.mode, info.sourceDir)
        }
        val totalApkBytes = selectedApps.sumOf { (_, _, sourceDir) ->
            if (sourceDir == null) 0L
            else runCatching { File(sourceDir).length() }.getOrDefault(0L)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            item {
                MetadataSummaryCard(template)
            }
            item {
                SelectionSummaryCard(
                    selectedCount = selectedApps.size,
                    totalApkBytes = totalApkBytes
                )
            }
            items(selectedApps, key = { it.first.packageName }) { (app, mode, _) ->
                PreviewAppItem(app, mode)
            }
        }
    }

    generatedInfo?.let { info ->
        GeneratedDialog(
            info = info,
            onDismiss = { generatedInfo = null }
        )
    }
}

@Composable
private fun MetadataSummaryCard(template: ModuleTemplate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("模块元数据", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SummaryRow("ID", template.moduleId)
            SummaryRow("名称", template.name.ifBlank { "未命名" })
            SummaryRow("版本", "${template.version} (${template.versionCode})")
            if (template.author.isNotBlank()) SummaryRow("作者", template.author)
            if (template.description.isNotBlank()) {
                SummaryRow("描述", template.description)
            }
        }
    }
}

@Composable
private fun SelectionSummaryCard(selectedCount: Int, totalApkBytes: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("已选应用", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(
                "共 $selectedCount 个，APK 总大小约 ${totalApkBytes / 1024} KB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PreviewAppItem(app: AppInfo, mode: InstallMode) {
    val ctx = LocalContext.current
    val iconPainter: Painter? = remember(app.packageName) {
        try {
            ctx.packageManager.getApplicationIcon(app.packageName)?.toBitmap()?.asImageBitmap()
                ?.let { BitmapPainter(it) }
        } catch (_: Throwable) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (app.label.firstOrNull() ?: '?').toString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            val modeLabel = when (mode) {
                InstallMode.PRIV_APP -> "特权"
                InstallMode.SYSTEM_APP -> "普通"
                InstallMode.INSTALL_EXISTING -> "install-existing"
            }
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private data class GeneratedInfo(
    val displayName: String,
    val packageCount: Int,
    val totalBytes: Long,
    val savedToDownloads: Boolean
)

@Composable
private fun GeneratedDialog(info: GeneratedInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模块已生成") },
        text = {
            Text(
                "文件名: ${info.displayName}\n" +
                    "包含应用: ${info.packageCount} 个\n" +
                    "APK 总大小: ${info.totalBytes / 1024} KB\n" +
                    (if (info.savedToDownloads) "已保存到: Download/MagicModules/"
                    else "未保存到 Download") + "\n\n" +
                    "请在 Magisk/KSU 管理器中导入此 zip 安装，重启后生效。"
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
