package com.asinosoft.vpn.ui

import android.util.Patterns
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.model.RestoreSubscriptionUiState
import com.asinosoft.vpn.ui.components.EmailTextField
import com.asinosoft.vpn.ui.components.FullSizeColumn
import com.asinosoft.vpn.ui.theme.Golden
import com.asinosoft.vpn.ui.theme.Typography

@Composable
fun RestoreSubscriptionView(
    state: RestoreSubscriptionUiState,
    onRestoreSubscription: (String) -> Unit = {},
    onClose: () -> Unit = {},
) {
    when (state) {
        RestoreSubscriptionUiState.EnterEmail -> EnterEmailView(onRestoreSubscription)
        is RestoreSubscriptionUiState.SendRequest -> WaitForResponse(state.email)
        is RestoreSubscriptionUiState.RequestSucceed -> Success(state.email, onClose)
        is RestoreSubscriptionUiState.RequestFailed -> Failure(state.error, onClose)
    }
}

@Preview(showSystemUi = true, locale = "ru")
@Composable
private fun EnterEmailView(
    onRestoreSubscription: (String) -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }

    FullSizeColumn {

        Spacer(Modifier.height(80.dp))

        Text(
            text = stringResource(R.string.restore_subscription_prompt1),
            textAlign = TextAlign.Center,
            style = Typography.bodyLarge,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.restore_subscription_prompt2),
            textAlign = TextAlign.Center,
            style = Typography.bodyLarge,
        )

        Spacer(Modifier.height(24.dp))

        EmailTextField(
            email = email,
            onValueChange = { email = it },
            onSend = { onRestoreSubscription(email) },
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onRestoreSubscription(email) },
            enabled = Patterns.EMAIL_ADDRESS.matcher(email).matches(),
        ) {
            Text(stringResource(R.string.restore))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun WaitForResponse(
    email: String = "user@example.com",
) {
    FullSizeColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.Email, contentDescription = "Email", tint = Golden)
            Spacer(Modifier.width(16.dp))
            Text(email)
        }

        Text(stringResource(R.string.sending), style = Typography.titleMedium)

        CircularProgressIndicator()
    }
}

@Preview(showSystemUi = true)
@Composable
private fun Success(
    email: String = "user@example.com",
    onClose: () -> Unit = {},
) {
    FullSizeColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.Email, contentDescription = "Email", tint = Golden)
            Spacer(Modifier.width(16.dp))
            Text(email)
        }

        Text(stringResource(R.string.sent), style = Typography.titleMedium)

        Button(onClick = onClose) {
            Text(stringResource(R.string.close))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun Failure(
    error: String = "Some error ".repeat(20),
    onClose: () -> Unit = {},
) {
    FullSizeColumn {
        Text(
            text = error,
            style = Typography.titleMedium,
            color = Color.Red,
        )

        Button(onClick = onClose) {
            Text(stringResource(R.string.close))
        }
    }
}
