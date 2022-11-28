package com.example.fridgeapp.handlers

import androidx.room.*
import com.example.fridgeapp.data.FridgeSnap
import io.reactivex.Flowable
import io.reactivex.Observable

//оригинальная dao
@Dao
interface FridgeSnapDao {
    @Query("SELECT * FROM FridgeSnap")
    fun getAll(): Flowable<List<FridgeSnap>>

    @Query("SELECT * FROM FridgeSnap WHERE id LIKE :id")
    suspend fun loadById(id: Int): FridgeSnap

    @Insert
    suspend fun insertSnap(fridgeSnap: FridgeSnap)

    @Query("DELETE FROM FridgeSnap WHERE id LIKE :id")
    suspend fun deleteSnap(id: Int)

    @Update
    suspend fun updateSnap(fridgeSnap: FridgeSnap)
}