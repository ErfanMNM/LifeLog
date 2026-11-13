package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE status = 'IN_PROGRESS' ORDER BY startTime DESC LIMIT 1")
    fun getActiveTrip(): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(SUM(distanceKm), 0.0) FROM trips WHERE status = 'COMPLETED'")
    fun getTotalDistance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(costVnd), 0) FROM trips WHERE status = 'COMPLETED'")
    fun getTotalCost(): Flow<Long>

    @Query("SELECT COUNT(*) FROM trips WHERE status = 'COMPLETED'")
    fun getCompletedTripCount(): Flow<Int>
}
