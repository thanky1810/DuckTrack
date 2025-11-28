package com.example.ducktrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. Bảng lưu điểm, chúng ta chỉ có 1 user, nên id luôn là 1
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val points: Int // Số điểm sao 🌟
)

// 2. Bảng lưu các hạt giống đã mở khóa
@Entity(tableName = "unlocked_seeds")
data class UnlockedSeed(
    @PrimaryKey val seedId: String // "normal", "pine", v.v.
)

// 3. Bảng lưu các cây đã trồng
@Entity(tableName = "grown_trees")
data class GrownTree(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ID tự tăng
    val seedId: String // "normal", "pine", v.v.
)