package com.example.sysappmodule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.sysappmodule.ui.theme.ConnectionRadius
import com.example.sysappmodule.ui.theme.CornerRadius

@Immutable
data class SettingItemData(
    val key: Any,
    val content: @Composable (Shape) -> Unit
)

class SettingsGroupScope {
    val items = mutableListOf<SettingItemData>()

    fun item(key: Any? = null, content: @Composable (Shape) -> Unit) {
        items.add(SettingItemData(key ?: items.size, content))
    }
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: SettingsGroupScope.() -> Unit
) {
    val scope = remember { SettingsGroupScope().apply(content) }
    val items = scope.items

    Column(modifier = modifier.padding(contentPadding)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((-2).dp)
        ) {
            items.forEachIndexed { index, itemData ->
                val isFirst = index == 0
                val isLast = index == items.lastIndex

                val topRadius = if (isFirst) CornerRadius else ConnectionRadius
                val bottomRadius = if (isLast) CornerRadius else ConnectionRadius

                val corners = ItemCorners(
                    topStart = topRadius,
                    topEnd = topRadius,
                    bottomStart = bottomRadius,
                    bottomEnd = bottomRadius
                )

                val shape = RoundedCornerShape(
                    topStart = topRadius,
                    topEnd = topRadius,
                    bottomStart = bottomRadius,
                    bottomEnd = bottomRadius
                )

                CompositionLocalProvider(LocalItemCorners provides corners) {
                    itemData.content(shape)
                }
            }
        }
    }
}
