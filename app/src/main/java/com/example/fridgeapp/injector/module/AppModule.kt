package com.example.fridgeapp.injector.module

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule constructor(private val mApp: Application) {
    @Provides
    @Singleton
    fun provideApplication(): Application {
        return mApp
    }
}