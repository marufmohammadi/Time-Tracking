package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreSyncManager
import com.example.data.repository.TimeRepository

object DatabaseProvider {
    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var repository: TimeRepository? = null

    fun getRepository(context: Context): TimeRepository {
        return repository ?: synchronized(this) {
            repository ?: run {
                val db = database ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "time_progress_db"
                )
                .fallbackToDestructiveMigration() // Prevent crashes if base models alter
                .build()
                .also { database = it }

                val dao = db.timeSessionDao()
                val syncManager = FirestoreSyncManager(context.applicationContext, dao)
                val repo = TimeRepository(dao, syncManager)
                repository = repo
                repo
            }
        }
    }
}
