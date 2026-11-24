package com.example.recordatoriosapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.recordatoriosapp.data.local.dao.ReminderDao
import com.example.recordatoriosapp.data.local.entity.ReminderEntity

@Database(entities = [ReminderEntity::class], version = 2, exportSchema = false)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                // Usamos migración destructiva porque estamos en fase de desarrollo 
                // y agregamos el campo 'userId' que rompe el esquema anterior.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
