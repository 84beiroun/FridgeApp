package com.example.fridgeapp.helpers.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fridgeapp.helpers.data.FridgeSnap
import com.example.fridgeapp.helpers.data.FridgeSnapDao

//сама бд
@Database(entities = [FridgeSnap::class], version = 6)
abstract class FridgeSnapsDB : RoomDatabase() {
    abstract fun getDao(): FridgeSnapDao
}
