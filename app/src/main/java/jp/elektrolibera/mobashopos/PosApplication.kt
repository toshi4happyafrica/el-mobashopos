package jp.elektrolibera.mobashopos

import android.app.Application
import android.content.Context
import androidx.room.Room
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.data.api.ApiClient
import jp.elektrolibera.mobashopos.data.db.MasterDatabase
import jp.elektrolibera.mobashopos.data.settings.SettingsStore

/** 依存関係の組み立て（手動 DI） */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, MasterDatabase::class.java, "master.db")
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val repository = PosRepository(
        settings = SettingsStore(context),
        db = database,
        client = ApiClient(),
    )
}

class PosApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
