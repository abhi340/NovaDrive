package com.example.tvdrive

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class TvDriveApp : Application(), coil.ImageLoaderFactory {

    /** Lazily-created manual DI container. All singletons live here. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(): ImageLoader {
        return container.imageLoader
    }

    override fun onCreate() {
        super.onCreate()
        // Set Coil singleton with strict memory cap — critical for 1 GB devices
        Coil.setImageLoader(container.imageLoader)
    }
}
