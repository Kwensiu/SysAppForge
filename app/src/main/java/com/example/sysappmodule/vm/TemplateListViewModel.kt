package com.example.sysappmodule.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sysappmodule.data.ModuleTemplate
import com.example.sysappmodule.data.TemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TemplateListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TemplateRepository(app)

    val templates: StateFlow<List<ModuleTemplate>> = repo.templates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _events = MutableStateFlow<TemplateListEvent?>(null)
    val events: StateFlow<TemplateListEvent?> = _events.asStateFlow()

    fun createTemplate(): String {
        val newTemplate = ModuleTemplate(
            id = UUID.randomUUID().toString(),
            moduleId = "my_module",
            name = "我的模块",
            version = "v1.0.0",
            versionCode = 1L,
            author = "",
            description = ""
        )
        viewModelScope.launch {
            repo.upsert(newTemplate)
            _events.value = TemplateListEvent.NavigateToDetail(newTemplate.id)
        }
        return newTemplate.id
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun consumeEvent() { _events.value = null }

    sealed class TemplateListEvent {
        data class NavigateToDetail(val templateId: String) : TemplateListEvent()
    }
}
