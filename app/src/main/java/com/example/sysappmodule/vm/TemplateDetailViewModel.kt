package com.example.sysappmodule.vm

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sysappmodule.data.AppInfo
import com.example.sysappmodule.data.InstallMode
import com.example.sysappmodule.data.ModuleConfig
import com.example.sysappmodule.data.ModuleTemplate
import com.example.sysappmodule.data.PackageLoader
import com.example.sysappmodule.data.SelectedApp
import com.example.sysappmodule.data.SelectedPackage
import com.example.sysappmodule.data.TemplateRepository
import com.example.sysappmodule.module.ApkExtractor
import com.example.sysappmodule.module.ModuleBuilder
import com.example.sysappmodule.util.FileSaver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class TemplateDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TemplateRepository(app)
    private val packageLoader = PackageLoader(app)
    private val moduleBuilder = ModuleBuilder(ApkExtractor())
    private val fileSaver = FileSaver(app)

    private val _template = MutableStateFlow<ModuleTemplate?>(null)
    val template: StateFlow<ModuleTemplate?> = _template.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(AppFilter.ALL)
    val filter: StateFlow<AppFilter> = _filter.asStateFlow()

    private val _showSystem = MutableStateFlow(false)
    val showSystem: StateFlow<Boolean> = _showSystem.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>(replay = 1)
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    private var loadedTemplateId: String? = null
    private var templateJob: Job? = null

    fun load(templateId: String) {
        if (templateId == loadedTemplateId && templateJob?.isActive == true) return
        loadedTemplateId = templateId
        templateJob?.cancel()
        templateJob = viewModelScope.launch {
            repo.templates.collect { list ->
                val t = list.firstOrNull { it.id == templateId }
                _template.value = t
                if (t != null && _allApps.value.isEmpty()) {
                    loadApps()
                }
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                _allApps.value = packageLoader.loadInstalledApps(includeSystem = true)
            } catch (_: Throwable) {}
        }
    }

    fun updateSearchQuery(q: String) { _searchQuery.value = q }
    fun updateFilter(f: AppFilter) { _filter.value = f }
    fun toggleShowSystem() { _showSystem.value = !_showSystem.value }
    fun updateTemplate(transform: (ModuleTemplate) -> ModuleTemplate) {
        val current = _template.value ?: return
        val newTemplate = transform(current).copy(updatedAt = System.currentTimeMillis())
        _template.value = newTemplate
        viewModelScope.launch { repo.upsert(newTemplate) }
    }

    fun toggleApp(app: AppInfo) {
        val current = _template.value ?: return
        val newSelection = current.selectedApps.toMutableList()
        val idx = newSelection.indexOfFirst { it.packageName == app.packageName }
        if (idx >= 0) newSelection.removeAt(idx)
        else newSelection.add(SelectedPackage(app.packageName, InstallMode.PRIV_APP))
        updateTemplate { it.copy(selectedApps = newSelection) }
    }

    fun setInstallMode(packageName: String, mode: InstallMode) {
        val current = _template.value ?: return
        val newSelection = current.selectedApps.map { sel ->
            if (sel.packageName == packageName) sel.copy(mode = mode) else sel
        }
        updateTemplate { it.copy(selectedApps = newSelection) }
    }

    fun deleteTemplate() {
        val current = _template.value ?: return
        viewModelScope.launch {
            repo.delete(current.id)
            _events.emit(DetailEvent.Deleted)
        }
    }

    /** 转换为 ModuleConfig 以供 ModuleBuilder 使用。需要从已加载的应用列表反查 AppInfo。 */
    fun toModuleConfig(): ModuleConfig? {
        val t = _template.value ?: return null
        val apps = _allApps.value
        val selectedApps = t.selectedApps.mapNotNull { sel ->
            val appInfo = apps.firstOrNull { it.packageName == sel.packageName } ?: return@mapNotNull null
            SelectedApp(appInfo, sel.mode)
        }
        return ModuleConfig(
            id = t.moduleId,
            name = t.name,
            version = t.version,
            versionCode = t.versionCode,
            author = t.author,
            description = t.description,
            selectedApps = selectedApps,
        )
    }

    fun generateModule() {
        val template = _template.value
        val config = toModuleConfig()
        if (config == null || config.selectedApps.isEmpty()) {
            viewModelScope.launch { _events.emit(DetailEvent.Error("请至少选择一个应用")) }
            return
        }
        val missingPackages = template.orEmptySelectedPackageNames() -
            config.selectedApps.map { it.app.packageName }.toSet()
        if (missingPackages.isNotEmpty()) {
            viewModelScope.launch {
                _events.emit(
                    DetailEvent.Error("以下应用当前不可用，请返回移除后重试: ${missingPackages.joinToString()}")
                )
            }
            return
        }
        if (!ModuleBuilder.MODULE_ID_REGEX.matches(config.id)) {
            viewModelScope.launch { _events.emit(DetailEvent.Error("模块 ID 非法")) }
            return
        }
        viewModelScope.launch {
            _isGenerating.value = true
            _events.emit(DetailEvent.Info("正在生成模块…"))
            try {
                val ctx = getApplication<Application>()
                val workDir = File(ctx.cacheDir, "module_work_${System.currentTimeMillis()}")
                val tempZip = File(ctx.cacheDir, "module_temp_${config.id}_${System.currentTimeMillis()}.zip")
                val result = moduleBuilder.build(config, workDir, tempZip)

                // externalCacheDir can be null while shared storage is unavailable.
                val shareRoot = ctx.externalCacheDir ?: ctx.cacheDir
                val shareDir = File(shareRoot, "modules").apply { mkdirs() }
                val displayName = "${config.id}_${config.version}.zip"
                val shareFile = File(shareDir, displayName)
                if (shareFile.exists()) shareFile.delete()
                tempZip.inputStream().use { input ->
                    shareFile.outputStream().use { output -> input.copyTo(output) }
                }
                tempZip.delete()

                val downloadsUri: Uri? = try {
                    fileSaver.saveToDownloads(shareFile, displayName)
                } catch (_: Throwable) { null }

                _events.emit(DetailEvent.ModuleGenerated(
                    file = shareFile,
                    displayName = displayName,
                    downloadsUri = downloadsUri,
                    packageCount = result.packageCount,
                    totalBytes = result.totalApkBytes
                ))
            } catch (t: Throwable) {
                _events.emit(DetailEvent.Error("生成失败: ${t.message ?: t.javaClass.simpleName}"))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun buildOpenInstallIntent(file: File): Intent = fileSaver.buildOpenInstallIntent(file)
    fun buildShareIntent(file: File): Intent = fileSaver.buildShareIntent(file)

    private fun ModuleTemplate?.orEmptySelectedPackageNames(): Set<String> =
        this?.selectedApps?.mapTo(linkedSetOf()) { it.packageName }.orEmpty()

    sealed class DetailEvent {
        data class Info(val message: String) : DetailEvent()
        data class Error(val message: String) : DetailEvent()
        data class ModuleGenerated(
            val file: File,
            val displayName: String,
            val downloadsUri: Uri?,
            val packageCount: Int,
            val totalBytes: Long
        ) : DetailEvent()
        object Deleted : DetailEvent()
    }
}

enum class AppFilter { ALL, USER, SYSTEM, SELECTED }
