package com.example.fridgeapp.helpers.module

import android.app.Application
import com.example.fridgeapp.AddSnap
import com.example.fridgeapp.CardExpanded
import com.example.fridgeapp.ListFragment
import com.example.fridgeapp.MainActivity
import com.example.fridgeapp.helpers.data.FridgeSnapDao
import com.example.fridgeapp.helpers.repository.FridgeSnapsDB
import dagger.Component
import javax.inject.Singleton

//компонент приложения, тут методы непосредственно инъекций все
@Singleton
@Component(modules = [AppModule::class, RoomModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(fragment: ListFragment)
    fun inject(fragment: AddSnap)
    fun inject(fragment: CardExpanded)
    fun snapsDao(): FridgeSnapDao
    fun snapsDB(): FridgeSnapsDB
    fun application(): Application
}