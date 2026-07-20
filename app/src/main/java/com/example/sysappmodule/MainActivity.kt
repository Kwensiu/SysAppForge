package com.example.sysappmodule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sysappmodule.ui.AppNavigation
import com.example.sysappmodule.ui.theme.SysAppModuleTheme
import com.example.sysappmodule.vm.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val themeMode by settingsVm.themeMode.collectAsState()
            val dynamicColor by settingsVm.dynamicColor.collectAsState()
            SysAppModuleTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                AppNavigation()
            }
        }
    }
}
