package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ControllerButtonConfig::class,
        EmulatorConfig::class,
        GameProfile::class,
        GameStats::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EmulatorDatabase : RoomDatabase() {
    abstract fun emulatorDao(): EmulatorDao

    companion object {
        @Volatile
        private var INSTANCE: EmulatorDatabase? = null

        fun getDatabase(context: Context): EmulatorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EmulatorDatabase::class.java,
                    "emulator_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
