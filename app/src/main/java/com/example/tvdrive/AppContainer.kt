package com.example.tvdrive

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.tvdrive.auth.AuthManager
import com.example.tvdrive.auth.DeviceAuthManager
import com.example.tvdrive.data.local.TvDriveDatabase
import com.example.tvdrive.data.remote.DriveRestClient
import com.example.tvdrive.data.remote.PhotosRestClient
import com.example.tvdrive.data.repository.DriveRepository
import com.example.tvdrive.data.repository.PhotosRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Manual DI container — replaces Hilt.
 * All singletons are lazy so nothing is created until first use.
 * This keeps startup RAM near zero for unused paths.
 */
class AppContainer(private val app: Application) {

    /** Single shared OkHttpClient — connection pool shared across all requests */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // longer read for slow TV connections
            .writeTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

    /** Google Sign-In auth state manager */
    val authManager: AuthManager by lazy { AuthManager(app) }

    /** Google OAuth 2.0 Device Flow (QR Code Sign-In) manager */
    val deviceAuthManager: DeviceAuthManager by lazy { DeviceAuthManager(okHttpClient) }

    /** Room database — single instance */
    val database: TvDriveDatabase by lazy {
        Room.databaseBuilder(app, TvDriveDatabase::class.java, "tvdrive.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /** Drive REST API client (OkHttp, no google-api-client JAR) */
    val driveRestClient: DriveRestClient by lazy { DriveRestClient(authManager, okHttpClient) }

    /** Photos Library REST API client */
    val photosRestClient: PhotosRestClient by lazy { PhotosRestClient(authManager, okHttpClient) }

    /**
     * Drive repository — cache-first (Room TTL 5 min), falls back to network.
     * Caching reduces network calls and RAM churn on low-end devices.
     */
    val driveRepository: DriveRepository by lazy {
        DriveRepository(driveRestClient, database.driveFileCacheDao())
    }

    /** Photos repository — network only (Photos API has its own CDN caching) */
    val photosRepository: PhotosRepository by lazy { PhotosRepository(photosRestClient) }

    /**
     * Coil ImageLoader — memory capped at 15% of available RAM (≈38 MB on 256 MB heap).
     * Disk cache capped at 200 MB.
     * Injects OAuth2 Authorization token for googleapis.com image requests.
     */
    val imageLoader: ImageLoader by lazy {
        val authOkHttpClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                val token = authManager.getAccessTokenSync()
                val shouldAuth = token != null &&
                    !url.contains("commondatastorage.googleapis.com") &&
                    (url.contains("googleapis.com/drive") || url.contains("drive.google.com"))
                val newReq = if (shouldAuth) {
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
                chain.proceed(newReq)
            }
            .build()

        ImageLoader.Builder(app)
            .okHttpClient(authOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(app)
                    .maxSizePercent(0.15)   // 15% of app's available RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(app.cacheDir.resolve("img_cache"))
                    .maxSizeBytes(200L * 1024 * 1024)   // 200 MB
                    .build()
            }
            .crossfade(300)
            .build()
    }
}
