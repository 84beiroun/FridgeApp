package com.example.fridgeapp.injector.repository

import com.example.fridgeapp.data.FridgeSnap
import io.reactivex.Completable
import io.reactivex.Flowable

//репозиторий функций дао бд
interface SnapsRepository {
    fun getAll(): Flowable<List<FridgeSnap>>

    fun insertSnap(fridgeSnap: FridgeSnap) : Completable

    fun deleteSnap(id: Int) : Completable

    fun updateSnap(fridgeSnap: FridgeSnap): Completable
}