package com.example.ducktrack

import android.app.Application
import com.example.ducktrack.data.AppDatabase
import com.example.ducktrack.data.UserDataRepository

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { UserDataRepository(database.userDao()) }
}