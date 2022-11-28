package com.example.fridgeapp.injector.repository

import com.example.fridgeapp.data.FridgeSnap
import io.reactivex.Flowable
import io.reactivex.Observable

//репозиторий функций дао бд
interface SnapsRepository {
    fun getAll(): Flowable<List<FridgeSnap>>

    suspend fun loadById(id: Int): FridgeSnap

    suspend fun insertSnap(fridgeSnap: FridgeSnap)

    suspend fun deleteSnap(id: Int)

    suspend fun updateSnap(fridgeSnap: FridgeSnap)
}