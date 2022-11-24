package com.example.fridgeapp.injector.module

import android.app.Application
import com.example.fridgeapp.view.AddSnap
import com.example.fridgeapp.view.CardExpanded
import com.example.fridgeapp.view.ListFragment
import com.example.fridgeapp.loaders.MainActivity
import com.example.fridgeapp.handlers.FridgeSnapDao
import com.example.fridgeapp.injector.repository.FridgeSnapsDB
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