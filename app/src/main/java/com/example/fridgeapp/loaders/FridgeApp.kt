package com.example.fridgeapp.loaders

import android.app.Application
import com.example.fridgeapp.injector.module.AppComponent
import com.example.fridgeapp.injector.module.AppModule
import com.example.fridgeapp.injector.module.DaggerAppComponent
import com.example.fridgeapp.injector.module.RoomModule

//позволяет создать один раз экземпляр бд и пользоваться им
open class FridgeApp : Application() {
    companion object {
        lateinit var dbInstance: AppComponent
    }

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        dbInstance =
            DaggerAppComponent.builder().appModule(AppModule(this)).roomModule(RoomModule(this))
                .build()
    }
}