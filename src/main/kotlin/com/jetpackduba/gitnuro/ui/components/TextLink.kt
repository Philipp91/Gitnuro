package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.openUrlInBrowser


@Composable
fun TextLink(
    text: String,
    url: String,
    modifier: Modifier = Modifier,
    colorsInverted: Boolean = false,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    val textColor = if (isHovered == colorsInverted) {
        MaterialTheme.colors.onBackground
    } else {
        MaterialTheme.colors.primaryVariant
    }

    Text(
        text = text,
        modifier = Modifier
            .hoverable(hoverInteraction)
            .handMouseClickable {
                openUrlInBrowser(url)
            }
            .then(modifier),
        color = textColor,
    )
}