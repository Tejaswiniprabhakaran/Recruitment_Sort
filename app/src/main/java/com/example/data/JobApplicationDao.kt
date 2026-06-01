package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobApplicationDao {
    @Query("SELECT * FROM job_applications ORDER BY receivedTimestamp DESC")
    fun getAllApplications(): Flow<List<JobApplication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(app: JobApplication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(apps: List<JobApplication>)

    @Update
    suspend fun updateApplication(app: JobApplication)

    @Delete
    suspend fun deleteApplication(app: JobApplication)

    @Query("DELETE FROM job_applications")
    suspend fun clearAll()

    @Query("SELECT * FROM job_applications WHERE id = :id LIMIT 1")
    suspend fun getApplicationById(id: Int): JobApplication?
}
