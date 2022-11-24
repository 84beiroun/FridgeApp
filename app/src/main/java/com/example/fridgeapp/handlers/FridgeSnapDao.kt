package com.example.fridgeapp.handlers

import androidx.room.*
import com.example.fridgeapp.data.FridgeSnap

//оригинальная dao
@Dao
interface FridgeSnapDao {
    @Query("SELECT * FROM FridgeSnap")
    suspend fun getAll(): List<FridgeSnap>

    @Query("SELECT * FROM FridgeSnap WHERE id LIKE :id")
    suspend fun loadById(id: Int): FridgeSnap

    @Insert
    suspend fun insertSnap(fridgeSnap: FridgeSnap)

    @Delete
    suspend fun deleteSnap(fridgeSnap: FridgeSnap)

    @Update
    suspend fun updateSnap(fridgeSnap: FridgeSnap)
}