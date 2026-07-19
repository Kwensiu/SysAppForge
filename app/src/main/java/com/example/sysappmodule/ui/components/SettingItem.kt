package com.example.sysappmodule.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.sysappmodule.ui.theme.CornerRadius

@Composable
fun SettingItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    trailingContent: @Composable () -> Unit = {}
) {
    val baseCorners = LocalItemCorners.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressed = isPressed && onClick != null

    val topStart by animateDpAsState(
        targetValue = if (pressed) CornerRadius else baseCorners.topStart,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "topStart"
    )
    val topEnd by animateDpAsState(
        targetValue = if (pressed) CornerRadius else baseCorners.topEnd,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "topEnd"
    )
    val bottomStart by animateDpAsState(
        targetValue = if (pressed) CornerRadius else baseCorners.bottomStart,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "bottomStart"
    )
    val bottomEnd by animateDpAsState(
        targetValue = if (pressed) CornerRadius else baseCorners.bottomEnd,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "bottomEnd"
    )

    val currentShape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomStart = bottomStart,
        bottomEnd = bottomEnd
    )

    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val descriptionColor = if (selected) {
        contentColor.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconColor = if (selected) {
        contentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val clickableModifier = if (onClick != null && enabled) {
        Modifier
            .clip(currentShape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick
            )
    } else {
        Modifier.clip(currentShape)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = currentShape)
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleStyle,
                color = contentColor
            )
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = descriptionStyle,
                    color = descriptionColor
                )
            }
        }

        Box(contentAlignment = Alignment.Center) {
            trailingContent()
        }
    }
}
