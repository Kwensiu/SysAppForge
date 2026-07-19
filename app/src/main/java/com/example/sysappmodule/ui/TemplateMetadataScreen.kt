package com.example.sysappmodule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.sysappmodule.data.ModuleTemplate
import com.example.sysappmodule.module.ModuleBuilder
import com.example.sysappmodule.vm.isMetadataValid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateMetadataScreen(
    draft: ModuleTemplate?,
    isCreating: Boolean,
    onUpdate: ((ModuleTemplate) -> ModuleTemplate) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var showErrors by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreating) "新建模板" else "编辑模板") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (draft != null) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = {
                            showErrors = true
                            if (draft.isMetadataValid()) onSave()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (draft == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("模板信息")
            }
            item {
                MetadataTextField(
                    value = draft.templateName,
                    onValueChange = { value -> onUpdate { it.copy(templateName = value) } },
                    label = "模板名称",
                    isError = showErrors && draft.templateName.isBlank(),
                    supportingText = if (showErrors && draft.templateName.isBlank()) "请输入模板名称" else null
                )
            }
            item { HorizontalDivider() }
            item {
                SectionHeader("模块元数据")
            }
            item {
                MetadataTextField(
                    value = draft.moduleId,
                    onValueChange = { value -> onUpdate { it.copy(moduleId = value) } },
                    label = "模块 ID",
                    isError = showErrors && !ModuleBuilder.MODULE_ID_REGEX.matches(draft.moduleId.trim()),
                    supportingText = if (
                        showErrors && !ModuleBuilder.MODULE_ID_REGEX.matches(draft.moduleId.trim())
                    ) "只能包含小写字母、数字和下划线，且必须以字母开头" else
                        "创建时自动生成，后续保持不变更利于模块升级"
                )
            }
            item {
                MetadataTextField(
                    value = draft.name,
                    onValueChange = { value -> onUpdate { it.copy(name = value) } },
                    label = "模块名称",
                    isError = showErrors && draft.name.isBlank(),
                    supportingText = if (showErrors && draft.name.isBlank()) "请输入模块名称" else null
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetadataTextField(
                        value = draft.version,
                        onValueChange = { value -> onUpdate { it.copy(version = value) } },
                        label = "版本名称",
                        modifier = Modifier.weight(1f),
                        isError = showErrors && draft.version.isBlank(),
                        supportingText = if (showErrors && draft.version.isBlank()) "不能为空" else null
                    )
                    MetadataTextField(
                        value = draft.versionCode.takeIf { it > 0 }?.toString().orEmpty(),
                        onValueChange = { value ->
                            if (value.all(Char::isDigit)) {
                                onUpdate { it.copy(versionCode = value.toLongOrNull() ?: 0L) }
                            }
                        },
                        label = "版本号",
                        modifier = Modifier.weight(1f),
                        isError = showErrors && draft.versionCode <= 0,
                        supportingText = if (showErrors && draft.versionCode <= 0) "必须大于 0" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            item {
                MetadataTextField(
                    value = draft.author,
                    onValueChange = { value -> onUpdate { it.copy(author = value) } },
                    label = "作者"
                )
            }
            item {
                MetadataTextField(
                    value = draft.description,
                    onValueChange = { value -> onUpdate { it.copy(description = value) } },
                    label = "描述",
                    singleLine = false,
                    minLines = 3
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun MetadataTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions
    )
}
