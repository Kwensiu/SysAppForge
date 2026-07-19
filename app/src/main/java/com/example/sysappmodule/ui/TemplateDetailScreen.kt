package com.example.sysappmodule.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.sysappmodule.data.SelectedPackage
import com.example.sysappmodule.util.toBitmap
import com.example.sysappmodule.vm.AppFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    template: ModuleTemplate?,
    allApps: List<AppInfo>,
    searchQuery: String,
    filter: AppFilter,
    showSystem: Boolean,
    metadataExpanded: Boolean,
    isGenerating: Boolean,
    onSearch: (String) -> Unit,
    onFilter: (AppFilter) -> Unit,
    onToggleShowSystem: () -> Unit,
    onToggleMetadataExpanded: () -> Unit,
    onUpdateTemplate: ((ModuleTemplate) -> ModuleTemplate) -> Unit,
    onToggleApp: (AppInfo) -> Unit,
    onSetInstallMode: (String, InstallMode) -> Unit,
    onDeleteTemplate: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        template?.name?.ifBlank { "未命名模板" } ?: "加载中…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { pendingDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除模板",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            if (template != null) {
                BottomNextBar(
                    selectedCount = template.selectedApps.size,
                    isGenerating = isGenerating,
                    onNext = onNext
                )
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

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)) {

            MetadataPanel(
                template = template,
                expanded = metadataExpanded,
                onToggle = onToggleMetadataExpanded,
                onUpdate = onUpdateTemplate
            )

            AppSelector(
                template = template,
                allApps = allApps,
                searchQuery = searchQuery,
                filter = filter,
                showSystem = showSystem,
                onSearch = onSearch,
                onFilter = onFilter,
                onToggleShowSystem = onToggleShowSystem,
                onToggleApp = onToggleApp,
                onSetInstallMode = onSetInstallMode
            )
        }
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text("删除模板") },
            text = { Text("确定要删除「${template?.name}」吗？此操作不可恢复。") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pendingDelete = false
                    onDeleteTemplate()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MetadataPanel(
    template: ModuleTemplate,
    expanded: Boolean,
    onToggle: () -> Unit,
    onUpdate: ((ModuleTemplate) -> ModuleTemplate) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "模块元数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    MetadataField("模块 ID", template.moduleId) { v ->
                        onUpdate { it.copy(moduleId = v) }
                    }
                    MetadataField("模块名称", template.name) { v ->
                        onUpdate { it.copy(name = v) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        MetadataField("版本", template.version, modifier = Modifier.weight(1f)) { v ->
                            onUpdate { it.copy(version = v) }
                        }
                        MetadataField("versionCode", template.versionCode.toString(),
                            modifier = Modifier.weight(1f)) { v ->
                            val code = v.toLongOrNull() ?: 1L
                            onUpdate { it.copy(versionCode = code) }
                        }
                    }
                    MetadataField("作者", template.author) { v ->
                        onUpdate { it.copy(author = v) }
                    }
                    MetadataField("描述", template.description, singleLine = false, minLines = 2) { v ->
                        onUpdate { it.copy(description = v) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
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
private fun AppSelector(
    template: ModuleTemplate,
    allApps: List<AppInfo>,
    searchQuery: String,
    filter: AppFilter,
    showSystem: Boolean,
    onSearch: (String) -> Unit,
    onFilter: (AppFilter) -> Unit,
    onToggleShowSystem: () -> Unit,
    onToggleApp: (AppInfo) -> Unit,
    onSetInstallMode: (String, InstallMode) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp)) {

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            placeholder = { Text("按应用名或包名搜索") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(filter == AppFilter.ALL, { onFilter(AppFilter.ALL) }, { Text("全部") })
            FilterChip(filter == AppFilter.USER, { onFilter(AppFilter.USER) }, { Text("用户") })
            FilterChip(filter == AppFilter.SYSTEM, { onFilter(AppFilter.SYSTEM) }, { Text("系统") })
            FilterChip(filter == AppFilter.SELECTED, { onFilter(AppFilter.SELECTED) }, { Text("已选") })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "已选 ${template.selectedApps.size} 个",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("显示系统", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = showSystem, onCheckedChange = { onToggleShowSystem() })
        }

        val q = searchQuery.trim().lowercase()
        val filtered = allApps.filter { app ->
            if (!showSystem && app.isSystem && !app.isUpdatedSystem) return@filter false
            when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystem || app.isUpdatedSystem
                AppFilter.SYSTEM -> app.isSystem && !app.isUpdatedSystem
                AppFilter.SELECTED -> template.selectedApps.any { it.packageName == app.packageName }
            }
        }.let { list ->
            if (q.isEmpty()) list
            else list.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (allApps.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filtered.isEmpty()) {
                Text(
                    "未找到应用",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val selected = template.selectedApps.firstOrNull {
                            it.packageName == app.packageName
                        }
                        AppSelectionItem(
                            app = app,
                            isSelected = selected != null,
                            selectedMode = selected?.mode,
                            onToggle = { onToggleApp(app) },
                            onModeChange = { mode -> onSetInstallMode(app.packageName, mode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    selectedMode: InstallMode?,
    onToggle: () -> Unit,
    onModeChange: (InstallMode) -> Unit
) {
    val ctx = LocalContext.current
    val iconPainter: Painter? = remember(app.packageName) {
        try {
            ctx.packageManager.getApplicationIcon(app.packageName)?.toBitmap()?.asImageBitmap()
                ?.let { BitmapPainter(it) }
        } catch (_: Throwable) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconPainter != null) {
                    androidx.compose.foundation.Image(
                        painter = iconPainter,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (app.label.firstOrNull() ?: '?').toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isSystem) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (app.isUpdatedSystem) "SYS*" else "SYS",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (isSelected) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            AnimatedVisibility(
                visible = isSelected && selectedMode != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 60.dp, end = 12.dp, bottom = 8.dp)) {
                    InstallModeOption(InstallMode.PRIV_APP, selectedMode) { onModeChange(it) }
                    InstallModeOption(InstallMode.SYSTEM_APP, selectedMode) { onModeChange(it) }
                    InstallModeOption(InstallMode.INSTALL_EXISTING, selectedMode) { onModeChange(it) }
                }
            }
        }
    }
}

@Composable
private fun InstallModeOption(
    mode: InstallMode,
    current: InstallMode?,
    onSelect: (InstallMode) -> Unit
) {
    val (label, desc) = when (mode) {
        InstallMode.PRIV_APP -> "特权系统应用" to "放到 /system/priv-app/，可获得特权权限"
        InstallMode.SYSTEM_APP -> "普通系统应用" to "放到 /system/app/，仅获得系统应用身份"
        InstallMode.INSTALL_EXISTING -> "install-existing" to "调用 pm install-existing，需应用已存在"
    }
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
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BottomNextBar(
    selectedCount: Int,
    isGenerating: Boolean,
    onNext: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Button(
                onClick = onNext,
                enabled = selectedCount > 0 && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
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
                    Text(if (selectedCount > 0) "下一步（$selectedCount 个应用）" else "请先选择应用")
                }
            }
        }
    }
}


