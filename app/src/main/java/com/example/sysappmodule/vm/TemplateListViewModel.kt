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

class TemplateListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TemplateRepository(app)

    val templates: StateFlow<List<ModuleTemplate>> = repo.templates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun deleteTemplate(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}
