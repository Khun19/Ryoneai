package com.example.data.feedback

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Query("DELETE FROM user_feedback WHERE id = :id")
    suspend fun deleteFeedbackById(id: Int)

    @Query("DELETE FROM user_feedback")
    suspend fun clearAllFeedback()
}
