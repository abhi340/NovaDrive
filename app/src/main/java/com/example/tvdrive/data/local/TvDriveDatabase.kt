package com.example.tvdrive.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ── Entities ──────────────────────────────────────────────────────────────────

/**
 * Cached Drive file listing per parent folder.
 * TTL = 5 minutes. Older entries trigger a network refresh.
 * Caching prevents re-fetching on D-pad back navigation — key for slow connections.
 */
@Entity(tableName = "drive_file_cache")
data class DriveFileCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mimeType: String,
    val size: Long = 0L,
    val modifiedTime: String = "",
    val thumbnailLink: String? = null,
    val webContentLink: String? = null,
    val parentFolderId: String,
    @ColumnInfo(defaultValue = "0") val cachedAt: Long = System.currentTimeMillis()
)

/** Stores playback resume position per file. Tiny table — low RAM impact. */
@Entity(tableName = "playback_positions")
data class PlaybackPosition(
    @PrimaryKey val fileId: String,
    val positionMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

/** Tracks downloaded files. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val fileId: String,
    val fileName: String,
    val mimeType: String,
    val localPath: String,
    val sizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED"  // PENDING | IN_PROGRESS | COMPLETED | FAILED
)

// ── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface DriveFileCacheDao {

    @Query("SELECT * FROM drive_file_cache WHERE parentFolderId = :folderId ORDER BY mimeType DESC, name ASC")
    suspend fun getForFolder(folderId: String): List<DriveFileCacheEntity>

    @Query("SELECT MAX(cachedAt) FROM drive_file_cache WHERE parentFolderId = :folderId")
    suspend fun getLastCacheTime(folderId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<DriveFileCacheEntity>)

    @Query("DELETE FROM drive_file_cache WHERE parentFolderId = :folderId")
    suspend fun deleteForFolder(folderId: String)

    @Query("SELECT * FROM drive_file_cache WHERE name LIKE '%' || :query || '%' LIMIT 30")
    suspend fun search(query: String): List<DriveFileCacheEntity>
}

@Dao
interface PlaybackPositionDao {
    @Query("SELECT * FROM playback_positions WHERE fileId = :fileId")
    suspend fun get(fileId: String): PlaybackPosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PlaybackPosition)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE fileId = :fileId")
    suspend fun delete(fileId: String)

    @Query("SELECT * FROM downloads WHERE fileId = :fileId")
    suspend fun get(fileId: String): DownloadEntity?
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(
    entities = [DriveFileCacheEntity::class, PlaybackPosition::class, DownloadEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TvDriveDatabase : RoomDatabase() {
    abstract fun driveFileCacheDao(): DriveFileCacheDao
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun downloadDao(): DownloadDao
}
