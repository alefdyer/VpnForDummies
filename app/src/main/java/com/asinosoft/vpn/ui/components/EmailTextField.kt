package com.asinosoft.vpn.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun EmailTextField(
    email: String = "",
    onValueChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = email,
        onValueChange = onValueChange,
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth()
            .padding(16.dp),
        singleLine = true,
        placeholder = { Text("example@mail.com") },
        leadingIcon = { Icon(Icons.Filled.Email, "email") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions { onSend() },
    )
}
