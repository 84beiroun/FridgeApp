package com.example.fridgeapp.handlers

import androidx.room.*
import com.example.fridgeapp.data.FridgeSnap
import io.reactivex.Completable
import io.reactivex.Flowable

//оригинальная dao
@Dao
interface FridgeSnapDao {
    @Query("SELECT * FROM FridgeSnap")
    fun getAll(): Flowable<List<FridgeSnap>>

    @Insert
    fun insertSnap(fridgeSnap: FridgeSnap) : Completable

    @Query("DELETE FROM FridgeSnap WHERE id LIKE :id")
    fun deleteSnap(id: Int) : Completable

    @Update
    fun updateSnap(fridgeSnap: FridgeSnap) : Completable
}