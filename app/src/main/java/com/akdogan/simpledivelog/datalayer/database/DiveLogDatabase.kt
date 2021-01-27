package com.akdogan.simpledivelog.datalayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DatabaseDiveLogEntry::class], version = 3, exportSchema = false)
abstract class DiveLogDatabase : RoomDatabase(){

    abstract val diveLogDatabaseDao: DiveLogDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: DiveLogDatabase? = null

        fun getInstance(context: Context): DiveLogDatabase{
            synchronized(this){
                var instance = INSTANCE
                if (instance == null){
                    instance = Room.databaseBuilder(
                        context,
                        DiveLogDatabase::class.java,
                        "dive_log_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }

    }
}