package com.example.fridgeapp.injector.repository

import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.handlers.FridgeSnapDao
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

//сурсы бд
class SnapsDataSource @Inject constructor(
    private val fridgeSnapDao: FridgeSnapDao
) : SnapsRepository {
    override fun getAll() = fridgeSnapDao.getAll()
    override fun insertSnap(fridgeSnap: FridgeSnap) : Completable = fridgeSnapDao.insertSnap(fridgeSnap)
    override fun deleteSnap(id: Int) : Completable = fridgeSnapDao.deleteSnap(id)
    override fun updateSnap(fridgeSnap: FridgeSnap): Completable = fridgeSnapDao.updateSnap(fridgeSnap)
}