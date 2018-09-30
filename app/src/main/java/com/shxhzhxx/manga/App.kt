package com.shxhzhxx.manga

import androidx.room.Room
import com.shxhzhxx.imageloader.BitmapLoader
import com.shxhzhxx.imageloader.ImageLoader
import com.shxhzhxx.sdk.Application

class App : Application() {
    companion object {
        lateinit var db: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()
        ImageLoader.getInstance().bitmapLoader.resizeCache(BitmapLoader.CacheSize.JUMBO)
        db = Room.databaseBuilder(this, AppDatabase::class.java, "manga-db").build()
    }
}