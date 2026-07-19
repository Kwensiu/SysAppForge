package com.example.sysappmodule.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sysappmodule.data.ModuleTemplate
import com.example.sysappmodule.data.TemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TemplateListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TemplateRepository(app)

    val templates: StateFlow<List<ModuleTemplate>> = repo.templates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun createTemplate(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return
        val newTemplate = ModuleTemplate(
            id = UUID.randomUUID().toString(),
            moduleId = "my_module",
            name = normalizedName,
            version = "v1.0.0",
            versionCode = 1L,
            author = "",
            description = ""
        )
        viewModelScope.launch {
            repo.upsert(newTemplate)
        }
    }

    fun renameTemplate(id: String, name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return
        val template = templates.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repo.upsert(template.copy(name = normalizedName))
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}
