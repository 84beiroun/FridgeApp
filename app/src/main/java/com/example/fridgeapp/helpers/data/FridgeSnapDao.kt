package com.example.fridgeapp.helpers.data

import androidx.room.*

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