package com.example.fridgeapp.injector.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.handlers.FridgeSnapDao

//сама бд
@Database(entities = [FridgeSnap::class], version = 6)
abstract class FridgeSnapsDB : RoomDatabase() {
    abstract fun getDao(): FridgeSnapDao
}
