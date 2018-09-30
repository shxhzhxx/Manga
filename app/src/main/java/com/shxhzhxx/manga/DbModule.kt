package com.shxhzhxx.manga

import androidx.room.*


@Entity(tableName = "settings")
data class PathSettings(@PrimaryKey val path: String, var reverse: Boolean = false,
                        var vertical: Boolean = false, var snap: Boolean = true, var lastTime: String? = null)


@Dao
interface PathSettingsDao {
    @Query("SELECT * FROM settings WHERE path=:path")
    fun load(path: String): PathSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(vararg paths: PathSettings)
}


@Database(entities = [PathSettings::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PathSettingsDao
}