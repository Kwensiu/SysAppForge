package com.example.sysappmodule.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.templatesDataStore by preferencesDataStore(name = "templates")
private val Context.themeDataStore by preferencesDataStore(name = "theme")

private val TEMPLATES_KEY = stringPreferencesKey("templates_json")
private val THEME_KEY = stringPreferencesKey("theme_mode")
private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")

/**
 * 模板仓库。持久化到 DataStore Preferences，序列化方式为 JSON。
 */
class TemplateRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val listSerializer = ListSerializer(ModuleTemplate.serializer())

    val templates: Flow<List<ModuleTemplate>> = context.templatesDataStore.data.map { prefs ->
        prefs[TEMPLATES_KEY]?.let {
            runCatching { json.decodeFromString<List<ModuleTemplate>>(it) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun save(templates: List<ModuleTemplate>) {
        context.templatesDataStore.edit { prefs ->
            prefs[TEMPLATES_KEY] = json.encodeToString(listSerializer, templates)
        }
    }

    suspend fun upsert(template: ModuleTemplate) {
        context.templatesDataStore.edit { prefs ->
            val current = prefs[TEMPLATES_KEY]?.let {
                runCatching { json.decodeFromString<List<ModuleTemplate>>(it) }.getOrNull()
            } ?: emptyList()
            val updated = template.copy(updatedAt = System.currentTimeMillis())
            val newList = current.toMutableList().apply {
                val idx = indexOfFirst { it.id == template.id }
                if (idx >= 0) set(idx, updated) else add(updated)
            }
            prefs[TEMPLATES_KEY] = json.encodeToString(listSerializer, newList)
        }
    }

    suspend fun delete(templateId: String) {
        context.templatesDataStore.edit { prefs ->
            val current = prefs[TEMPLATES_KEY]?.let {
                runCatching { json.decodeFromString<List<ModuleTemplate>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[TEMPLATES_KEY] = json.encodeToString(listSerializer, current.filterNot { it.id == templateId })
        }
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class ThemeRepository(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val dynamicColor: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_KEY] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name.lowercase()
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_KEY] = enabled
        }
    }
}
