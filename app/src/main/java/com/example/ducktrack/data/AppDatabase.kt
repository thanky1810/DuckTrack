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
    entities = [UserProfile::class, UnlockedSeed::class, GrownTree::class],
    version = 1 // Tăng số này nếu bạn thay đổi cấu trúc bảng
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
                    "ducktrack_database" // Tên file database
                )
                    .addCallback(DatabaseCallback(context)) // Thêm callback
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // Callback để tự động thêm cây "normal" vào CSDL khi tạo lần đầu
    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                // Dùng coroutine để thêm dữ liệu
                CoroutineScope(Dispatchers.IO).launch {
                    // Thêm cây "normal" làm cây mặc định
                    val defaultSeed = UnlockedSeed(SeedType.NORMAL.id)
                    database.userDao().insertUnlockedSeed(defaultSeed)

                    // Thêm user profile mặc định với 0 điểm
                    val defaultProfile = UserProfile(id = 1, points = 0)
                    database.userDao().upsertUserProfile(defaultProfile)
                }
            }
        }
    }
}