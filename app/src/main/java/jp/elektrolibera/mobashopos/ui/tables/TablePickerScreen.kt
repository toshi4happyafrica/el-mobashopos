package jp.elektrolibera.mobashopos.ui.tables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.elektrolibera.mobashopos.R
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.ui.common.PosScaffold

/** テーブル番号を選ぶ画面（旧 顧客情報入力画面に相当。手入力ではなく一覧から選ぶ） */
@Composable
fun TablePickerScreen(
    repo: PosRepository,
    title: String,
    onBack: () -> Unit,
    onSelect: (tableId: String) -> Unit,
) {
    val tables by repo.tables.collectAsState(initial = emptyList())
    PosScaffold(title = title, onBack = onBack) { padding ->
        if (tables.isEmpty()) {
            Text(
                stringResource(R.string.tables_empty),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp),
            ) {
                items(tables, key = { it.id }) { t ->
                    Button(
                        onClick = { onSelect(t.id) },
                        modifier = Modifier
                            .padding(4.dp)
                            .height(72.dp),
                    ) {
                        Text(t.name ?: t.code, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
