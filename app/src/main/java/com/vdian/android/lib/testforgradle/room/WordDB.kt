package com.vdian.android.lib.testforgradle.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * @author yulun
 * @since 2022-07-25 16:20
 */
@Database(entities = [WordEntity::class], version = 2, exportSchema = false)
abstract class WordDB : RoomDatabase() {

    abstract fun getWordDao(): WordDao

    companion object {
        @Volatile
        private var instantce: WordDB? = null
        private const val DB_NAME = "jetpack_room.db"


        private val MIGRATION_1_2 = object : Migration(1,2){
            override fun migrate(database: SupportSQLiteDatabase) {
                //FRUIT 表  新增一列
                database.execSQL("ALTER TABLE word_table add COLUMN content text")
            }
        }

        fun getInstance(context: Context): WordDB? {
            if (instantce == null) {
                synchronized(WordDB::class.java) {
                    if (instantce == null) {
                        instantce = createInstance(context)
                    }
                }
            }
            return instantce
        }

        private fun createInstance(context: Context): WordDB {
            return Room.databaseBuilder(context.applicationContext, WordDB::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()
        }


    }
}

