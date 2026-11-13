package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TimestampPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface TimestampPhotoDao {
    @Query("SELECT * FROM timestamp_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<TimestampPhoto>>

    @Query("SELECT * FROM timestamp_photos WHERE linkedTripId = :tripId ORDER BY timestamp DESC")
    fun getPhotosForTrip(tripId: Long): Flow<List<TimestampPhoto>>

    @Query("SELECT * FROM timestamp_photos WHERE linkedNoteId = :noteId ORDER BY timestamp DESC")
    fun getPhotosForNote(noteId: Long): Flow<List<TimestampPhoto>>

    @Query("SELECT * FROM timestamp_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): TimestampPhoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: TimestampPhoto): Long

    @Query("DELETE FROM timestamp_photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM timestamp_photos")
    fun getPhotoCount(): Flow<Int>
}
