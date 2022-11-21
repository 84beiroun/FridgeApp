package com.example.fridgeapp.helpers.repository

import com.example.fridgeapp.helpers.data.FridgeSnap
import com.example.fridgeapp.helpers.data.FridgeSnapDao
import javax.inject.Inject

//сурсы бд
class SnapsDataSource @Inject constructor(
    private val fridgeSnapDao: FridgeSnapDao
) : SnapsRepository {
    override suspend fun getAll() = fridgeSnapDao.getAll()
    override suspend fun loadById(id: Int) = fridgeSnapDao.loadById(id)
    override suspend fun insertSnap(fridgeSnap: FridgeSnap) = fridgeSnapDao.insertSnap(fridgeSnap)
    override suspend fun deleteSnap(fridgeSnap: FridgeSnap) = fridgeSnapDao.deleteSnap(fridgeSnap)
    override suspend fun updateSnap(fridgeSnap: FridgeSnap) = fridgeSnapDao.updateSnap(fridgeSnap)
}