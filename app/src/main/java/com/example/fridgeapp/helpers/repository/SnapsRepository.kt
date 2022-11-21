package com.example.fridgeapp.helpers.repository

import com.example.fridgeapp.helpers.data.FridgeSnap

//репозиторий функций дао бд
interface SnapsRepository {
    suspend fun getAll(): List<FridgeSnap>

    suspend fun loadById(id: Int): FridgeSnap

    suspend fun insertSnap(fridgeSnap: FridgeSnap)

    suspend fun deleteSnap(fridgeSnap: FridgeSnap)

    suspend fun updateSnap(fridgeSnap: FridgeSnap)
}