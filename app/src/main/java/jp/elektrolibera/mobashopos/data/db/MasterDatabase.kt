package jp.elektrolibera.mobashopos.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

// 端末内のマスターキャッシュ（サーバーの GET /api/v1/master の内容を保存）

@Entity(tableName = "master_meta")
data class MasterMetaEntity(
    @PrimaryKey val key: Int = 0,
    val version: Int,
    val storeId: String,
    val storeName: String,
    val baseCurrency: String,
    val minorUnit: Int,
)

@Entity(tableName = "dining_tables")
data class TableEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String?,
    val sortOrder: Int,
)

@Entity(tableName = "menu_categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val name: String,
    val priceAmount: Long,
    val currency: String,
    val sortOrder: Int,
)

/**
 * KSP2 の既知の不具合（Unit を返す suspend DAO メソッドで "unexpected jvm signature V"）を避けるため、
 * suspend メソッドはすべて値（件数・行ID）を返し、DAO に @Transaction のメソッドを置かない。
 * まとめて置き換える処理はファイル末尾の拡張関数 replaceMaster()（withTransaction）で行う。
 */
@Dao
interface MasterDao {
    @Query("SELECT * FROM master_meta WHERE `key` = 0")
    fun meta(): Flow<MasterMetaEntity?>

    @Query("SELECT * FROM dining_tables ORDER BY sortOrder, code")
    fun tables(): Flow<List<TableEntity>>

    @Query("SELECT * FROM menu_categories ORDER BY sortOrder, name")
    fun categories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM menu_items ORDER BY sortOrder, name")
    fun items(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM dining_tables WHERE id = :id")
    suspend fun table(id: String): TableEntity?

    @Query("DELETE FROM master_meta")
    suspend fun clearMeta(): Int

    @Query("DELETE FROM dining_tables")
    suspend fun clearTables(): Int

    @Query("DELETE FROM menu_categories")
    suspend fun clearCategories(): Int

    @Query("DELETE FROM menu_items")
    suspend fun clearItems(): Int

    @Insert
    suspend fun insertMeta(meta: MasterMetaEntity): Long

    @Insert
    suspend fun insertTables(tables: List<TableEntity>): List<Long>

    @Insert
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Insert
    suspend fun insertItems(items: List<MenuItemEntity>): List<Long>
}

@Database(
    entities = [MasterMetaEntity::class, TableEntity::class, CategoryEntity::class, MenuItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MasterDatabase : RoomDatabase() {
    abstract fun masterDao(): MasterDao
}

// 以下は @Database クラスの外に置く（Room/KSP の処理対象にしないため）

/** マスター一式を丸ごと置き換える（1トランザクション） */
suspend fun MasterDatabase.replaceMaster(
    meta: MasterMetaEntity,
    tables: List<TableEntity>,
    categories: List<CategoryEntity>,
    items: List<MenuItemEntity>,
) {
    withTransaction {
        val dao = masterDao()
        clearWith(dao)
        dao.insertMeta(meta)
        dao.insertTables(tables)
        dao.insertCategories(categories)
        dao.insertItems(items)
    }
}

suspend fun MasterDatabase.clearMaster() {
    withTransaction { clearWith(masterDao()) }
}

private suspend fun clearWith(dao: MasterDao) {
    dao.clearMeta()
    dao.clearTables()
    dao.clearCategories()
    dao.clearItems()
}
