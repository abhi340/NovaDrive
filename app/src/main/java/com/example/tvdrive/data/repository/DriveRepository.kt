package com.example.tvdrive.data.repository

import com.example.tvdrive.data.local.DriveFileCacheDao
import com.example.tvdrive.data.local.DriveFileCacheEntity
import com.example.tvdrive.data.model.DriveFile
import com.example.tvdrive.data.remote.DrivePageResult
import com.example.tvdrive.data.remote.DriveRestClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CACHE_TTL_MS = 5 * 60 * 1000L  // 5 minutes

/**
 * Drive repository — cache-first strategy.
 *
 * Low-end device strategy:
 * - Cache folder listings in Room (TTL 5 min)
 * - On D-pad Back navigation, folder contents reload from cache (0 network calls)
 * - Full refresh only when cache is stale or user explicitly pulls
 */
class DriveRepository(
    private val client: DriveRestClient,
    private val cacheDao: DriveFileCacheDao
) {
    /**
     * Get files in [folderId].
     * Returns cached data if fresh, otherwise fetches from network and caches.
     */
    suspend fun listFiles(folderId: String, forceRefresh: Boolean = false): Result<List<DriveFile>> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val lastCached = cacheDao.getLastCacheTime(folderId) ?: 0L
                if (System.currentTimeMillis() - lastCached < CACHE_TTL_MS) {
                    val cached = cacheDao.getForFolder(folderId)
                    if (cached.isNotEmpty()) return@withContext Result.success(cached.map { it.toDomain() })
                }
            }
            // Fetch from network
            try {
                val result = client.listFiles(folderId)
                val entities = result.files.map { it.toEntity(folderId) }
                cacheDao.deleteForFolder(folderId)
                cacheDao.insertAll(entities)
                Result.success(result.files)
            } catch (e: Exception) {
                // On failure, return stale cache rather than nothing
                val stale = cacheDao.getForFolder(folderId)
                if (stale.isNotEmpty()) Result.success(stale.map { it.toDomain() })
                else Result.failure(e)
            }
        }

    suspend fun searchFiles(query: String): Result<List<DriveFile>> = withContext(Dispatchers.IO) {
        // Check cache first for local search (fast, no network)
        val localResults = cacheDao.search(query)
        if (localResults.isNotEmpty()) {
            return@withContext Result.success(localResults.map { it.toDomain() })
        }
        // Fall back to network search
        try {
            Result.success(client.searchFiles(query).files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamUrl(fileId: String) = client.streamUrl(fileId)
    suspend fun getToken() = client.getToken()
}

private fun DriveFileCacheEntity.toDomain() = DriveFile(
    id = id, name = name, mimeType = mimeType, size = size,
    modifiedTime = modifiedTime, thumbnailLink = thumbnailLink,
    webContentLink = webContentLink, parents = listOf(parentFolderId)
)

private fun DriveFile.toEntity(parentFolderId: String) = DriveFileCacheEntity(
    id = id, name = name, mimeType = mimeType, size = size,
    modifiedTime = modifiedTime, thumbnailLink = thumbnailLink,
    webContentLink = webContentLink, parentFolderId = parentFolderId
)
