package com.example.ducktrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ducktrack.ui.main.garden.SeedType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    // THÊM TaskEntity::class VÀO ĐÂY
    entities = [UserProfile::class, UnlockedSeed::class, GrownTree::class, TaskEntity::class],
    version = 2, // TĂNG VERSION TỪ 1 LÊN 2
    exportSchema = false // Tắt lỗi schema export
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ducktrack_database"
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration() // Cho phép xóa dữ liệu cũ nếu đổi version
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val defaultSeed = UnlockedSeed(SeedType.NORMAL.id)
                    database.userDao().insertUnlockedSeed(defaultSeed)
                    val defaultProfile = UserProfile(id = 1, points = 0)
                    database.userDao().upsertUserProfile(defaultProfile)
                }
            }
        }
    }
}