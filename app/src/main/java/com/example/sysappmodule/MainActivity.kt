package com.example.sysappmodule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sysappmodule.ui.AppListScreen
import com.example.sysappmodule.ui.ModuleConfigScreen
import com.example.sysappmodule.ui.theme.SysAppModuleTheme
import com.example.sysappmodule.vm.MainViewModel
import com.example.sysappmodule.vm.MainViewModel.Tab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SysAppModuleTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(vm: MainViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val config by vm.moduleConfig.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    var lastModuleEvent by remember { mutableStateOf<MainViewModel.UiEvent.ModuleGenerated?>(null) }

    LaunchedEffect(Unit) { vm.loadApps() }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is MainViewModel.UiEvent.Info -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MainViewModel.UiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MainViewModel.UiEvent.ModuleGenerated -> {
                    lastModuleEvent = event
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.currentTab == Tab.Apps) stringResource(R.string.title_app_list)
                        else stringResource(R.string.title_module_config)
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.currentTab == Tab.Apps,
                    onClick = { vm.switchTab(Tab.Apps) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_apps)) }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == Tab.Config,
                    onClick = { vm.switchTab(Tab.Config) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_config)) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when (uiState.currentTab) {
                Tab.Apps -> AppListScreen(
                    state = uiState,
                    onSearch = vm::updateSearchQuery,
                    onFilter = vm::updateFilter,
                    onToggleShowSystem = vm::toggleShowSystem,
                    onToggleSelect = vm::toggleSelection,
                    onClearSelection = vm::clearSelection
                )
                Tab.Config -> ModuleConfigScreen(
                    config = config,
                    isGenerating = uiState.isGenerating,
                    onConfigChange = vm::updateConfig,
                    onModeChange = vm::setInstallMode,
                    onGenerate = vm::generateModule
                )
            }
        }
    }

    val e = lastModuleEvent
    if (e != null) {
        val sizeKb = e.totalBytes / 1024
        AlertDialog(
            onDismissRequest = { lastModuleEvent = null },
            title = { Text("模块已生成") },
            text = {
                Text(
                    "文件名: ${e.displayName}\n" +
                        "包含应用: ${e.packageCount} 个\n" +
                        "APK 总大小: ${sizeKb} KB\n" +
                        (if (e.downloadsUri != null) "已保存到: Download/MagicModules/\n" else "未保存到 Download（请使用分享）\n") +
                        "\n请在 Magisk/KSU 管理器中导入此 zip 安装，重启后生效。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.buildOpenInstallIntent()?.let { ctx.startActivity(it) }
                    lastModuleEvent = null
                }) { Text(stringResource(R.string.action_open_install)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.buildShareIntent()?.let { ctx.startActivity(Intent.createChooser(it, "分享模块")) }
                    lastModuleEvent = null
                }) {
                    Text(stringResource(R.string.action_share))
                }
            }
        )
    }
}
