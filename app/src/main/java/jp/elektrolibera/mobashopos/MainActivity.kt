package jp.elektrolibera.mobashopos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import jp.elektrolibera.mobashopos.ui.PosApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as PosApplication).container.repository
        setContent { PosApp(repo) }
    }
}
