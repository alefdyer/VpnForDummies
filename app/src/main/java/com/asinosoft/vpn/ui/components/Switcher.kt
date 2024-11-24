package com.asinosoft.vpn.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import com.asinosoft.vpn.ui.theme.Golden

@Composable
fun Switcher(
    position: Boolean,
    on: () -> Unit = {},
    off: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    var color by remember { mutableStateOf(Color.Transparent) }
    Switch(modifier = Modifier
        .onFocusChanged { state ->
            color = if (state.isFocused) Golden else Color.Transparent
        }
        .focusRequester(focusRequester)
        .focusable(),
        checked = position,
        colors = SwitchDefaults.colors(
            checkedBorderColor = color,
            uncheckedBorderColor = color
        ),
        onCheckedChange = { if (it) on() else off() }
    )
}

@Preview
@Composable
fun PreviewSwitcher() {
    Surface {
        Column {
            Switcher(true)
            Switcher(false)
        }
    }
}
