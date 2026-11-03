package jp.elektrolibera.mobashopos.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.elektrolibera.mobashopos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                    }
                },
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

/** 処理中はくるくるを表示して押せなくするボタン */
@Composable
fun BusyButton(
    text: String,
    busy: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = modifier) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
