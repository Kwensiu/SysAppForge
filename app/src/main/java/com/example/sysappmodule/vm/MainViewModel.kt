package com.example.sysappmodule.vm

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sysappmodule.data.AppInfo
import com.example.sysappmodule.data.InstallMode
import com.example.sysappmodule.data.ModuleConfig
import com.example.sysappmodule.data.PackageLoader
import com.example.sysappmodule.data.SelectedApp
import com.example.sysappmodule.module.ApkExtractor
import com.example.sysappmodule.module.ModuleBuilder
import com.example.sysappmodule.util.FileSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val packageLoader = PackageLoader(app)
    private val moduleBuilder = ModuleBuilder(ApkExtractor())
    private val fileSaver = FileSaver(app)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _moduleConfig = MutableStateFlow(ModuleConfig())
    val moduleConfig: StateFlow<ModuleConfig> = _moduleConfig.asStateFlow()

    private val _events = MutableStateFlow<UiEvent?>(null)
    val events: StateFlow<UiEvent?> = _events.asStateFlow()

    fun consumeEvent() { _events.value = null }

    fun switchTab(tab: Tab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    enum class Tab { Apps, Config }

    /** 加载已安装应用列表 */
    fun loadApps() {
        if (_uiState.value.isLoaded || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val apps = packageLoader.loadInstalledApps(includeSystem = true)
                _uiState.update {
                    it.copy(
                        allApps = apps,
                        filteredApps = apps,
                        isLoading = false,
                        isLoaded = true
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = t.message ?: "未知错误") }
            }
        }
    }

    fun updateSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        applyFilter()
    }

    fun updateFilter(filter: AppFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilter()
    }

    fun toggleShowSystem(show: Boolean) {
        _uiState.update { it.copy(showSystem = show) }
        applyFilter()
    }

    private fun applyFilter() {
        val state = _uiState.value
        val q = state.searchQuery.trim().lowercase()
        val filtered = state.allApps.filter { app ->
            // 显示系统应用开关
            if (!state.showSystem && app.isSystem && !app.isUpdatedSystem) {
                return@filter false
            }
            // 筛选
            when (state.filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystem || app.isUpdatedSystem
                AppFilter.SYSTEM -> app.isSystem && !app.isUpdatedSystem
                AppFilter.SELECTED -> state.selectedPackages.contains(app.packageName)
            }
        }.let { list ->
            if (q.isEmpty()) list
            else list.filter {
                it.label.lowercase().contains(q) ||
                    it.packageName.lowercase().contains(q)
            }
        }
        _uiState.update { it.copy(filteredApps = filtered) }
    }

    fun toggleSelection(app: AppInfo) {
        val state = _uiState.value
        val newSet = state.selectedPackages.toMutableSet()
        if (!newSet.add(app.packageName)) {
            newSet.remove(app.packageName)
        }
        _uiState.update { it.copy(selectedPackages = newSet) }

        // 同步到 moduleConfig.selectedApps
        _moduleConfig.update { config ->
            val current = config.selectedApps.toMutableList()
            val idx = current.indexOfFirst { it.app.packageName == app.packageName }
            if (idx >= 0) {
                current.removeAt(idx)
            } else {
                current.add(SelectedApp(app, InstallMode.PRIV_APP))
            }
            config.copy(selectedApps = current)
        }
        applyFilter()
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPackages = emptySet()) }
        _moduleConfig.update { it.copy(selectedApps = emptyList()) }
        applyFilter()
    }

    fun setInstallMode(packageName: String, mode: InstallMode) {
        _moduleConfig.update { config ->
            val list = config.selectedApps.map { sel ->
                if (sel.app.packageName == packageName) sel.copy(mode = mode) else sel
            }
            config.copy(selectedApps = list)
        }
    }

    fun updateConfig(transform: (ModuleConfig) -> ModuleConfig) {
        _moduleConfig.update(transform)
    }

    /** 生成模块 zip */
    fun generateModule() {
        val config = _moduleConfig.value
        if (config.selectedApps.isEmpty()) {
            _events.value = UiEvent.Error("请至少选择一个应用")
            return
        }
        if (!ModuleBuilder.MODULE_ID_REGEX.matches(config.id)) {
            _events.value = UiEvent.Error("模块 ID 非法，只能包含小写字母、数字、下划线，且以字母开头")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            _events.value = UiEvent.Info("正在生成模块…")
            try {
                val ctx = getApplication<Application>()
                val workDir = File(ctx.cacheDir, "module_work_${System.currentTimeMillis()}")
                val tempZip = File(ctx.cacheDir, "module_temp_${config.id}_${System.currentTimeMillis()}.zip")
                val result = moduleBuilder.build(config, workDir, tempZip)

                // 复制到 FileProvider 共享目录，便于打开安装
                val shareDir = File(ctx.externalCacheDir, "modules").apply { mkdirs() }
                val displayName = "${config.id}_${config.version}.zip"
                val shareFile = File(shareDir, displayName)
                if (shareFile.exists()) shareFile.delete()
                tempZip.inputStream().use { input ->
                    shareFile.outputStream().use { output -> input.copyTo(output) }
                }
                tempZip.delete()

                // 同步保存到 Download 目录
                val downloadsUri: Uri? = try {
                    fileSaver.saveToDownloads(shareFile, displayName)
                } catch (t: Throwable) {
                    null
                }

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        lastGeneratedFile = shareFile,
                        lastDisplayName = displayName
                    )
                }
                _events.value = UiEvent.ModuleGenerated(
                    file = shareFile,
                    displayName = displayName,
                    downloadsUri = downloadsUri,
                    packageCount = result.packageCount,
                    totalBytes = result.totalApkBytes
                )
            } catch (t: Throwable) {
                _uiState.update { it.copy(isGenerating = false) }
                _events.value = UiEvent.Error("生成失败: ${t.message ?: t.javaClass.simpleName}")
            }
        }
    }

    fun buildOpenInstallIntent(): Intent? {
        val file = _uiState.value.lastGeneratedFile ?: return null
        return fileSaver.buildOpenInstallIntent(file)
    }

    fun buildShareIntent(): Intent? {
        val file = _uiState.value.lastGeneratedFile ?: return null
        return fileSaver.buildShareIntent(file)
    }

    // ===== 状态容器 =====

    enum class AppFilter { ALL, USER, SYSTEM, SELECTED }

    data class UiState(
        val isLoading: Boolean = false,
        val isLoaded: Boolean = false,
        val isGenerating: Boolean = false,
        val allApps: List<AppInfo> = emptyList(),
        val filteredApps: List<AppInfo> = emptyList(),
        val searchQuery: String = "",
        val filter: AppFilter = AppFilter.ALL,
        val showSystem: Boolean = false,
        val selectedPackages: Set<String> = emptySet(),
        val error: String? = null,
        val lastGeneratedFile: File? = null,
        val lastDisplayName: String? = null,
        val currentTab: Tab = Tab.Apps
    ) {
        val selectedCount: Int get() = selectedPackages.size
    }

    sealed class UiEvent {
        data class Info(val message: String) : UiEvent()
        data class Error(val message: String) : UiEvent()
        data class ModuleGenerated(
            val file: File,
            val displayName: String,
            val downloadsUri: Uri?,
            val packageCount: Int,
            val totalBytes: Long
        ) : UiEvent()
    }
}
