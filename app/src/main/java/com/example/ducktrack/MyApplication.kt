package com.example.ducktrack

import android.app.Application
import com.example.ducktrack.data.AppDatabase
import com.example.ducktrack.data.UserDataRepository

class MyApplication : Application() {

    // Tạo Database và Repository bằng lazy
    // Cả 2 sẽ là singleton
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { UserDataRepository(database.userDao()) }

    override fun onCreate() {
        super.onCreate()
        // Database và Repository sẽ được khởi tạo khi được gọi lần đầu
    }
}