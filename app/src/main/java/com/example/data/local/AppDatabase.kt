package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.TimeSession

@Database(entities = [TimeSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeSessionDao(): TimeSessionDao
}
