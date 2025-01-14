package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors

@Composable
fun AdjustableOutlinedTextField(
    value: String,
    hint: String = "",
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = false,
    colors: TextFieldColors = outlinedTextFieldColors(),
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = MaterialTheme.typography.body1.fontSize,
        color = MaterialTheme.colors.onBackground,
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(4.dp),
    backgroundColor: Color = MaterialTheme.colors.background,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }

    val cursorColor = colors.cursorColor(isError).value
    val indicatorColor by colors.indicatorColor(enabled, isError, interactionSource)

    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
    ) {
        BasicTextField(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .background(backgroundColor)
                .fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            maxLines = maxLines,
            textStyle = textStyle.copy(color = textColor),
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(cursorColor),
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = indicatorColor,
                            shape = shape
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    innerTextField()
                }
            }
        )

        if (value.isEmpty() && hint.isNotEmpty()) {
            Text(
                hint,
                color = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier
                    .padding(start = 12.dp, top = 12.dp),
                style = MaterialTheme.typography.body2
            )
        }
    }
}