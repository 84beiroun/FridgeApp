package com.example.fridgeapp.helpers.module

import android.app.Application
import androidx.room.Room
import com.example.fridgeapp.helpers.data.FridgeSnapDao
import com.example.fridgeapp.helpers.repository.FridgeSnapsDB
import com.example.fridgeapp.helpers.repository.SnapsDataSource
import com.example.fridgeapp.helpers.repository.SnapsRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

//модуль бд, зачем я вообще просто перевожу названия классов
@Module
class RoomModule constructor(private val mApp: Application) {
    private var fridgeSnapsDB = Room.databaseBuilder(
        mApp, FridgeSnapsDB::class.java, "fridgeSnapsDB"
    ).fallbackToDestructiveMigration().build()

    //выше видно что при переходе на новую версию бд ничего не сохраняется, пока не деплой, пока можно
    @Singleton
    @Provides
    fun providesDB(): FridgeSnapsDB {
        return fridgeSnapsDB
    }

    @Singleton
    @Provides
    fun providesSnapDao(): FridgeSnapDao {
        return fridgeSnapsDB.getDao()
    }

    @Singleton
    @Provides
    fun snapsRepository(snapDao: FridgeSnapDao): SnapsRepository {
        return SnapsDataSource(snapDao)
    }
}