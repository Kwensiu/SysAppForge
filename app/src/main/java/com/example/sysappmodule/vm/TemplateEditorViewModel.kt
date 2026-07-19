package com.example.sysappmodule.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sysappmodule.data.ModuleTemplate
import com.example.sysappmodule.data.TemplateRepository
import com.example.sysappmodule.module.ModuleBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TemplateEditorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TemplateRepository(app)

    private val _draft = MutableStateFlow<ModuleTemplate?>(null)
    val draft: StateFlow<ModuleTemplate?> = _draft.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private var loadedTemplateId: String? = null

    fun createDraft() {
        if (_draft.value == null) _draft.value = ModuleTemplate.createDraft()
    }

    fun load(templateId: String) {
        if (templateId == loadedTemplateId) return
        loadedTemplateId = templateId
        viewModelScope.launch {
            _draft.value = repo.templates.first().firstOrNull { it.id == templateId }
            if (_draft.value == null) _events.emit(EditorEvent.NotFound)
        }
    }

    fun update(transform: (ModuleTemplate) -> ModuleTemplate) {
        _draft.value = _draft.value?.let(transform)
    }

    fun save() {
        val current = _draft.value ?: return
        if (!current.isMetadataValid()) return
        val normalized = current.copy(
            templateName = current.templateName.trim(),
            moduleId = current.moduleId.trim(),
            name = current.name.trim(),
            version = current.version.trim(),
            author = current.author.trim(),
            description = current.description.trim(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repo.upsert(normalized)
            _events.emit(EditorEvent.Saved)
        }
    }

    sealed interface EditorEvent {
        data object Saved : EditorEvent
        data object NotFound : EditorEvent
    }
}

fun ModuleTemplate.isMetadataValid(): Boolean =
    templateName.isNotBlank() &&
        name.isNotBlank() &&
        version.isNotBlank() &&
        versionCode > 0 &&
        ModuleBuilder.MODULE_ID_REGEX.matches(moduleId.trim())
