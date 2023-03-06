package com.saket.productscanner.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saket.productscanner.models.Product

@Database(entities = [Product::class], version = 1)
abstract class ProductDatabase: RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {

        @Volatile
        private var instance: ProductDatabase? = null

        fun getDatabase(context: Context): ProductDatabase{
            if (instance == null){
                synchronized(this){
                    instance = Room.databaseBuilder(context, ProductDatabase::class.java,"productDB").build()
                }
            }
            return instance!!
        }
    }
}